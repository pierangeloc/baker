package com.ing.baker.kafka

import java.time.Duration
import java.util._
import java.util.concurrent._
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Function

import com.ing.baker.kafka.BakerKafkaConsumer._
import com.ing.baker.runtime.core.{Baker, NoSuchProcessException}
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}
import scala.concurrent.duration._


object BakerKafkaConsumer {
  private val eventDeliverTimout = Duration.ofSeconds(10)
  private val pollTimeoutMillis = 10000
  private val unexpectedExceptionTimeoutMillis = 1000
}

class BakerKafkaConsumer[K, V, E](val baker: Baker,
                                  val eventExtractor: V => Option[(String, E)],
                                  val consumer: KafkaConsumer[K, V],
                                  val topic: String) {

  private val log = LoggerFactory.getLogger(classOf[BakerKafkaConsumer[_, _, _]])

  def this(baker: Baker,
           eventExtractor: Function[V, Optional[java.util.Map.Entry[UUID, E]]],
           props: Properties,
           topic: String) {

    this(baker, eventExtractor, new KafkaConsumer[K, V](props), topic)
  }

  private val service = Executors.newSingleThreadExecutor
  private var pollTask: java.util.concurrent.Future[Unit] = null
  private val isRunning = new AtomicBoolean(true)

  def start(): Unit = {

    if (pollTask != null)
      log.warn("Already started")

    else {
      val pollLoop: Runnable = () => {
        while (isRunning.get)
          doPoll()
      }

      consumer.subscribe(Collections.singletonList(topic))
      pollTask = service.submit(pollLoop, true)
    }
  }

  protected def doPoll(): Unit = {

    val records = consumer.poll(pollTimeoutMillis)

    records.asScala.foreach { record =>

      if (log.isTraceEnabled)
        log.trace(record.value.toString)

      Try { eventExtractor(record.value) } match {
        case Failure(exception) =>
          log.error("Event extractor function threw exception", exception)
        case Success(Some((processId, event))) =>
          deliverEvent(processId, event)
        case Success(None) =>
          log.trace("Ignoring record {}", record)
      }
    }

    consumer.commitSync()
  }

  def stop(): Unit = {
    if (pollTask != null) {
      isRunning.set(false)
      try
        pollTask.get(10, TimeUnit.SECONDS)
      catch {
        case e: Exception => pollTask.cancel(true)
      } finally
        consumer.close()
    }
  }

  /**
    * Delivers an event to baker
    */
  def deliverEvent(processId: String, event: Any): Unit = {

    var eventProcessed = false

    while (!eventProcessed)
      try {
        baker.processEventAsync(processId, event, eventDeliverTimout.toMillis millis).confirmReceived(eventDeliverTimout)
        eventProcessed = true
      } catch {
        case _: NoSuchProcessException =>
          log.trace("No such process: {}", processId)
          eventProcessed = true
        case e: TimeoutException =>
          log.warn("Processing of event timed out for processId {}. Event: {}", processId, event, e)
        // We ignore this exception on purpose
        case e: Exception =>
          // This should never happen, we don't throw the exception to not stop the poll loop
          log.error("Unexpected exception from baker for process: {}", processId, e)
          // We wait here
          Thread.sleep(unexpectedExceptionTimeoutMillis)
      }
  }
}

