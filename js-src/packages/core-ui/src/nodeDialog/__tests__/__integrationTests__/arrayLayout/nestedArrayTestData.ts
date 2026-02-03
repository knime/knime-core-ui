export const nestedArrayInitialData = {
  data: {
    model: {
      layer1: [
        {
          layer2: [
            {
              id: "Choice1",
            },
            {
              id: "Choice1,Choice2",
            },
          ],
        },
        {
          layer2: [
            {
              id: "Choice1,Choice2,Choice3",
            },
            {
              id: "Choice1,Choice2,Choice3,Choice4",
            },
          ],
        },
      ],
    },
  },
  schema: {
    type: "object",
    properties: {
      model: {
        type: "object",
        properties: {
          layer1: {
            type: "array",
            items: {
              type: "object",
              properties: {
                layer2: {
                  type: "array",
                  items: {
                    type: "object",
                    properties: {
                      id: {
                        type: "string",
                      },
                    },
                  },
                },
              },
            },
          },
        },
      },
    },
  },
  /* eslint-disable-next-line camelcase */
  ui_schema: {
    elements: [
      {
        scope: "#/properties/model/properties/layer1",
        type: "Control",
        options: {
          arrayElementTitle: "Layer1",
          detail: [
            {
              scope: "#/properties/layer2",
              type: "Control",
              options: {
                arrayElementTitle: "Layer2",
                detail: [
                  {
                    scope: "#/properties/id",
                    type: "Control",
                  },
                ],
              },
            },
          ],
        },
      },
    ],
  },
  globalUpdates: [],
  initialUpdates: [],
  flowVariableSettings: {},
};
