import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import {
  mountJsonFormsComponent,
  getControlBase,
  initializesJsonFormsControl,
} from "@@/test-setup/utils/jsonFormsTestUtils";
import FileChooserInput from "../FileChooserInput.vue";
import { VueWrapper } from "@vue/test-utils";
import SettingsSubPanel from "@/nodeDialog/layoutComponents/settingsSubPanel/SettingsSubPanel.vue";
import LabeledInput from "@/nodeDialog/uiComponents/label/LabeledInput.vue";
import DialogLabel from "@/nodeDialog/uiComponents/label/DialogLabel.vue";
import SideDrawer from "webapps-common/ui/components/SideDrawer.vue";
import FolderLenseIcon from "webapps-common/ui/assets/img/icons/folder-lense.svg";
import SideDrawerContent from "../SideDrawerContent.vue";
import FunctionButton from "webapps-common/ui/components/FunctionButton.vue";
import SideDrawerContentBase from "../SideDrawerContentBase.vue";
import flushPromises from "flush-promises";
import FSLocationTextInput from "../FSLocationTextInput.vue";

describe("FileChooserInput.vue", () => {
  let props: any, wrapper: VueWrapper<any, any>, component: any;

  beforeEach(async () => {
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
              properties: {
                path: {
                  type: "string",
                },
                fsCategory: {
                  oneOf: [
                    {
                      const: "relative-to-current-hubspace",
                      title: "relative-to-current-hubspace",
                    },
                    {
                      const: "CUSTOM_URL",
                      title: "CUSTOM_URL",
                    },
                  ],
                },
                timeout: {
                  type: "integer",
                  format: "int32",
                },
              },
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
    };

    const getPanelsContainerMock = vi.fn().mockReturnValue("body");
    component = await mountJsonFormsComponent(FileChooserInput, {
      props,
      provide: {
        getPanelsContainerMock,
      },
      stubs: {
        SideDrawerContentBase: true,
      },
    });
    wrapper = component.wrapper;
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
    expect(wrapper.findComponent(FSLocationTextInput).exists()).toBe(true);
    expect(wrapper.findComponent(SettingsSubPanel).exists()).toBe(true);
    expect(wrapper.findComponent(LabeledInput).exists()).toBe(true);
  });

  it("sets labelForId", async () => {
    const dialogLabel = wrapper.findComponent(DialogLabel) as any;
    expect(
      (await expandSideDrawer(wrapper)).findComponent(SideDrawerContent).props()
        .id,
    ).toBe(dialogLabel.vm.labelForId);
  });

  it("initializes jsonforms", () => {
    initializesJsonFormsControl(component);
  });

  it("calls handleChange when applying changes from the side panel", async () => {
    const changedValue = { ...props.control.data.path, path: "new path" };
    const sideDrawer = await expandSideDrawer(wrapper);
    sideDrawer
      .findComponent(SideDrawerContentBase)
      .vm.$emit("update:modelValue", changedValue);
    await wrapper.findComponent(SettingsSubPanel).vm.$emit("apply");
    expect(component.handleChange).toHaveBeenCalledWith(props.control.path, {
      path: changedValue,
    });
  });

  it("disables input when controlled by a flow variable", () => {
    const { wrapper } = mountJsonFormsComponent(FileChooserInput, {
      props,
      withControllingFlowVariable: `${props.control.path}.path` as any,
    });
    expect(findFolderLenseButton(wrapper).props().disabled).toBe(true);
  });

  it("sets default data when unsetting controlling flow variable", async () => {
    const stringRepresentation = "myStringRepresentation";
    props.control.data.path.fsCategory = "RELATIVE";
    props.control.data.path.context = { fsToString: stringRepresentation };
    const { flowVariablesMap, wrapper, handleChange } =
      await mountJsonFormsComponent(FileChooserInput, {
        props,
        withControllingFlowVariable: `${props.control.path}.path` as any,
      });
    flowVariablesMap[
      `${props.control.path}.path` as any
    ].controllingFlowVariableName = null as any;
    // @ts-ignore
    wrapper.vm.control = { ...wrapper.vm.control };
    await flushPromises();
    expect(handleChange).toHaveBeenCalledWith(props.control.path, {
      path: {
        path: "",
        fsCategory: "relative-to-current-hubspace",
        timeout: 10000,
      },
    });
  });
});
