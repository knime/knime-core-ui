import {
  type Mock,
  afterEach,
  beforeEach,
  describe,
  expect,
  it,
  vi,
} from "vitest";
import { reactive } from "vue";
import { VueWrapper } from "@vue/test-utils";
import flushPromises from "flush-promises";

import { FunctionButton, SideDrawer } from "@knime/components";
import {
  type ProvidedMethods,
  type VueControlTestProps,
  getControlBase,
  mountJsonFormsControlLabelContent,
} from "@knime/jsonforms/testing";
import FolderLenseIcon from "@knime/styles/img/icons/folder-lense.svg";

import { createPersistSchema } from "@@/test-setup/utils/createPersistSchema";
import type { FlowSettings } from "@/nodeDialog/api/types";
import { injectionKey as flowVariablesMapInjectionKey } from "@/nodeDialog/composables/components/useProvidedFlowVariablesMap";
import SettingsSubPanel from "../../../../layoutComponents/settingsSubPanel/SettingsSubPanel.vue";
import FSLocationTextControl from "../FSLocationTextControl.vue";
import FileChooserControl from "../FileChooserControl.vue";
import SideDrawerContent from "../SideDrawerContent.vue";

describe("FileChooserControl.vue", () => {
  let props: VueControlTestProps<typeof FileChooserControl>,
    wrapper: VueWrapper<any, any>,
    changeValue: Mock;

  const labelForId = "myLabelForId";

  const mountFileChooserControl = ({
    props,
    provide,
    stubs,
    withControllingFlowVariable,
  }: {
    props: VueControlTestProps<typeof FileChooserControl>;
    provide?: Partial<ProvidedMethods>;
    stubs?: Record<string, boolean>;
    withControllingFlowVariable?: string;
  }) => {
    const flowVariablesMap: Record<string, FlowSettings> = reactive({});
    if (withControllingFlowVariable) {
      flowVariablesMap[withControllingFlowVariable] = {
        controllingFlowVariableName: "controllingFlowVariableName",
        exposedFlowVariableName: null,
        controllingFlowVariableAvailable: true,
      };
    }
    const getPanelsContainer = vi.fn().mockReturnValue("body");
    const component = mountJsonFormsControlLabelContent(FileChooserControl, {
      props,
      provide: {
        // @ts-expect-error
        getPanelsContainer,
        setSubPanelExpanded: vi.fn(),
        getPersistSchema: () => ({}),
        [flowVariablesMapInjectionKey as symbol]: flowVariablesMap,
        ...provide,
      },
      stubs: {
        SideDrawerContent: true,
        ...stubs,
      },
    });
    return {
      flowVariablesMap,
      ...component,
    };
  };

  beforeEach(() => {
    props = {
      control: {
        ...getControlBase("test"),
        data: {
          path: {
            path: "myPath",
            timeout: 1000,
            fsCategory: "relative-to-current-hubspace",
          },
        },
        schema: {
          type: "object",
          title: "File path",
          properties: {
            path: {
              type: "object",
            },
          },
          default: "default value",
        },
        uischema: {
          type: "Control",
          scope: "#/properties/view/properties/filePath",
          options: {
            format: "fileChooser",
          },
        },
      },
      labelForId,
      disabled: false,
      isValid: true,
    };

    const component = mountFileChooserControl({
      props,
    });
    wrapper = component.wrapper;
    changeValue = component.changeValue;
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  const hasFolderLenseIcon = (
    wrapper: ReturnType<VueWrapper["findComponent"]>,
  ) => wrapper.findComponent(FolderLenseIcon).exists();
  const findFolderLenseButton = (wrapper: VueWrapper) =>
    wrapper
      .findAllComponents(FunctionButton)
      .filter((fb) => hasFolderLenseIcon(fb))[0];
  const expandSideDrawer = async (wrapper: VueWrapper) => {
    await findFolderLenseButton(wrapper).vm.$emit("click");
    return wrapper.findComponent(SideDrawer);
  };

  it("renders", () => {
    expect(wrapper.findComponent(FSLocationTextControl).exists()).toBe(true);
    expect(wrapper.findComponent(SettingsSubPanel).exists()).toBe(true);
  });

  it("sets labelForId", async () => {
    expect(
      (await expandSideDrawer(wrapper)).findComponent(SideDrawerContent).props()
        .id,
    ).toBe(labelForId);
  });

  it("calls changeValue when applying changes from the side panel", async () => {
    const changedValue = { ...props.control.data.path, path: "new path" };
    const sideDrawer = await expandSideDrawer(wrapper);
    sideDrawer
      .findComponent(SideDrawerContent)
      .vm.$emit("update:modelValue", changedValue);
    await wrapper.findComponent(SettingsSubPanel).vm.$emit("apply");
    expect(changeValue).toHaveBeenCalledWith({
      path: changedValue,
    });
  });

  const createPersistSchemaMock = () =>
    createPersistSchema({
      path: props.control.path,
      leaf: {
        type: "object",
        properties: {
          path: {},
        },
      },
    });

  it.each(["LOCAL", "relative-to-current-hubspace"])(
    "sets default data when unsetting controlling flow variable",
    async (fsCategory) => {
      const stringRepresentation = "myStringRepresentation";
      // @ts-expect-error
      props.control.data.path.fsCategory = "RELATIVE";
      props.control.data.path.context = { fsToString: stringRepresentation };
      if (fsCategory === "LOCAL") {
        props.control.uischema.options!.isLocal = true;
      }
      const { flowVariablesMap, wrapper, changeValue } =
        await mountFileChooserControl({
          props,
          provide: {
            // @ts-expect-error
            getPersistSchema: () => createPersistSchemaMock(),
          },
          withControllingFlowVariable: `${props.control.path}.path` as any,
        });
      flowVariablesMap[
        `${props.control.path}.path` as any
      ].controllingFlowVariableName = null;
      // @ts-ignore
      wrapper.vm.control = { ...wrapper.vm.control };
      await flushPromises();
      expect(changeValue).toHaveBeenCalledWith({
        path: {
          path: "",
          fsCategory,
          timeout: 10000,
          context: {
            fsToString: "",
            fsSpecifier: undefined,
          },
        },
      });
    },
  );

  describe("switches to valid values when mounted", () => {
    it("does not switch to the first valid category if the current category is valid", () => {
      props.control.data.path.fsCategory = "relative-to-embedded-data";
      const { changeValue } = mountFileChooserControl({
        props,
        stubs: {
          FSLocationTextControl: true,
        },
      });
      expect(changeValue).not.toHaveBeenCalled();
    });

    it("switches to current hub space if non-supported fsCategory is given", () => {
      props.control.data.path.fsCategory = "LOCAL";
      props.control.uischema.options!.isLocal = false;
      const { changeValue } = mountFileChooserControl({
        props,
        stubs: {
          FSLocationTextControl: true,
        },
      });
      expect(changeValue).toHaveBeenCalledWith(
        expect.objectContaining({
          path: {
            ...props.control.data.path,
            fsCategory: "relative-to-current-hubspace",
          },
        }),
      );
    });

    it("switches to LOCAL if non-supported fsCategory is given and isLocal is true", () => {
      props.control.data.path.fsCategory = "CONNECTED";
      props.control.uischema.options!.isLocal = true;
      const { changeValue } = mountFileChooserControl({
        props,
        stubs: {
          FSLocationTextControl: true,
        },
      });
      expect(changeValue).toHaveBeenCalledWith(
        expect.objectContaining({
          path: {
            ...props.control.data.path,
            fsCategory: "LOCAL",
          },
        }),
      );
    });

    it("switches to CONNECTED if any other fsCategory is given and ", () => {
      props.control.data.path.fsCategory = "relative-to-current-hubspace";
      props.control.uischema.options!.portIndex = 1;
      const { changeValue } = mountFileChooserControl({
        props,
        stubs: {
          FSLocationTextControl: true,
        },
      });
      expect(changeValue).toHaveBeenCalledWith(
        expect.objectContaining({
          path: {
            ...props.control.data.path,
            fsCategory: "CONNECTED",
          },
        }),
      );
    });

    it("does not switch to a valid category when overwritten by a flow variable.", () => {
      props.control.data.path.fsCategory = "CONNECTED";
      props.control.uischema.options!.portIndex = 1;
      const { changeValue } = mountFileChooserControl({
        props,
        stubs: {
          FSLocationTextControl: true,
        },
        withControllingFlowVariable: `${props.control.path}.path` as any,
      });
      expect(changeValue).not.toHaveBeenCalled();
    });
  });
});
