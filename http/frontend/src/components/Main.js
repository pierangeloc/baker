require('normalize.css/normalize.css');

import React, { Component } from "react";
import { render } from "react-dom";
import Form from "react-jsonschema-form";
import "bootstrap/dist/css/bootstrap.css";

const schema = {
  title: "Todo",
  type: "object",
  required: ["title"],
  properties: {
    title: {type: "string", title: "Title", default: "A new task"},
    done: {type: "boolean", title: "Done?", default: false}
  }
};

const log = (type) => console.log.bind(console, type);

class AppComponent extends Component {
  render() {
    return (
      <div className="App">
        <Form schema={schema}
                onChange={log("changed")}
                onSubmit={log("submitted")}
                onError={log("errors")} />
      </div>
    );
  }
}

AppComponent.defaultProps = {
};

export default AppComponent;
