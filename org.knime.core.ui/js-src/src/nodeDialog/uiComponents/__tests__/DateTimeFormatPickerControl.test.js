import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import flushPromises from "flush-promises";

import { DateTimeFormatInput } from "@knime/components";

import {
  getControlBase,
  initializesJsonFormsControl,
  mountJsonFormsComponent,
} from "@@/test-setup/utils/jsonFormsTestUtils";
import DateTimeFormatPickerControl from "../DateTimeFormatPickerControl.vue";
import DialogLabel from "../label/DialogLabel.vue";

describe("DateTimeFormatPickerControl.vue", () => {
  let defaultProps, wrapper, component;

  beforeEach(async () => {
    defaultProps = {
      control: {
        ...getControlBase("path"),
        data: "dd",
        schema: {
          properties: {
            maxRows: {
              type: "integer",
              title: "Show tooltip",
            },
          },
        },
        uischema: {
          type: "Control",
          scope: "#/properties/view/properties/maxRows",
          options: {
            format: "integer",
          },
        },
      },
    };
    component = await mountJsonFormsComponent(DateTimeFormatPickerControl, {
      props: defaultProps,
    });
    wrapper = component.wrapper;
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  it("renders", () => {
    expect(wrapper.getComponent(DateTimeFormatInput).props()).toMatchObject({
      compact: true,
      disabled: false,
      modelValue: defaultProps.control.data,
      allDefaultFormats: expect.any(Array),
      formatValidator: expect.any(Function),
    });
  });

  it("initializes jsonforms", () => {
    initializesJsonFormsControl(component);
  });

  it("sets labelForId", async () => {
    await wrapper.vm.$nextTick();
    const dialogLabel = wrapper.findComponent(DialogLabel);
    expect(wrapper.getComponent(DateTimeFormatInput).attributes().id).toBe(
      dialogLabel.vm.labelForId,
    );
    expect(dialogLabel.vm.labeledElement).toBeDefined();
    expect(dialogLabel.vm.labeledElement).not.toBeNull();
  });

  it("calls handleChange when interval input is changed", () => {
    const { wrapper, handleChange } = mountJsonFormsComponent(
      DateTimeFormatPickerControl,
      {
        props: defaultProps,
      },
    );
    const changedFormat = "MM";
    wrapper
      .findComponent(DateTimeFormatInput)
      .vm.$emit("update:modelValue", changedFormat);
    expect(handleChange).toHaveBeenCalledWith(
      defaultProps.control.path,
      changedFormat,
    );
  });

  it("sets correct label", () => {
    expect(wrapper.find("label").text()).toBe(defaultProps.control.label);
  });

  it("disables input when controlled by a flow variable", () => {
    const { wrapper } = mountJsonFormsComponent(DateTimeFormatPickerControl, {
      props: defaultProps,
      withControllingFlowVariable: true,
    });
    expect(wrapper.vm.disabled).toBeTruthy();
  });

  it("uses format from provider in options", async () => {
    const formatTypeProvider = "myProvider";
    defaultProps.control.uischema.options.formatProvider = formatTypeProvider;
    let provideFormats;
    const addStateProviderListenerMock = vi.fn((_id, callback) => {
      provideFormats = callback;
    });
    component = mountJsonFormsComponent(DateTimeFormatPickerControl, {
      props: defaultProps,
      provide: { addStateProviderListenerMock },
    });
    wrapper = component.wrapper;
    expect(addStateProviderListenerMock).toHaveBeenCalledWith(
      { id: formatTypeProvider },
      expect.anything(),
    );
    expect(wrapper.getComponent(DateTimeFormatInput).props()).toMatchObject({
      allDefaultFormats: [],
    });

    const formatsToProvide = [
      {
        format: "yy-yyyy",
        temporalType: "DATE",
        category: "EUROPEAN",
        example: "21-2021",
      },
    ];

    provideFormats(formatsToProvide);
    await flushPromises();
    expect(wrapper.getComponent(DateTimeFormatInput).props()).toMatchObject({
      allDefaultFormats: formatsToProvide,
    });
  });
});
