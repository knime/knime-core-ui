import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import {
  mountJsonFormsComponent,
  initializesJsonFormsControl,
  getControlBase,
} from "@@/test-setup/utils/jsonFormsTestUtils";
import DropdownInput from "../DropdownInput.vue";
import LabeledInput from "../label/LabeledInput.vue";
import DialogLabel from "../label/DialogLabel.vue";
import Dropdown from "webapps-common/ui/components/forms/Dropdown.vue";
import flushPromises from "flush-promises";

describe("DropdownInput.vue", () => {
  let wrapper, props, component;

  const path = "test";

  beforeEach(async () => {
    props = {
      control: {
        ...getControlBase(path),
        data: "Universe_0_0",
        schema: {
          title: "Y Axis Column",
        },
        uischema: {
          type: "Control",
          scope: "#/properties/view/properties/yAxisColumn",
          options: {
            format: "columnSelection",
            showRowKeys: false,
            showNoneColumn: false,
            possibleValues: [
              {
                id: "Universe_0_0",
                text: "Universe_0_0",
              },
              {
                id: "Universe_0_1",
                text: "Universe_0_1",
              },
              {
                id: "Universe_1_0",
                text: "Universe_1_0",
              },
              {
                id: "Universe_1_1",
                text: "Universe_1_1",
              },
            ],
          },
        },
      },
    };
    component = await mountJsonFormsComponent(DropdownInput, {
      props,
      withControllingFlowVariable: true,
    });
    wrapper = component.wrapper;
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  it("renders", () => {
    expect(wrapper.getComponent(DropdownInput).exists()).toBe(true);
    expect(wrapper.findComponent(LabeledInput).exists()).toBe(true);
    expect(wrapper.findComponent(Dropdown).exists()).toBe(true);
  });

  it("sets labelForId", () => {
    const dialogLabel = wrapper.findComponent(DialogLabel);
    expect(wrapper.getComponent(Dropdown).props().id).toBe(
      dialogLabel.vm.labelForId,
    );
    expect(dialogLabel.vm.labeledElement).toBeDefined();
    expect(dialogLabel.vm.labeledElement).not.toBeNull();
  });

  it("initializes jsonforms", () => {
    initializesJsonFormsControl(component);
  });

  describe("reacts to dropdown input change", () => {
    let dirtySettingsMock;

    beforeEach(() => {
      dirtySettingsMock = vi.fn();
    });

    afterEach(() => {
      vi.clearAllMocks();
    });

    it("calls updateData when input is changed", async () => {
      const { wrapper, updateData } = await mountJsonFormsComponent(
        DropdownInput,
        { props },
        {
          "pagebuilder/dialog": {
            actions: { dirtySettings: dirtySettingsMock },
            namespaced: true,
          },
        },
      );
      const changedDropdownInput = "Shaken not stirred";
      wrapper
        .findComponent(Dropdown)
        .vm.$emit("update:modelValue", changedDropdownInput);
      expect(updateData).toHaveBeenCalledWith(
        expect.anything(),
        props.control.path,
        changedDropdownInput,
      );
      expect(dirtySettingsMock).not.toHaveBeenCalled();
    });

    it("indicates model settings change when model setting is changed", async () => {
      const { wrapper, updateData } = await mountJsonFormsComponent(
        DropdownInput,
        {
          props: {
            ...props,
            control: {
              ...props.control,
              uischema: {
                ...props.control.schema,
                scope: "#/properties/model/properties/yAxisColumn",
              },
            },
          },
          modules: {
            "pagebuilder/dialog": {
              actions: { dirtySettings: dirtySettingsMock },
              namespaced: true,
            },
          },
        },
      );
      const changedDropdownInput = "Shaken not stirred";
      wrapper
        .findComponent(Dropdown)
        .vm.$emit("update:modelValue", changedDropdownInput);
      expect(dirtySettingsMock).toHaveBeenCalledWith(expect.anything(), true);
      expect(updateData).toHaveBeenCalledWith(
        expect.anything(),
        props.control.path,
        changedDropdownInput,
      );
    });
  });

  it("sets correct initial value", () => {
    expect(wrapper.findComponent(Dropdown).vm.modelValue).toBe(
      props.control.data,
    );
  });

  it("sets correct label", () => {
    expect(wrapper.find("label").text()).toBe(props.control.label);
  });

  it("sets placeholder text correctly if possible values are not yet available", async () => {
    props.control.uischema.options.possibleValues = [];
    props.asyncInitialOptions = new Promise((_resolve) => {});
    const { wrapper } = mountJsonFormsComponent(DropdownInput, { props });
    await flushPromises();
    expect(wrapper.findComponent(Dropdown).props().placeholder).toBe("Loading");
  });

  it("sets placeholder text correctly if possible values are empty", async () => {
    props.control.uischema.options.possibleValues = [];
    const { wrapper } = mountJsonFormsComponent(DropdownInput, { props });
    await flushPromises();
    expect(wrapper.findComponent(Dropdown).props().placeholder).toBe(
      "No values present",
    );
  });

  it("sets placeholder text correctly if there are possible values present", async () => {
    props.control.data = "";
    const { wrapper } = mountJsonFormsComponent(DropdownInput, { props });
    await flushPromises();
    expect(wrapper.findComponent(Dropdown).props().placeholder).toBe(
      "No value selected",
    );
  });

  it("disables dropdown when controlled by a flow variable", () => {
    expect(wrapper.vm.disabled).toBeTruthy();
    expect(wrapper.findComponent(Dropdown).vm.disabled).toBeTruthy();
  });

  it("does not disable dropdown when not controlled by a flow variable", async () => {
    delete props.control.rootSchema.flowVariablesMap;
    const { wrapper } = await mountJsonFormsComponent(DropdownInput, { props });
    await flushPromises();
    expect(wrapper.vm.disabled).toBeFalsy();
    expect(wrapper.findComponent(Dropdown).vm.disabled).toBeFalsy();
  });

  it("disables dropdown when there are no possible values", async () => {
    props.control.uischema.options.possibleValues = [];
    const { wrapper } = await mountJsonFormsComponent(DropdownInput, { props });
    await flushPromises();
    expect(wrapper.findComponent(Dropdown).vm.disabled).toBeTruthy();
  });

  describe("dependencies to other settings", () => {
    let settingsChangeCallback,
      initialSettingsChangeCallback,
      wrapper,
      dependencies,
      getDataMock,
      sendAlert,
      unregisterWatcher,
      newSettings;

    const dependenciesUischema = ["foo", "bar"];
    const result = [
      { id: "first", text: "First" },
      { id: "second", text: "Second" },
    ];
    const updateHandler = "UpdateHandler";

    beforeEach(() => {
      newSettings = {
        view: { foo: "foo", bar: "bar" },
        model: { baz: "baz" },
      };
      props.control.uischema.options.dependencies = dependenciesUischema;
      props.control.uischema.options.choicesUpdateHandler = updateHandler;
      getDataMock = vi.fn(() => {
        return {
          result,
          state: "SUCCESS",
          message: null,
        };
      });
      const comp = mountJsonFormsComponent(DropdownInput, {
        props,
        provide: { getDataMock },
      });
      wrapper = comp.wrapper;
      sendAlert = comp.sendAlert;

      const callbacks = comp.callbacks;
      const firstWatcherCall = callbacks[0];
      unregisterWatcher = comp.unregisterWatcher;
      settingsChangeCallback = firstWatcherCall.transformSettings;
      initialSettingsChangeCallback = firstWatcherCall.init;
      dependencies = firstWatcherCall.dependencies;
      wrapper.vm.cancel = vi.fn();
    });

    it("registers watcher", () => {
      expect(settingsChangeCallback).toBeDefined();
      expect(dependencies).toStrictEqual(dependenciesUischema);
    });

    it("deregisters watcher on unmount", () => {
      wrapper.unmount();
      expect(unregisterWatcher).toHaveBeenCalled();
    });

    it("requests new data if dependencies change", () => {
      settingsChangeCallback({
        view: { foo: "foo", bar: "bar" },
        model: { baz: "baz" },
      });
      expect(getDataMock).toHaveBeenCalledWith({
        method: "settings.update",
        options: [
          expect.anything(),
          updateHandler,
          {
            foo: "foo",
            bar: "bar",
            baz: "baz",
          },
        ],
      });
    });

    it("sets new options and selected the first option", async () => {
      await settingsChangeCallback(newSettings);
      await flushPromises();
      expect(wrapper.vm.options).toStrictEqual(result);
      expect(newSettings[path]).toBe(result[0].id);
    });

    it("sets new options without changing the data on the initial update", async () => {
      initialSettingsChangeCallback(newSettings);
      await flushPromises();
      expect(wrapper.vm.options).toStrictEqual(result);
      expect(newSettings[path]).toBeUndefined();
    });

    it("selects null if the fetched options are empty", async () => {
      getDataMock.mockImplementation(() => ({
        result: [],
        state: "SUCCESS",
        message: null,
      }));
      settingsChangeCallback(newSettings);
      await flushPromises();
      expect(wrapper.vm.options).toStrictEqual([]);
      expect(newSettings[path]).toBeNull();
    });

    it("sets empty options and warns about error on state FAIL", async () => {
      const message = "Error message";
      getDataMock.mockImplementation(() => ({
        result: null,
        state: "FAIL",
        message,
      }));
      settingsChangeCallback(newSettings);
      await flushPromises();
      expect(wrapper.vm.options).toStrictEqual([]);
      expect(newSettings[path]).toBeNull();
      expect(sendAlert).toHaveBeenCalledWith({
        message: "Error message",
        type: "error",
      });
    });
  });

  it("sets initial options if provided", async () => {
    const customOptions = [{ id: "foo", text: "bar" }];
    props.asyncInitialOptions = Promise.resolve(customOptions);
    const { wrapper } = mountJsonFormsComponent(DropdownInput, { props });
    await flushPromises();
    expect(wrapper.vm.options).toStrictEqual(customOptions);
  });
});
