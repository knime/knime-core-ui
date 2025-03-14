/* eslint-disable max-lines */
import { describe, expect, it } from "vitest";

import type { PersistSchema } from "../../types/Persist";
import {
  getConfigPaths as getConfigAndDataPaths,
  getLongestCommonPrefix,
  getSubConfigKeys,
} from "../paths";

describe("paths", () => {
  const getDataPaths = (path: string, persistSchema: PersistSchema) =>
    getConfigAndDataPaths({ path, persistSchema }).map(
      ({ dataPath }) => dataPath,
    );
  const getConfigPaths = (path: string, persistSchema: PersistSchema) =>
    getConfigAndDataPaths({ path, persistSchema }).map(
      ({ configPath, deprecatedConfigPaths }) => ({
        configPath,
        deprecatedConfigPaths,
      }),
    );

  describe("sub config keys", () => {
    it("infers sub config keys from atomic schema if no sub config keys provided", () => {
      const persistSchema = {};
      expect(getSubConfigKeys(persistSchema)).toStrictEqual([]);
    });

    it("infers sub config keys from nested schema if no sub config keys provided", () => {
      const persistSchema: PersistSchema = {
        type: "object",
        properties: {
          a: {
            type: "object",
            properties: {
              b: {},
              c: {
                type: "array",
                items: {},
              },
            },
          },
          d: {
            type: "array",
            items: {
              type: "object",
              properties: {
                e: {},
              },
            },
          },
        },
      };
      expect(getSubConfigKeys(persistSchema)).toStrictEqual([
        ["a", "b"],
        ["a", "c"],
        ["d", "e"],
      ]);
    });

    it("respects overridden config key when inferring sub config keys", () => {
      const persistSchema: PersistSchema = {
        type: "object",
        properties: {
          a: {
            type: "object",
            configKey: "b",
            properties: {
              c: {
                type: "object",
                properties: {
                  d: {},
                  e: {},
                },
              },
            },
          },
        },
      };
      expect(getSubConfigKeys(persistSchema)).toStrictEqual([
        ["b", "c", "d"],
        ["b", "c", "e"],
      ]);
    });

    it("respects empty config keys when inferring sub config keys", () => {
      const persistSchema: PersistSchema = {
        type: "object",
        properties: {
          a: {
            type: "object",
            configPaths: [],
            properties: {
              d: {
                type: "object",
                properties: {
                  e: {},
                  f: {},
                },
              },
            },
          },
        },
      };
      expect(getSubConfigKeys(persistSchema)).toStrictEqual([]);
    });
  });

  describe("data paths", () => {
    it("returns given path if sub persist schema are empty", () => {
      const path = "model.mySetting";
      const persistSchema: PersistSchema = {
        type: "object",
        properties: {
          model: {
            type: "object",
            properties: {
              mySetting: {},
            },
          },
        },
      };

      const dataPaths = getDataPaths(path, persistSchema);
      expect(dataPaths).toStrictEqual([path]);
    });

    it("appends keys from sub persist schema", () => {
      const path = "model.mySetting";
      const persistSchema: PersistSchema = {
        type: "object",
        properties: {
          model: {
            type: "object",
            properties: {
              mySetting: {
                type: "object",
                properties: {
                  first: {},
                  second: {},
                },
              },
            },
          },
        },
      };
      const dataPaths = getDataPaths(path, persistSchema);
      expect(dataPaths).toStrictEqual([
        "model.mySetting.first",
        "model.mySetting.second",
      ]);
    });

    it("repeats data path for multiple config keys", () => {
      const path = "model.mySetting";
      const persistSchema: PersistSchema = {
        type: "object",
        properties: {
          model: {
            type: "object",
            properties: {
              mySetting: {
                type: "object",
                configPaths: [["model_1"], ["model_2"]],
                properties: {},
              },
            },
          },
        },
      };
      const dataPaths = getDataPaths(path, persistSchema);
      expect(dataPaths).toStrictEqual(["model.mySetting", "model.mySetting"]);
    });
  });

  describe("config paths", () => {
    it("returns given path if no configPaths and no sub persist schema are given", () => {
      const path = "model.mySetting";
      const persistSchema: PersistSchema = {
        type: "object",
        properties: {
          model: {
            type: "object",
            properties: {
              mySetting: {},
            },
          },
        },
      };
      const configPaths = getConfigPaths(path, persistSchema);
      expect(configPaths).toStrictEqual([
        { configPath: path, deprecatedConfigPaths: [] },
      ]);
    });

    it("returns given path if persist schema ends early", () => {
      const path = "model.mySetting";
      const persistSchema: PersistSchema = {
        type: "object",
        properties: {
          model: {
            type: "object",
            properties: {},
          },
        },
      };
      const configPaths = getConfigPaths(path, persistSchema);
      expect(configPaths).toStrictEqual([
        { configPath: path, deprecatedConfigPaths: [] },
      ]);
    });

    it("appends keys from sub persist schema", () => {
      const path = "model.mySetting";
      const persistSchema: PersistSchema = {
        type: "object",
        properties: {
          model: {
            type: "object",
            properties: {
              mySetting: {
                type: "object",
                properties: { first: {}, second: {} },
              },
            },
          },
        },
      };
      const configPaths = getConfigPaths(path, persistSchema);
      expect(configPaths).toStrictEqual(
        ["model.mySetting.first", "model.mySetting.second"].map(
          (configPath) => ({ configPath, deprecatedConfigPaths: [] }),
        ),
      );
    });

    it("uses configKey and continues traversal", () => {
      const path = "model.mySetting";
      const persistSchema: PersistSchema = {
        type: "object",
        properties: {
          model: {
            type: "object",
            configKey: "model_1",
            properties: {
              mySetting: {
                type: "object",
                properties: {
                  subConfigKey: {},
                },
              },
            },
          },
        },
      };
      const configPaths = getConfigPaths(path, persistSchema);
      expect(configPaths).toStrictEqual([
        {
          configPath: "model_1.mySetting.subConfigKey",
          deprecatedConfigPaths: [],
        },
      ]);
    });

    it("uses configPaths and stops traversal", () => {
      const path = "model";
      const persistSchema: PersistSchema = {
        type: "object",
        properties: {
          model: {
            type: "object",
            configPaths: [["model_1"], ["model_2", "sub"]],
            properties: {
              mySetting: {
                configPaths: [["mySetting_1"], ["mySetting_2"]],
                type: "object",
                properties: {
                  subConfigKey: {},
                },
              },
            },
          },
        },
      };
      const configPaths = getConfigPaths(path, persistSchema);
      expect(configPaths).toStrictEqual(
        ["model_1", "model_2.sub"].map((configPath) => ({
          configPath,
          deprecatedConfigPaths: [],
        })),
      );
    });

    it("returns an empty result if traversal is aborted with empty config paths before the path ended", () => {
      const path = "model.mySetting";
      const persistSchema: PersistSchema = {
        type: "object",
        properties: {
          model: {
            type: "object",
            configPaths: [],
            properties: {
              mySetting: {
                type: "object",
                properties: {
                  subConfigKey: {},
                },
              },
            },
          },
        },
      };
      const configPaths = getConfigPaths(path, persistSchema);
      expect(configPaths).toStrictEqual([]);
    });

    it("returns a non-empyt result if traversal is aborted with non-empty config paths", () => {
      const path = "model.mySetting";
      const persistSchema: PersistSchema = {
        type: "object",
        properties: {
          model: {
            type: "object",
            configPaths: [["model_1"], ["model_2"]],
            properties: {
              mySetting: {
                type: "object",
                properties: {
                  subConfigKey: {},
                },
              },
            },
          },
        },
      };
      const configPaths = getConfigPaths(path, persistSchema);
      expect(configPaths).toStrictEqual(
        ["model_1", "model_2"].map((configPath) => ({
          configPath,
          deprecatedConfigPaths: [],
        })),
      );
    });

    it("navigates to items and ignores segments for array schema ", () => {
      const path = "model.3.mySetting";
      const persistSchema: PersistSchema = {
        type: "object",
        properties: {
          model: {
            type: "array",
            items: {
              type: "object",
              properties: {
                mySetting: {
                  configPaths: [["mySetting_1"], ["mySetting_2"]],
                  type: "object",
                  properties: {
                    subConfigKey: {},
                  },
                },
              },
              configPaths: [["ignored"]],
            } as any,
          },
        },
      };
      const configPaths = getConfigPaths(path, persistSchema);
      expect(configPaths).toStrictEqual(
        ["model.3.mySetting_1", "model.3.mySetting_2"].map((configPath) => ({
          configPath,
          deprecatedConfigPaths: [],
        })),
      );
    });

    it("detects deprecatedConfigKeys", () => {
      const path = "model.mySetting";
      const persistSchema: PersistSchema = {
        type: "object",
        properties: {
          model: {
            type: "object",
            deprecatedConfigKeys: [
              {
                deprecated: [
                  ["deprecated", "1"],
                  ["deprecated", "2"],
                ],
              },
              {
                deprecated: [["deprecated", "3"]],
              },
            ],
            properties: {
              mySetting: {
                deprecatedConfigKeys: [
                  {
                    deprecated: [["deprecated", "4"]],
                  },
                ],
                configPaths: [["mySetting_1"], ["mySetting_2"]],
                type: "object",
                properties: {
                  subConfigKey: {},
                },
              },
            },
          },
          view: {
            type: "object",
            properties: {},
          },
        },
      };
      const configPaths = getConfigPaths(path, persistSchema);
      const expectedDeprecatedConfigPaths = [
        "deprecated.1",
        "deprecated.2",
        "deprecated.3",
        "model.deprecated.4",
      ];
      expect(configPaths).toStrictEqual([
        {
          configPath: "model.mySetting_1",
          deprecatedConfigPaths: expectedDeprecatedConfigPaths,
        },
        {
          configPath: "model.mySetting_2",
          deprecatedConfigPaths: expectedDeprecatedConfigPaths,
        },
      ]);
    });

    it("detects propertiesDeprecatedConfigKeys", () => {
      const persistSchema: PersistSchema = {
        type: "object",
        properties: {
          model: {
            type: "object",
            propertiesDeprecatedConfigKeys: [
              {
                deprecated: [["deprecated"]],
              },
            ],
            properties: {
              mySetting: {
                configPaths: [["mySetting_1"], ["mySetting_2"]],
              },
              myOtherSetting: {},
            },
          },
        },
      };
      expect(getConfigPaths("model.mySetting", persistSchema)).toStrictEqual([
        {
          configPath: "model.mySetting_1",
          deprecatedConfigPaths: ["model.deprecated"],
        },
        {
          configPath: "model.mySetting_2",
          deprecatedConfigPaths: ["model.deprecated"],
        },
      ]);
      expect(
        getConfigPaths("model.myOtherSetting", persistSchema),
      ).toStrictEqual([
        {
          configPath: "model.myOtherSetting",
          deprecatedConfigPaths: ["model.deprecated"],
        },
      ]);
    });

    it("detects propertiesConfigPaths", () => {
      const persistSchema: PersistSchema = {
        type: "object",
        properties: {
          model: {
            type: "object",
            propertiesConfigPaths: [["mySetting_1"], ["mySetting_2"]],
            properties: {
              mySetting: {},
              myOtherSetting: {},
            },
          },
        },
      };
      expect(getConfigPaths("model.mySetting", persistSchema)).toStrictEqual([
        {
          configPath: "model.mySetting_1",
          deprecatedConfigPaths: [],
        },
        {
          configPath: "model.mySetting_2",
          deprecatedConfigPaths: [],
        },
      ]);
    });
  });

  describe("longest common prefixes", () => {
    it("determines longest common prefix for empty path array", () => {
      expect(getLongestCommonPrefix([])).toBe("");
    });

    it("determines longest common prefix", () => {
      const path = "model.mySetting";
      const persistSchema: PersistSchema = {
        type: "object",
        properties: {
          model: {
            type: "object",
            properties: {
              mySetting: {},
              mySettingTwo: {},
            },
          },
        },
      };
      const dataPaths = getDataPaths(path, persistSchema);
      const prefix = getLongestCommonPrefix(dataPaths);
      expect(prefix).toBe("model.");
    });

    it("determines longest common prefix with keys from sub persist schema", () => {
      const path = "model.mySetting";
      const persistSchema: PersistSchema = {
        type: "object",
        properties: {
          model: {
            type: "object",
            properties: {
              mySetting: {
                type: "object",
                properties: {
                  one: {
                    type: "object",
                    properties: {
                      two: {},
                      three: {},
                    },
                  },
                },
              },
            },
          },
        },
      };
      const dataPaths = getDataPaths(path, persistSchema);
      const prefix = getLongestCommonPrefix(dataPaths);
      expect(prefix).toBe("model.mySetting.one.");
    });
  });
});
