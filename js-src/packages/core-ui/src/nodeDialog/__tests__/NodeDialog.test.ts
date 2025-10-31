/* eslint-disable max-lines */
import {
  type Mock,
  type MockInstance,
  beforeEach,
  describe,
  expect,
  it,
  vi,
} from "vitest";
import { VueWrapper, shallowMount } from "@vue/test-utils";
import { JsonForms } from "@jsonforms/vue";
import flushPromises from "flush-promises";

import { JsonFormsDialog } from "@knime/jsonforms";
import {
  type AlertType,
  AlertingService,
  DialogService,
  JsonDataService,
  SharedDataService,
} from "@knime/ui-extension-service";

import { dialogInitialData } from "../../../test-setup/mocks/dialogData";
import NodeDialog from "../NodeDialog.vue";
import type { FlowSettings } from "../api/types";
import type { DialogSettings } from "../composables/nodeDialog/useUpdates";
import type { SettingsData } from "../types/SettingsData";
import type { Update, UpdateResult } from "../types/Update";

import { getOptions } from "./utils";

const clear = (obj: Record<string, unknown>) => {
  for (const prop of Object.getOwnPropertyNames(obj)) {
    delete obj[prop];
  }
};

describe("NodeDialog.vue", () => {
  let initialDataSpy: MockInstance, setApplyListenerSpy: MockInstance;

  beforeEach(() => {
    vi.clearAllMocks();
    initialDataSpy = vi
      .spyOn(JsonDataService.prototype, "initialData")
      .mockResolvedValue({
        ...dialogInitialData,
      });
    vi.spyOn(JsonDataService.prototype, "applyData").mockResolvedValue({
      isApplied: true,
    });
    vi.spyOn(SharedDataService.prototype, "shareData").mockResolvedValue();
    setApplyListenerSpy = vi.spyOn(DialogService.prototype, "setApplyListener");
    vi.spyOn(DialogService.prototype, "hasNodeView").mockReturnValue(false);
  });

  it("renders empty wrapper", async () => {
    const wrapper = shallowMount(NodeDialog, getOptions());
    await flushPromises();

    expect(wrapper.findComponent(NodeDialog).exists()).toBe(true);
    expect(setApplyListenerSpy).toHaveBeenCalled();
    expect(wrapper.find("a.advanced-options").exists()).not.toBe(true);
  });

  it("passes props to jsonform", async () => {
    const wrapper = shallowMount(NodeDialog, getOptions());
    await flushPromises();

    const jsonformsStub = wrapper.getComponent(JsonFormsDialog);

    expect(jsonformsStub.props("data")).toStrictEqual(dialogInitialData.data);
    expect(jsonformsStub.props("schema")).toStrictEqual(
      dialogInitialData.schema,
    );
    expect(jsonformsStub.props("uischema")).toStrictEqual(
      dialogInitialData.ui_schema,
    );
  });

  // Skipped due to problems stubbing JsonFormsDialog
  it.skip("renders advanced settings", async () => {
    const advancedDialogData = { ...dialogInitialData };
    advancedDialogData.ui_schema.options = { isAdvanced: true };
    vi.spyOn(JsonDataService.prototype, "initialData").mockResolvedValueOnce(
      advancedDialogData,
    );
    const wrapper = shallowMount(NodeDialog, getOptions());
    await flushPromises();

    expect(wrapper.findComponent(NodeDialog).exists()).toBe(true);
    const advancedLink = wrapper.find("a.advanced-options");
    expect(advancedLink.exists()).toBe(true);
    expect(advancedLink.text()).toBe("Show advanced settings");

    await advancedLink.trigger("click");
    expect(advancedLink.text()).toBe("Hide advanced settings");
  });

  // Skipped due to problems stubbing JsonFormsDialog
  it.skip("sets new values on change", async () => {
    const wrapper = shallowMount(NodeDialog, getOptions());
    // @ts-expect-error
    const onSettingsChangedSpy = vi.spyOn(wrapper.vm, "onSettingsChanged");
    // @ts-expect-error
    const publishDataSpy = vi.spyOn(wrapper.vm.sharedDataService, "shareData");

    await flushPromises();

    const jsonformsStub = wrapper.getComponent(JsonForms);
    jsonformsStub.vm.$emit("change", {
      data: { ...dialogInitialData.data, model: { yAxisScale: "NEW_VALUE" } },
    });

    expect(onSettingsChangedSpy).toHaveBeenCalledWith({
      data: { ...dialogInitialData.data, model: { yAxisScale: "NEW_VALUE" } },
    });

    const expectedData = {
      ...dialogInitialData.data,
      model: { yAxisScale: "NEW_VALUE" },
    };

    expect(publishDataSpy).toHaveBeenCalledWith({
      flowVariableSettings: dialogInitialData.flowVariableSettings,
      data: expectedData,
    });
  });

  type Wrapper = VueWrapper<InstanceType<typeof NodeDialog>>;

  describe("applySettings", () => {
    let wrapper: Wrapper, sendAlert: MockInstance;

    beforeEach(async () => {
      setApplyListenerSpy.mockReset();
      sendAlert = vi.fn();
      (AlertingService as never as Mock).mockImplementation(() => ({
        sendAlert,
      }));
      wrapper = shallowMount(NodeDialog, getOptions()) as never as Wrapper;
      await flushPromises();
    });

    it("calls apply data with successful response", async () => {
      const applyListener = setApplyListenerSpy.mock.calls[0][0];

      const result = { isApplied: true };
      const applyDataSpy = vi
        .spyOn(wrapper.vm.jsonDataService, "applyData")
        .mockResolvedValue(result);
      await flushPromises();

      expect(await applyListener()).toStrictEqual(result);

      expect(applyDataSpy).toHaveBeenCalled();
    });
  });

  it("provides 'getData' method", async () => {
    const wrapper = shallowMount(NodeDialog, getOptions()) as never as Wrapper;
    await flushPromises();
    const callParams = { method: "foo", options: ["bar"] } as any;
    wrapper.vm.coreComponent!.getData(callParams);
    expect(wrapper.vm.jsonDataService.data).toHaveBeenCalledWith(callParams);
  });

  it("provides 'sendAlert' method", async () => {
    const sendAlert = vi.fn();
    (AlertingService as never as Mock).mockImplementation(() => ({
      sendAlert,
    }));
    const options = getOptions();
    const wrapper = shallowMount(NodeDialog, options) as never as Wrapper;
    await flushPromises();
    const callParams = { type: "error" as AlertType, message: "message" };
    wrapper.vm.coreComponent!.sendAlert(callParams);
    expect(sendAlert).toHaveBeenCalledWith(callParams);
  });

  it("provides 'getAvailableFlowVariables' method", async () => {
    const wrapper = shallowMount(NodeDialog, getOptions());
    await flushPromises();
    const path = "path.to.my.setting";
    const currentData: SettingsData = { foo: "bar" } as never;
    wrapper.vm.coreComponent!.setCurrentData(currentData);
    delete wrapper.vm.flowVariablesMap["view.title"];
    wrapper.vm.coreComponent!.getAvailableFlowVariables(path);
    expect(wrapper.vm.jsonDataService.data).toHaveBeenCalledWith({
      method: "flowVariables.getAvailableFlowVariables",
      options: [
        JSON.stringify({
          data: currentData,
          flowVariableSettings: {},
        }),
        ["path", "to", "my", "setting"],
      ],
    });
  });

  it("provides 'getFlowVariableOverrideValue' method", async () => {
    const wrapper = await shallowMount(NodeDialog, getOptions());
    await flushPromises();
    const expectedResult = "value";
    const getDataSpy = vi
      .spyOn(wrapper.vm.jsonDataService, "data")
      .mockResolvedValue(expectedResult);
    const path = "path.to.my.setting";
    const currentData: SettingsData = { foo: "bar" } as never;
    wrapper.vm.coreComponent!.setCurrentData(currentData);
    clear(wrapper.vm.flowVariablesMap);
    wrapper.vm.flowVariablesMap.myPath = {
      controllingFlowVariableAvailable: true,
      controllingFlowVariableName: "myVar",
      exposedFlowVariableName: null,
    };
    const result = await wrapper.vm.coreComponent!.getFlowVariableOverrideValue(
      "_persistPath",
      path,
    );
    expect(getDataSpy).toHaveBeenCalledWith({
      method: "flowVariables.getFlowVariableOverrideValue",
      options: [
        JSON.stringify({
          data: currentData,
          flowVariableSettings: wrapper.vm.flowVariablesMap,
        }),
        ["path", "to", "my", "setting"],
      ],
    });
    expect(result).toStrictEqual(expectedResult);
  });

  describe("registerWatcher", () => {
    it("provides registerWatcher method", async () => {
      const wrapper = shallowMount(NodeDialog, getOptions());
      await flushPromises();
      expect(wrapper.vm.coreComponent!.registerWatcher).toBeDefined();
    });

    it.skip("adds watcher when calling registerWatcher", async () => {
      const wrapper = shallowMount(NodeDialog, getOptions());
      await flushPromises();

      wrapper.vm.coreComponent!.setCurrentData({
        test: "test",
        test2: "test",
        otherTest: "test",
      } as never);

      const transformSettings = vi.fn();
      const init = vi.fn();
      const dependencies = ["#/properties/test", "#/properties/test2"];

      await wrapper.vm.coreComponent!.registerWatcher({
        transformSettings,
        dependencies,
      });

      // @ts-expect-error
      expect(wrapper.vm.registeredWatchers.length).toBe(1);
      wrapper.vm.coreComponent!.updateData("#/properties/test");
      // @ts-expect-error
      expect(wrapper.vm.registeredWatchers[0]).toMatchObject({
        dataPaths: [["test"], ["test2"]],
      });
      expect(init).not.toHaveBeenCalled();

      await wrapper.vm.coreComponent!.registerWatcher({
        transformSettings,
        init,
        dependencies,
      });

      // @ts-expect-error
      expect(wrapper.vm.registeredWatchers.length).toBe(2);

      // @ts-expect-error
      expect(wrapper.vm.registeredWatchers[1]).toMatchObject({
        dataPaths: [["test"], ["test2"]],
      });
      expect(init).toHaveBeenCalled();
    });

    it.skip("removes watcher when calling the returned value of registerWatcher", async () => {
      const wrapper = shallowMount(NodeDialog, getOptions());
      await flushPromises();

      wrapper.vm.coreComponent!.setCurrentData({
        currentData: {
          test: "test",
          test2: "test",
          otherTest: "test",
        },
      });

      const dependencies = ["#/properties/test", "#/properties/test2"];

      const unwatch = await wrapper.vm.coreComponent!.registerWatcher({
        transformSettings: vi.fn(),
        dependencies,
      });

      // @ts-expect-error
      expect(wrapper.vm.registeredWatchers.length).toBe(1);

      unwatch();

      // @ts-expect-error
      expect(wrapper.vm.registeredWatchers.length).toBe(0);
    });
  });

  describe("updateData (old mechanism: registerWatchers)", () => {
    let wrapper: Wrapper,
      registeredWatchers: Parameters<
        (Wrapper["vm"]["coreComponent"] & object)["registerWatcher"]
      >[0][],
      transformSettingsSpy: (data: DialogSettings) => void;

    const settingsData = {
      currentData: {
        test1: "test",
        test2: "test",
        test3: "test",
        test4: "test",
      },
    };

    beforeEach(async () => {
      wrapper = shallowMount(NodeDialog, getOptions());
      await flushPromises();

      wrapper.vm.coreComponent!.setCurrentData(
        settingsData.currentData as never,
      );
      transformSettingsSpy = vi.fn();
      registeredWatchers = [
        {
          transformSettings: vi.fn(() =>
            Promise.resolve((data: DialogSettings) => {
              transformSettingsSpy(data);
              // @ts-expect-error
              data.test4 = "transformed";
            }),
          ),
          dependencies: ["#/properties/test1", "#/properties/test2"],
        },
        {
          transformSettings: vi.fn(() => Promise.resolve(transformSettingsSpy)),
          dependencies: ["#/properties/test2", "#/properties/test3"],
        },
      ];
      registeredWatchers.forEach(async (watcher) => {
        await wrapper.vm.coreComponent!.registerWatcher(watcher);
      });
    });

    it("updates data normally if no watchers are triggered", async () => {
      const path = "test4";
      await wrapper.vm.coreComponent!.updateData(path);
      registeredWatchers.forEach(({ transformSettings }) => {
        expect(transformSettings).not.toHaveBeenCalled();
      });
    });

    it("transforms settings for triggered watchers and updates data", async () => {
      const path = "test2";
      await wrapper.vm.coreComponent!.updateData(path);
      registeredWatchers.forEach(({ transformSettings }) => {
        expect(transformSettings).toHaveBeenCalled();
      });
      expect(wrapper.vm.getCurrentData()).toMatchObject({
        test4: "transformed",
      });
    });

    it("aborts first update when second update is triggered in the meantime", async () => {
      const path = "test1";
      const firstUpdate = wrapper.vm.coreComponent!.updateData(path);
      const secondUpdate = wrapper.vm.coreComponent!.updateData(path);
      await firstUpdate;
      await secondUpdate;
      expect(transformSettingsSpy).toHaveBeenCalledOnce();
    });

    it("reacts to path updates nested inside array layouts", async () => {
      wrapper.vm.coreComponent!.setCurrentData({
        arrayLayoutSetting: [{ value: "first" }, { value: "second" }],
      });

      const arrayLayoutWatcher = {
        transformSettings: vi.fn(() => Promise.resolve(() => {})),
        dependencies: ["#/properties/arrayLayoutSetting"],
      };

      await wrapper.vm.coreComponent!.registerWatcher(arrayLayoutWatcher);
      const path = "arrayLayoutSetting.0.value";
      await wrapper.vm.coreComponent!.updateData(path);

      expect(arrayLayoutWatcher.transformSettings).toHaveBeenCalled();
    });
  });

  describe("value updates, triggers and stateProviderListeners", () => {
    const uiSchemaKey = "ui_schema";
    let globalUpdates: Update[], initialUpdates: UpdateResult[];

    beforeEach(() => {
      initialDataSpy.mockImplementation(
        vi.fn(() =>
          Promise.resolve({
            data: {
              view: {
                firstSetting: "firstSetting",
              },
              model: {
                secondSetting: "secondSetting",
              },
            },
            schema: {},
            [uiSchemaKey]: {},
            globalUpdates,
            initialUpdates,
            flowVariableSettings: {},
          }),
        ),
      );
    });

    const getDataServiceSpy = () => {
      return vi.spyOn(JsonDataService.prototype, "data");
    };

    const getWrapperWithDataServiceSpy = async (
      dispatchSpy: ((path: string, value: any) => void) | null = null,
    ) => {
      const dataServiceSpy = getDataServiceSpy();
      const wrapper = shallowMount(
        NodeDialog,
        getOptions({
          dispatch: dispatchSpy,
        }),
      );
      await flushPromises();
      return { wrapper, dataServiceSpy };
    };

    it("handles value updates", async () => {
      const triggerScope = "#/properties/view/properties/firstSetting";
      const dependencyScope = "#/properties/model/properties/secondSetting";
      globalUpdates = [
        {
          trigger: {
            scope: triggerScope,
          },
          dependencies: [dependencyScope],
        },
      ];

      const dispatchSpy = vi.fn();
      const { wrapper, dataServiceSpy } = await getWrapperWithDataServiceSpy(
        dispatchSpy,
      );

      const updatedValue = "updated";
      dataServiceSpy.mockResolvedValue({
        state: "SUCCESS",
        result: [
          {
            scope: "#/properties/model/properties/secondSetting",
            values: [{ indices: [], value: updatedValue }],
          },
        ],
        message: ["Success message."],
      });

      await wrapper.vm.coreComponent!.updateData("view.firstSetting");
      expect(dataServiceSpy).toHaveBeenCalledWith({
        method: "settings.update2",
        options: [
          null,
          expect.objectContaining({ scope: triggerScope }),
          { [dependencyScope]: [{ indices: [], value: "secondSetting" }] },
        ],
      });
      expect(dispatchSpy.mock.calls[0][1]).toBe("updated");
      expect(dispatchSpy.mock.calls[0][0]).toBe("model.secondSetting");
    });

    it("handles updates triggered by a widget user interaction", async () => {
      const triggerId = "myTriggerId";
      const dependencyScope = "#/properties/model/properties/secondSetting";
      globalUpdates = [
        {
          trigger: {
            id: triggerId,
          },
          dependencies: [dependencyScope],
        },
      ];

      const dispatchSpy = vi.fn();
      const { wrapper, dataServiceSpy } = await getWrapperWithDataServiceSpy(
        dispatchSpy,
      );

      const updatedValue = "updated";
      dataServiceSpy.mockResolvedValue({
        state: "SUCCESS",
        result: [
          {
            scope: "#/properties/model/properties/secondSetting",
            values: [{ indices: [], value: updatedValue }],
          },
        ],
        message: ["Success message."],
      });

      await wrapper.vm.coreComponent!.trigger({ id: triggerId });
      expect(dataServiceSpy).toHaveBeenCalledWith({
        method: "settings.update2",
        options: [
          null,
          expect.objectContaining({ id: triggerId }),
          { [dependencyScope]: [{ indices: [], value: "secondSetting" }] },
        ],
      });

      expect(dispatchSpy.mock.calls[0][0]).toBe("model.secondSetting");
      expect(dispatchSpy.mock.calls[0][1]).toBe(updatedValue);
    });

    it("calls registered state provider listeners on update", async () => {
      const triggerId = "myTriggerId";
      const dependencyScope = "#/properties/model/properties/secondSetting";
      globalUpdates = [
        {
          trigger: {
            id: triggerId,
          },
          dependencies: [dependencyScope],
        },
      ];

      const { wrapper, dataServiceSpy } = await getWrapperWithDataServiceSpy();

      const stateProviderScope = "myStateProviderScope";
      const stateProviderOptionName = "myStateProviderOptionName";

      const stateProviderListener = vi.fn();
      wrapper.vm.coreComponent!.addStateProviderListener(
        {
          scope: stateProviderScope,
          providedOptionName: stateProviderOptionName,
        },
        stateProviderListener,
      );
      const updatedValue = "updated";
      dataServiceSpy.mockResolvedValue({
        state: "SUCCESS",
        result: [
          {
            scope: stateProviderScope,
            providedOptionName: stateProviderOptionName,
            values: [{ indices: [], value: updatedValue }],
          } satisfies UpdateResult,
        ],
        message: ["Success message."],
      });
      await wrapper.vm.coreComponent!.trigger({ id: triggerId });
      expect(dataServiceSpy).toHaveBeenCalledWith({
        method: "settings.update2",
        options: [
          null,
          expect.objectContaining({ id: triggerId }),
          { [dependencyScope]: [{ indices: [], value: "secondSetting" }] },
        ],
      });

      expect(stateProviderListener).toHaveBeenCalledWith(updatedValue);
    });

    it("shows errors on updates", async () => {
      const triggerId = "myTriggerId";
      globalUpdates = [
        {
          trigger: {
            id: triggerId,
          },
          dependencies: [],
        },
      ];

      const sendAlert = vi.fn();
      (AlertingService as never as Mock).mockImplementation(() => ({
        sendAlert,
      }));
      const { wrapper, dataServiceSpy } = await getWrapperWithDataServiceSpy();

      const errorMessage = "my error message";
      dataServiceSpy.mockResolvedValue({
        state: "FAIL",
        message: [errorMessage],
      });

      await wrapper.vm.coreComponent!.trigger({ id: triggerId });
      expect(sendAlert).toHaveBeenCalledWith({
        message: errorMessage,
        type: "error",
      });
    });

    it("handles updates triggered before the dialog is opened", async () => {
      const updatedValue = "updatedValue";
      initialUpdates = [
        {
          scope: "#/properties/model/properties/secondSetting",
          values: [{ indices: [], value: updatedValue }],
        },
      ];

      const dispatchSpy = vi.fn();
      const wrapper = shallowMount(
        NodeDialog,
        getOptions({
          dispatch: dispatchSpy,
        }),
      );
      await flushPromises();
      expect(wrapper.vm.getCurrentData()).toStrictEqual({
        view: {
          firstSetting: "firstSetting",
        },
        model: {
          secondSetting: updatedValue,
        },
      });
    });

    it("handles updates triggered after the dialog is opened", async () => {
      const triggerId = "after-open-dialog";

      globalUpdates = [
        {
          trigger: {
            id: triggerId,
          },
          triggerInitially: true,
          dependencies: [],
        },
      ];

      const dataServiceSpy = getDataServiceSpy();
      const updatedValue = "updated";
      dataServiceSpy.mockResolvedValue({
        state: "SUCCESS",
        result: [
          {
            scope: "#/properties/model/properties/secondSetting",
            values: [{ indices: [], value: updatedValue }],
          },
        ],
        message: ["Success message."],
      });

      const dispatchSpy = vi.fn();

      shallowMount(
        NodeDialog,
        getOptions({
          dispatch: dispatchSpy,
        }),
      );
      await flushPromises();

      expect(dataServiceSpy).toHaveBeenCalledWith({
        method: "settings.update2",
        options: [null, expect.objectContaining({ id: triggerId }), {}],
      });
      expect(dispatchSpy.mock.calls[0][0]).toBe("model.secondSetting");
      expect(dispatchSpy.mock.calls[0][1]).toBe(updatedValue);
    });
  });

  describe("flawed controlling variable paths", () => {
    it("sets variable path to flawed if 'getFlowVariableOverrideValue' returns undefined", async () => {
      const wrapper = shallowMount(NodeDialog, getOptions());
      vi.spyOn(wrapper.vm.jsonDataService, "data").mockResolvedValue(undefined);
      const persistPath = "my.path";
      const flowSettings = {} as FlowSettings;
      await flushPromises();
      wrapper.vm.flowVariablesMap[persistPath] = flowSettings;

      await wrapper.vm.coreComponent!.getFlowVariableOverrideValue(
        persistPath,
        "_dataPath",
      );

      expect(
        wrapper.vm.coreComponent!.flawedControllingVariablePaths,
      ).toStrictEqual(new Set([persistPath]));
      expect(flowSettings.controllingFlowVariableFlawed).toBeTruthy();
    });

    it("excludes flawed overwritten variables from subsequent 'getFlowVariableOverrideValue' requests of other settings", async () => {
      const wrapper = shallowMount(NodeDialog, getOptions());
      const getDataSpy = vi
        .spyOn(wrapper.vm.jsonDataService, "data")
        .mockResolvedValue("not_undefined");
      const persistPathFlawedSetting = "flawed";
      const persistPathOtherSetting = "other";
      await flushPromises();

      clear(wrapper.vm.flowVariablesMap);
      wrapper.vm.flowVariablesMap[persistPathFlawedSetting] = {
        controllingFlowVariableName: "flawedSettingVariable",
        controllingFlowVariableFlawed: true,
      } as FlowSettings;
      wrapper.vm.flowVariablesMap[persistPathOtherSetting] = {
        controllingFlowVariableName: "otherSettingsVariable",
        controllingFlowVariableFlawed: false,
      } as FlowSettings;
      wrapper.vm.coreComponent!.flawedControllingVariablePaths.add(
        persistPathFlawedSetting,
      );

      wrapper.vm.coreComponent!.setCurrentData({});
      await wrapper.vm.coreComponent!.getFlowVariableOverrideValue(
        persistPathOtherSetting,
        "_dataPath",
      );
      expect(getDataSpy).toHaveBeenCalledWith({
        method: "flowVariables.getFlowVariableOverrideValue",
        options: [
          `{"data":{},"flowVariableSettings":${JSON.stringify({
            [persistPathOtherSetting]: {
              controllingFlowVariableName: "otherSettingsVariable",
              controllingFlowVariableFlawed: false,
            },
          })}}`,
          ["_dataPath"],
        ],
      });
    });

    it("does not exclude flawed overwritten variable from subsequent 'getFlowVariableOverrideValue' request of the same setting", async () => {
      const wrapper = shallowMount(NodeDialog, getOptions());
      const getDataSpy = vi
        .spyOn(wrapper.vm.jsonDataService, "data")
        .mockResolvedValue("not_undefined");
      const persistPathFlawedSetting = "flawed";
      await flushPromises();
      const flawedSettings = {
        controllingFlowVariableName: "flawedSettingVariable",
        controllingFlowVariableFlawed: true,
      } as FlowSettings;

      delete wrapper.vm.flowVariablesMap["view.title"];
      wrapper.vm.flowVariablesMap[persistPathFlawedSetting] = flawedSettings;

      const variableSettingsMapBeforeRequest = JSON.stringify({
        [persistPathFlawedSetting]: flawedSettings,
      });
      wrapper.vm.coreComponent!.flawedControllingVariablePaths.add(
        persistPathFlawedSetting,
      );

      wrapper.vm.coreComponent!.setCurrentData({});
      await wrapper.vm.coreComponent!.getFlowVariableOverrideValue(
        persistPathFlawedSetting,
        "_dataPath",
      );
      expect(getDataSpy).toHaveBeenCalledWith({
        method: "flowVariables.getFlowVariableOverrideValue",
        options: [
          `{"data":{},"flowVariableSettings":${variableSettingsMapBeforeRequest}}`,
          ["_dataPath"],
        ],
      });
    });

    it("excludes initially set paths from 'getFlowVariableOverrideValue' request until the controlling variable is are changed", async () => {
      const initialSetting1 = "first";
      const initialSetting2 = "second";
      const flowSettings1: Record<string, FlowSettings> = {
        [initialSetting1]: {
          controllingFlowVariableName: "var1",
          controllingFlowVariableFlawed: true,
        } as FlowSettings,
      };
      const flowSettings2: Record<string, FlowSettings> = {
        [initialSetting2]: {
          controllingFlowVariableName: "var2",
          controllingFlowVariableFlawed: false,
        } as FlowSettings,
      };
      initialDataSpy.mockResolvedValue({
        data: {},
        schema: {},
        flowVariableSettings: {
          ...flowSettings1,
          ...flowSettings2,
        },
      });

      const wrapper = shallowMount(NodeDialog, getOptions());
      await flushPromises();
      expect(
        wrapper.vm.coreComponent!.possiblyFlawedControllingVariablePaths,
      ).toStrictEqual(new Set([initialSetting1, initialSetting2]));
      expect(flowSettings1.controllingFlowVariableFlawed).toBeUndefined();
      expect(flowSettings2.controllingFlowVariableFlawed).toBeUndefined();
      const getDataSpy = vi
        .spyOn(wrapper.vm.jsonDataService, "data")
        .mockResolvedValue("not_undefined");

      wrapper.vm.coreComponent!.setCurrentData({});
      await wrapper.vm.coreComponent!.getFlowVariableOverrideValue(
        initialSetting1,
        "_dataPath",
      );
      expect(getDataSpy).toHaveBeenCalledWith({
        method: "flowVariables.getFlowVariableOverrideValue",
        options: [
          `{"data":{},"flowVariableSettings":${JSON.stringify(flowSettings1)}}`,
          ["_dataPath"],
        ],
      });

      /**
       * After the first request, flowSetting1 is known to be not flawed anymore,
       * so it will also be used for subsequent requests.
       */
      await wrapper.vm.coreComponent!.getFlowVariableOverrideValue(
        "other",
        "_dataPath",
      );
      expect(getDataSpy).toHaveBeenCalledWith({
        method: "flowVariables.getFlowVariableOverrideValue",
        options: [
          `{"data":{},"flowVariableSettings":${JSON.stringify(flowSettings1)}}`,
          ["_dataPath"],
        ],
      });
    });

    it("unsets flawed variable path if 'getFlowVariableOverrideValue' returns a value", async () => {
      vi.spyOn(JsonDataService.prototype, "data").mockResolvedValue(
        "not undefined",
      );

      const wrapper = shallowMount(NodeDialog, getOptions());

      const persistPath = "my.path";
      const flowSettings = {
        controllingFlowVariableFlawed: true,
      } as FlowSettings;
      await flushPromises();
      wrapper.vm.flowVariablesMap[persistPath] = flowSettings;
      wrapper.vm.coreComponent!.flawedControllingVariablePaths.add(persistPath);

      await wrapper.vm.coreComponent!.getFlowVariableOverrideValue(
        persistPath,
        "_dataPath",
      );

      expect(
        wrapper.vm.coreComponent!.flawedControllingVariablePaths,
      ).toStrictEqual(new Set([]));
      expect(flowSettings.controllingFlowVariableFlawed).toBeFalsy();
    });
  });
});
