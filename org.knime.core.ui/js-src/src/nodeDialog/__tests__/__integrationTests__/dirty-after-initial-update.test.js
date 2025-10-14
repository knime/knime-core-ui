import { beforeEach, describe, expect, it, vi } from "vitest";
import { mount } from "@vue/test-utils";
import flushPromises from "flush-promises";

import { DialogService, JsonDataService } from "@knime/ui-extension-service";

import NodeDialog from "../../NodeDialog.vue";
import { getOptions } from "../utils";

import {
  controllingFlowVariableState,
  exposedFlowVariableState,
} from "./utils/dirtySettingState";
import { dynamicImportsSettled } from "./utils/dynamicImportsSettled";

describe("dirty after initial update", () => {
  let dirtyStates;

  const registerSetting = ({ initialValue }) => {
    let value = initialValue;
    const setValue = (v) => {
      value = v;
    };
    const dirtyState = {
      getValue: () => value,
      initialValue,
    };
    dirtyStates.push(dirtyState);
    return {
      addControllingFlowVariable: () => controllingFlowVariableState,
      addExposedFlowVariable: () => exposedFlowVariableState,
      setValue,
    };
  };

  const mockInitialData = ({ data, initialUpdates = [] }) => {
    vi.clearAllMocks();
    vi.spyOn(JsonDataService.prototype, "initialData").mockResolvedValue({
      data,
      schema: {
        type: "object",
        properties: {
          model: {
            type: "object",
            properties: {
              value: {
                type: "string",
              },
            },
          },
        },
      },
      // eslint-disable-next-line camelcase
      ui_schema: {
        elements: [
          {
            scope: "#/properties/model/properties/value",
            type: "Control",
          },
        ],
      },
      flowVariableSettings: {},
      initialUpdates,
    });
  };

  const getCurrentValues = () => dirtyStates.map(({ getValue }) => getValue());
  const isClean = () =>
    dirtyStates
      .map(({ getValue, initialValue }) => getValue() === initialValue)
      .filter((clean) => clean === false).length === 0;

  beforeEach(() => {
    dirtyStates = [];
    vi.spyOn(DialogService.prototype, "registerSettings").mockImplementation(
      () => registerSetting,
    );
  });

  it("becomes dirty when an initial update changes a value", async () => {
    mockInitialData({
      data: {
        model: {
          value: "initial",
        },
      },
      initialUpdates: [
        {
          scope: "#/properties/model/properties/value",
          values: [
            {
              indices: [],
              value: "updated",
            },
          ],
        },
      ],
    });

    const wrapper = mount(NodeDialog, getOptions());
    await dynamicImportsSettled(wrapper);
    await flushPromises();

    expect(wrapper.vm.getCurrentData().model.value).toBe("updated");
    expect(getCurrentValues()).toStrictEqual(["updated"]);
    expect(isClean()).toBe(false);
  });

  it("becomes dirty when an initial update adds a new property", async () => {
    mockInitialData({
      data: {
        model: {},
      },
      initialUpdates: [
        {
          scope: "#/properties/model/properties/value",
          values: [
            {
              indices: [],
              value: "added",
            },
          ],
        },
      ],
    });

    const wrapper = mount(NodeDialog, getOptions());
    await dynamicImportsSettled(wrapper);
    await flushPromises();

    expect(wrapper.vm.getCurrentData().model.value).toBe("added");
    expect(getCurrentValues()).toStrictEqual(["added"]);
    expect(isClean()).toBe(false);
  });

  it("stays clean when the initial update matches the initial value", async () => {
    mockInitialData({
      data: {
        model: {
          value: "initial",
        },
      },
      initialUpdates: [
        {
          scope: "#/properties/model/properties/value",
          values: [
            {
              indices: [],
              value: "initial",
            },
          ],
        },
      ],
    });

    const wrapper = mount(NodeDialog, getOptions());
    await dynamicImportsSettled(wrapper);
    await flushPromises();

    expect(wrapper.vm.getCurrentData().model.value).toBe("initial");
    expect(getCurrentValues()).toStrictEqual(["initial"]);
    expect(isClean()).toBe(true);
  });
});
