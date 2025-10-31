import { beforeEach, describe, expect, it, vi } from "vitest";
import flushPromises from "flush-promises";

import { Dropdown } from "@knime/components";

import type { Result } from "@/nodeDialog/api/types/Result";
import type { Update, UpdateResult } from "@/nodeDialog/types/Update";

import { setupDynamicInputTest } from "./utils";

describe("dynamic parameters - update mechanism", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("processes provided updates within dynamic settings", async () => {
    const innerUpdate: Update = {
      dependencies: [],
      trigger: { id: "innerTrigger" },
      triggerInitially: true,
    };

    const { wrapper, dataSpy } = await setupDynamicInputTest({
      dynamicData: { key: "initialValue" },
      settingsId: "myDynamicSettings",
      updates: { globalUpdates: [innerUpdate] },
      additionalDataServiceHandlers: (params) => {
        if (params?.method === "settings.update2WithSettingsId") {
          return Promise.resolve({
            state: "SUCCESS",
            result: [
              {
                scope: "#/properties/key",
                values: [{ indices: [], value: "updated from inner trigger" }],
              },
            ] satisfies UpdateResult[],
          } as Result<UpdateResult[]>);
        }
        // eslint-disable-next-line no-undefined
        return undefined;
      },
    });

    await flushPromises();
    await wrapper.vm.$nextTick();

    // Check that the inner update was processed with update2WithSettingsId
    expect(dataSpy).toHaveBeenCalledWith(
      expect.objectContaining({
        method: "settings.update2WithSettingsId",
      }),
    );
  });

  it.each([
    ["", { key: "initialValue", otherKey: "initial" }, "initial"],
    [
      " with differing initial value",
      { key: "loadedInitialValue", otherKey: "initial" },
      "updated from inner trigger with value 'initialValue'",
    ],
  ])(
    "handles scoped value updates and dependencies correctly%s",
    async (_desc, initialValue, expectedOtherKeyValueBeforeChange) => {
      const innerUpdate: Update = {
        dependencies: ["#/properties/key"],
        trigger: {
          scope: "#/properties/key",
        },
        triggerInitially: false,
      };

      const { wrapper } = await setupDynamicInputTest({
        initialValue,
        dynamicData: { key: "initialValue", otherKey: "initial" },
        dynamicSchema: {
          type: "object",
          properties: {
            key: { type: "string" },
            otherKey: { type: "string" },
          },
        },
        dynamicUiSchema: {
          elements: [
            {
              scope: "#/properties/key",
              type: "Control",
            },
            {
              scope: "#/properties/otherKey",
              type: "Control",
            },
          ],
        },
        settingsId: "myDynamicSettings",
        updates: { globalUpdates: [innerUpdate] },
        additionalDataServiceHandlers: (params) => {
          if (
            params?.method === "settings.update2WithSettingsId" &&
            params?.options?.[2]?.scope === "#/properties/key"
          ) {
            const dependencyValue =
              params.options?.[3]?.["#/properties/key"]?.[0]?.value;
            return Promise.resolve({
              state: "SUCCESS",
              result: [
                {
                  scope: "#/properties/otherKey",
                  values: [
                    {
                      indices: [],
                      value: `updated from inner trigger with value '${dependencyValue}'`,
                    },
                  ],
                },
              ] satisfies UpdateResult[],
            } as Result<UpdateResult[]>);
          }
          // eslint-disable-next-line no-undefined
          return undefined;
        },
      });

      await flushPromises();
      await wrapper.vm.$nextTick();

      expect(wrapper.vm.getCurrentData().model.myDynamicSettings.otherKey).toBe(
        expectedOtherKeyValueBeforeChange,
      );

      // Change the value to trigger the scoped update
      const inputFields = wrapper.findAll("input");
      const keyInput = inputFields.find((input) =>
        input.element.value.includes("initialValue"),
      );

      expect(keyInput).toBeDefined();

      await keyInput!.setValue("changed");
      await flushPromises();

      expect(wrapper.vm.getCurrentData().model.myDynamicSettings.otherKey).toBe(
        "updated from inner trigger with value 'changed'",
      );
    },
  );

  it("handles ui state update responses correctly", async () => {
    const { wrapper } = await setupDynamicInputTest({
      dynamicData: { key: "initialValue" },
      dynamicUiSchema: {
        scope: "#/properties/key",
        options: {
          format: "dropDown",
        },
        providedOptions: ["possibleValues"],
      },
      settingsId: "myDynamicSettings",
      updates: {
        globalUpdates: [
          {
            dependencies: [],
            trigger: { id: "updateTrigger" },
            triggerInitially: true,
          },
        ],
      },
      additionalDataServiceHandlers: (params) => {
        if (params?.method === "settings.update2WithSettingsId") {
          return Promise.resolve({
            state: "SUCCESS",
            result: [
              {
                scope: "#/properties/key",
                providedOptionName: "possibleValues",
                values: [
                  {
                    indices: [],
                    value: [
                      { id: "updatedOption1", text: "Updated Option 1" },
                      { id: "updatedOption2", text: "Updated Option 2" },
                    ],
                  },
                ],
              },
            ] satisfies UpdateResult[],
          } as Result<UpdateResult[]>);
        }
        // eslint-disable-next-line no-undefined
        return undefined;
      },
    });

    await flushPromises();
    await wrapper.vm.$nextTick();

    const firstDropdown = wrapper.findComponent(Dropdown);
    expect(firstDropdown.exists()).toBe(true);

    expect(firstDropdown.props("modelValue")).toBe("initialValue");
    expect(firstDropdown.props("possibleValues")).toEqual([
      { id: "updatedOption1", text: "Updated Option 1" },
      { id: "updatedOption2", text: "Updated Option 2" },
    ]);
  });

  it("handles button-triggered updates correctly", async () => {
    const { wrapper } = await setupDynamicInputTest({
      dynamicData: { key: "initialValue" },
      dynamicSchema: {
        type: "object",
        properties: {
          myButton: {
            type: "null",
            title: "Update Value",
          },
        },
      },
      dynamicUiSchema: {
        scope: "#/properties/myButton",
        type: "Control",
        options: {
          format: "simpleButton",
          showTitleAndDescription: true,
          triggerId: "myButtonTriggerId",
        },
      },
      settingsId: "myDynamicSettings",
      updates: {
        globalUpdates: [
          {
            dependencies: [],
            trigger: { id: "myButtonTriggerId" },
            triggerInitially: false,
          },
        ],
      },
      additionalDataServiceHandlers: (params) => {
        if (params?.method === "settings.update2WithSettingsId") {
          return Promise.resolve({
            state: "SUCCESS",
            result: [
              {
                scope: "#/properties/key",
                values: [{ indices: [], value: "updated via button" }],
              },
            ] satisfies UpdateResult[],
          } as Result<UpdateResult[]>);
        }
        // eslint-disable-next-line no-undefined
        return undefined;
      },
    });

    await flushPromises();
    await wrapper.vm.$nextTick();

    const button = wrapper
      .findAllComponents("button")
      .find((btn) => btn.text() === "Update Value")!;
    await button.trigger("click");
    await flushPromises();

    expect(wrapper.vm.getCurrentData().model.myDynamicSettings.key).toBe(
      "updated via button",
    );
  });
});
