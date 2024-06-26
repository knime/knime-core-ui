import { describe, expect, it } from "vitest";
import { getConfigPaths, getDataPaths, getLongestCommonPrefix } from "../paths";
import Control from "@/nodeDialog/types/Control";

describe("paths", () => {
  describe("data paths", () => {
    it("returns given path if subConfigKeys are undefined", () => {
      const path = "myPath";
      const dataPaths = getDataPaths({ path, subConfigKeys: undefined });
      expect(dataPaths).toStrictEqual([path]);
    });

    it("returns given path if subConfigKeys are empty", () => {
      const path = "myPath";
      const dataPaths = getDataPaths({ path, subConfigKeys: [] });
      expect(dataPaths).toStrictEqual([path]);
    });

    it("appends subConfigKeys", () => {
      const path = "myPath";
      const dataPaths = getDataPaths({
        path,
        subConfigKeys: ["first", "second"],
      });
      expect(dataPaths).toStrictEqual(["myPath.first", "myPath.second"]);
    });
  });

  describe("config paths", () => {
    const createControl = (rootSchema: Control["rootSchema"]): Control =>
      ({
        rootSchema,
      }) as any;

    it("returns given path if no configKeys and no subConfigKeys are given", () => {
      const path = "model.mySetting";
      const control: Control = createControl({
        type: "object",
        properties: {
          model: {
            type: "object",
            properties: {
              mySetting: {},
            },
          },
        },
      });
      const configPaths = getConfigPaths({
        path,
        subConfigKeys: undefined,
        control,
      });
      expect(configPaths).toStrictEqual([
        { configPath: path, deprecatedConfigPaths: [] },
      ]);
    });

    it("appends subConfigKeys", () => {
      const path = "model.mySetting";
      const control: Control = createControl({
        type: "object",
        properties: {
          model: {
            type: "object",
            properties: {
              mySetting: {},
            },
          },
        },
      });
      const configPaths = getConfigPaths({
        path,
        subConfigKeys: ["first", "second"],
        control,
      });
      expect(configPaths).toStrictEqual(
        ["model.mySetting.first", "model.mySetting.second"].map(
          (configPath) => ({ configPath, deprecatedConfigPaths: [] }),
        ),
      );
    });

    it("uses configKeys", () => {
      const path = "model.mySetting";
      const control: Control = createControl({
        type: "object",
        properties: {
          model: {
            type: "object",
            configKeys: ["model_1", "model_2"],
            properties: {
              mySetting: {
                configKeys: ["mySetting_1", "mySetting_2"],
              },
            },
          },
        },
      });
      const configPaths = getConfigPaths({
        path,
        subConfigKeys: ["subConfigKey"],
        control,
      });
      expect(configPaths).toStrictEqual(
        [
          "model_1.mySetting_1.subConfigKey",
          "model_1.mySetting_2.subConfigKey",
          "model_2.mySetting_1.subConfigKey",
          "model_2.mySetting_2.subConfigKey",
        ].map((configPath) => ({ configPath, deprecatedConfigPaths: [] })),
      );
    });

    it("navigates to items and ignores config keys for array schema ", () => {
      const path = "model.3.mySetting";
      const control: Control = createControl({
        type: "object",
        properties: {
          model: {
            type: "array",
            configKeys: ["model_1", "model_2"],
            items: {
              type: "object",
              properties: {
                mySetting: {
                  configKeys: ["mySetting_1", "mySetting_2"],
                },
              },
              configKeys: ["ignored"],
            } as any,
          },
        },
      });
      const configPaths = getConfigPaths({
        path,
        subConfigKeys: ["subConfigKey"],
        control,
      });
      expect(configPaths).toStrictEqual(
        [
          "model_1.3.mySetting_1.subConfigKey",
          "model_1.3.mySetting_2.subConfigKey",
          "model_2.3.mySetting_1.subConfigKey",
          "model_2.3.mySetting_2.subConfigKey",
        ].map((configPath) => ({ configPath, deprecatedConfigPaths: [] })),
      );
    });

    it("detects deprecated configKeys", () => {
      const path = "model.mySetting";
      const control: Control = createControl({
        type: "object",
        properties: {
          model: {
            type: "object",
            configKeys: ["model_1", "model_2"],
            deprecatedConfigKeys: [
              {
                deprecated: [
                  ["deprecated", "1"],
                  ["deprecated", "2"],
                ],
                new: [["model_1"], ["model_1", "mySetting", "subSetting"]],
              },
              {
                deprecated: [["deprecated", "3"]],
                new: [
                  ["model_2", "mySetting_2"],
                  ["view", "otherSetting"],
                ],
              },
            ],
            properties: {
              mySetting: {
                deprecatedConfigKeys: [
                  {
                    deprecated: [["deprecated", "4"]],
                    new: [["mySetting_2"]],
                  },
                ],
                configKeys: ["mySetting_1", "mySetting_2"],
              },
            },
          },
          view: {
            type: "object",
            properties: {},
          },
        },
      });
      const configPaths = getConfigPaths({
        path,
        subConfigKeys: ["subConfigKey"],
        control,
      });
      expect(configPaths).toStrictEqual([
        {
          configPath: "model_1.mySetting_1.subConfigKey",
          deprecatedConfigPaths: ["deprecated.1", "deprecated.2"],
        },
        {
          configPath: "model_1.mySetting_2.subConfigKey",
          deprecatedConfigPaths: [
            "deprecated.1",
            "deprecated.2",
            "model_1.deprecated.4",
            "model_2.deprecated.4",
          ],
        },
        {
          configPath: "model_2.mySetting_1.subConfigKey",
          deprecatedConfigPaths: [],
        },
        {
          configPath: "model_2.mySetting_2.subConfigKey",
          deprecatedConfigPaths: [
            "deprecated.3",
            "model_1.deprecated.4",
            "model_2.deprecated.4",
          ],
        },
      ]);
    });
  });

  describe("longest common prefixes", () => {
    it("determines longest common prefix for empty path array", () => {
      expect(getLongestCommonPrefix([])).toBe("");
    });

    it("determines longest common prefix", () => {
      const path = "my.path";
      const dataPaths = getDataPaths({
        path,
        subConfigKeys: [],
      });
      const prefix = getLongestCommonPrefix(dataPaths);
      expect(prefix).toBe("my.path");
    });

    it("determines longest common prefix with subConfigKeys", () => {
      const path = "my.path";
      const dataPaths = getDataPaths({
        path,
        subConfigKeys: ["one.two", "one.three"],
      });
      const prefix = getLongestCommonPrefix(dataPaths);
      expect(prefix).toBe("my.path.one.");
    });
  });
});
