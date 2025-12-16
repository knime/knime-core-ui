import { DEFAULT_INITIAL_DATA } from "../src/initial-data-service-browser-mock";
import { createScriptingServiceMock } from "../src/scripting-service-browser-mock";
import type { SettingsInitialData } from "../src/settings-service";
import { createSettingsServiceMock } from "../src/settings-service-browser-mock";

import { type AppInitialData } from "./app-initial-data";

const settingsInitialData = {
  data: {
    model: {
      script: "A dummy script",
      supportMultipleStatements: false,
      separator: ";",
    },
  },
  schema: {
    type: "object",
    properties: {
      model: {
        type: "object",
        properties: {
          script: {
            type: "string",
            default: "A dummy script",
          },
          separator: {
            type: "string",
            title: "Statement Separator",
            description:
              "The separator used to split multiple statements. Only relevant if 'Support Multiple Statements' is enabled.",
            default: ";",
          },
          supportMultipleStatements: {
            type: "boolean",
            title: "Support Multiple Statements",
            description:
              "If enabled, the scripting node will support multiple statements execution.",
            default: false,
          },
        },
      },
    },
  },
  // eslint-disable-next-line camelcase
  ui_schema: {
    elements: [
      {
        label: "Multiple SQL Statements",
        type: "Section",
        description:
          "Settings for handling multiple SQL statements in the scripting node.",
        elements: [
          {
            type: "Control",
            scope: "#/properties/model/properties/supportMultipleStatements",
            options: {
              format: "checkbox",
            },
          },
          {
            type: "Control",
            scope: "#/properties/model/properties/separator",
          },
        ],
      },
    ],
  },
  flowVariableSettings: {},
  persist: {
    type: "object",
    properties: {
      model: {
        type: "object",
        properties: {
          script: {
            configKey: "expression",
          },
          supportMultipleStatements: {},
          separator: {},
        },
      },
    },
  },
} as unknown as SettingsInitialData;

const initialData = {
  language: "python",
  fileName: "script.py",
  ...DEFAULT_INITIAL_DATA,
} satisfies AppInitialData;
const scriptingService = createScriptingServiceMock({
  unlicensedKaiUser: false,
});
const settingsService = createSettingsServiceMock({ settingsInitialData });

export default {
  initialData,
  scriptingService,
  settingsService,
  displayMode: "large" as const,
};
