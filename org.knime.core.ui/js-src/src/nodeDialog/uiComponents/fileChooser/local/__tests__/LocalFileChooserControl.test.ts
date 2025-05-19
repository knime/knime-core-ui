import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import type { VueWrapper } from "@vue/test-utils";

import { FunctionButton, InputField } from "@knime/components";
import {
  type VueControlTestProps,
  getControlBase,
  mountJsonFormsControlLabelContent,
} from "@knime/jsonforms/testing";

import DialogFileExplorer from "../../DialogFileExplorer.vue";
import FileBrowserButton from "../../FileBrowserButton.vue";
import LabeledLocalFileChooserControl from "../LocalFileChooserControl.vue";
import type LocalFileChooserControl from "../LocalFileChooserControl.vue";

describe("LocalFileChooserControl.vue", () => {
  let props: VueControlTestProps<typeof LocalFileChooserControl>,
    wrapper: VueWrapper;

  const providedForSubPanel = {
    getPanelsContainer: vi.fn().mockReturnValue("body"),
    setSubPanelExpanded: vi.fn(),
  };

  const labelForId = "myLabelForId";

  beforeEach(async () => {
    props = {
      control: {
        ...getControlBase("test"),
        data: "test",
        schema: {
          properties: {
            localFile: {
              type: "string",
              title: "Local File",
            },
          },
          default: "default value",
        },
        uischema: {
          type: "Control",
          scope: "#/properties/view/properties/localFile",
          options: {
            format: "localFileChooser",
            isWriter: false,
          },
        },
      },
      labelForId,
      disabled: false,
      isValid: true,
    };

    const component = await mountJsonFormsControlLabelContent(
      LabeledLocalFileChooserControl,
      {
        props,
        // @ts-expect-error
        provide: providedForSubPanel,
        stubs: { DialogFileExplorer: true },
      },
    );
    wrapper = component.wrapper;
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  it("renders", () => {
    expect(wrapper.findComponent(InputField).exists()).toBe(true);
    expect(wrapper.findComponent(FileBrowserButton).exists()).toBe(true);
    expect(wrapper.findComponent(DialogFileExplorer).exists()).toBe(false);
  });

  it("sets labelForId", () => {
    expect(wrapper.getComponent(InputField).props().id).toBe(labelForId);
  });

  it("calls changeValue when text input is changed", () => {
    const { wrapper, changeValue } = mountJsonFormsControlLabelContent(
      LabeledLocalFileChooserControl,
      {
        props,
        // @ts-expect-error
        provide: providedForSubPanel,
        stubs: { DialogFileExplorer: true },
      },
    );
    const changedTextInput = "Shaken not stirred";
    wrapper
      .findComponent(InputField)
      .vm.$emit("update:modelValue", changedTextInput);
    expect(changeValue).toHaveBeenCalledWith(changedTextInput);
  });

  const clickFileBrowserButton = (wrapper: VueWrapper) =>
    wrapper
      .findComponent(FileBrowserButton)
      .findComponent(FunctionButton)
      .vm.$emit("click");

  it("sets correct initial value", async () => {
    await clickFileBrowserButton(wrapper);
    expect(
      wrapper.findComponent(DialogFileExplorer).props().initialFilePath,
    ).toBe(props.control.data);
  });

  it("sets correct browsing options", async () => {
    props.control.uischema.options!.fileExtension = "pdf";
    props.control.uischema.options!.isWriter = true;

    const { wrapper } = mountJsonFormsControlLabelContent(
      LabeledLocalFileChooserControl,
      {
        props,
        // @ts-expect-error
        provide: providedForSubPanel,
        stubs: { DialogFileExplorer: true },
      },
    );
    await clickFileBrowserButton(wrapper);
    expect(wrapper.findComponent(DialogFileExplorer).props()).toMatchObject({
      filteredExtensions: ["pdf"],
      appendedExtension: "pdf",
      isWriter: true,
    });
  });

  it("disables input and button when disabled by prop", async () => {
    await wrapper.setProps({ disabled: true });
    expect(wrapper.findComponent(InputField).props().disabled).toBe(true);
    expect(wrapper.findComponent(FileBrowserButton).props().disabled).toBe(
      true,
    );
  });
});
