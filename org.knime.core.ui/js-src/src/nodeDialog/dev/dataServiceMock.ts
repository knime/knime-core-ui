const errorFolder = "Open me to see an error message!";

export default (rpcRequest: { method: string; params: any[] }) => {
  switch (rpcRequest.method) {
    case "flowVariables.getAvailableFlowVariables":
      return {
        STRING: [
          {
            name: "stringVariable",
            value: "the string flow variable value (abbrevia...",
            abbreviated: true,
          },
        ],
        BOOLEAN: [
          { name: "booleanVariable", value: "true", abbreviated: false },
        ],
        NUMBER: [{ name: "numberVariable", value: "100", abbreviated: false }],
      };
    case "flowVariables.getFlowVariableOverrideValue":
      switch (
        JSON.parse(rpcRequest.params[0]).flowVariableSettings[
          rpcRequest.params[1].join(".")
        ].controllingFlowVariableName
      ) {
        case "stringVariable":
          return "some string";
        case "booleanVariable":
          return true;
        case "numberVariable":
          return 100;
        default:
          return "someValue";
      }
    case "settings.getChoices":
      if (rpcRequest.params[0] === "successfulChoicesProvider") {
        return {
          result: [
            {
              id: "NONE",
              text: "None",
            },
            {
              id: "COUNT",
              text: "Occurrence count",
            },
            {
              id: "SUM",
              text: "Sum",
            },
            {
              id: "AVG",
              text: "Average",
            },
          ],
          state: "SUCCESS",
        };
      } else {
        return {
          state: "FAIL",
          message: "Async choices fetching failed because xyz",
        };
      }
    case "fileChooser.listItems":
      return {
        folder: {
          items: [
            {
              isDirectory: true,
              name: "I am a directory",
            },
            {
              isDirectory: true,
              name: errorFolder,
            },
            {
              isDirectory: false,
              name: "I am a file, select me!",
            },
          ],
          path: "/path/to/folder",
        },
        ...(rpcRequest.params[2] === errorFolder
          ? { errorMessage: "I am an error message" }
          : {}),
      };
    case "fileChooser.getFilePath":
      return `/path/to/folder/${rpcRequest.params[2]}`;
    default:
      return null;
  }
};
