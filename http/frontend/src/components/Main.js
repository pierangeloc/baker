require('normalize.css/normalize.css');

import React, { Component } from 'react';
import { render } from 'react-dom';
import Form from 'react-jsonschema-form';
import 'bootstrap/dist/css/bootstrap.css';

const log = (type) => console.log.bind(console, type)

class AppComponent extends Component {

   constructor(props) {
       super(props);
       this.state = {
           sensoryEvent: 'example',
           schema : {
              title: 'Todo',
              type: 'object',
              required: ['title'],
              properties: {
                title: {type: 'string', title: 'Title', default: 'A new task'},
                done: {type: 'boolean', title: 'Done?', default: false}
              }
          }
        };
     }

   updateSensoryEvent = (evt) => { this.setState({ sensoryEvent: evt.target.value }); }

   updateForm = () => {
    console.debug('updateForm called');
    var schemaRequest = new Request('/api/schemas/' + this.state.sensoryEvent);
    var appComponent = this;
    fetch(schemaRequest).then(function(response) {
      return response.json();
    }).then(function(updatedSchema) {
      appComponent.setState({ schema: updatedSchema });
      render();
    });
   }

    render() {
      return (
        <div className='App'>
          event:
          <input type='text' value={this.state.sensoryEvent} onChange={this.updateSensoryEvent}></input>
          <button type='button' onClick={this.updateForm}>Update</button>

          <Form schema={this.state.schema}
                  onChange={log('changed')}
                  onSubmit={log('submitted')}
                  onError={log('errors')} />
        </div>
      );
    }
}

AppComponent.defaultProps = { };

export default AppComponent;
