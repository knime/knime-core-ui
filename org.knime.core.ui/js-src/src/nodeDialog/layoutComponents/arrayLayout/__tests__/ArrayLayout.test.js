/* eslint-disable max-lines */
/* eslint-disable no-undefined */
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { ref } from "vue";
import { mount } from "@vue/test-utils";
import { DispatchRenderer } from "@jsonforms/vue";
import * as jsonformsVueModule from "@jsonforms/vue";
import flushPromises from "flush-promises";

import { FunctionButton } from "@knime/components";
import ArrowDownIcon from "@knime/styles/img/icons/arrow-down.svg";
import ArrowUpIcon from "@knime/styles/img/icons/arrow-up.svg";
import TrashIcon from "@knime/styles/img/icons/trash.svg";

import { injectionKey as dirtySettingsInjectionKey } from "../../../composables/nodeDialog/useDirtySettings";
import { editResetButtonFormat } from "../../../renderers/editResetButtonRenderer";
import { elementCheckboxFormat } from "../../../renderers/elementCheckboxRenderer";
import ArrayLayout from "../ArrayLayout.vue";
import ArrayLayoutItem from "../ArrayLayoutItem.vue";
import ArrayLayoutItemControls from "../ArrayLayoutItemControls.vue";

let control;

const controlDataLength = 3;

beforeEach(() => {
  control = ref({
    visible: true,
    cells: [],
    data: [
      {
        borderStyle: "DASHED",
        color: "blue",
        label: undefined,
        size: 1,
        value: "0",
      },
      {
        borderStyle: "DOTTED",
        color: "red",
        label: undefined,
        size: 1,
        value: "1",
      },
      {
        borderStyle: "SOLID",
        color: "green",
        label: undefined,
        size: 1,
        value: "2",
      },
    ],
    path: "view/referenceLines",
    schema: {
      type: "object",
      properties: {
        borderStyle: {
          oneOf: [
            {
              const: "DASHED",
              title: "Dashed",
            },
            {
              const: "DOTTED",
              title: "Dotted",
            },
            {
              const: "SOLID",
              title: "Solid",
            },
          ],
          title: "Borderstyle",
          default: "DASHED",
        },
        color: {
          type: "string",
          title: "Color",
          default: "blue",
        },
        label: {
          type: "string",
          title: "Label",
        },
        size: {
          type: "integer",
          format: "int32",
          title: "Size",
          default: 1,
          minimum: 0,
          maximum: 10,
        },
        value: {
          type: "string",
          title: "Value",
          default: "0",
        },
      },
    },
    uischema: {
      type: "Control",
      scope: "#/properties/view/properties/referenceLines",
      options: {
        arrayElementTitle: "ElementTitle",
        detail: {
          value: {
            type: "Control",
            scope: "#/properties/value",
          },
          label: {
            type: "Control",
            scope: "#/properties/label",
          },
          borderStyle: {
            type: "Control",
            scope: "#/properties/borderStyle",
            options: {
              format: "radio",
              radioLayout: "horizontal",
            },
          },
          horizontalLayout: {
            type: "HorizontalLayout",
            elements: [
              { type: "Control", scope: "#/properties/size" },
              { type: "Control", scope: "#/properties/color" },
            ],
          },
        },
      },
    },
  });
});

const mountArrayLayout = ({ props, provide }) => {
  const { arrayControlMocks } = provide || {};
  vi.spyOn(jsonformsVueModule, "useJsonFormsArrayControl").mockReturnValue({
    addItem: arrayControlMocks?.addItem ?? vi.fn(() => () => {}),
    moveDown: arrayControlMocks?.moveDown ?? vi.fn(() => () => {}),
    moveUp: arrayControlMocks?.moveUp ?? vi.fn(() => () => {}),
    removeItems: arrayControlMocks?.removeItems ?? vi.fn(() => () => {}),
    control: props.control,
  });

  const handleChange = vi.fn();

  vi.spyOn(jsonformsVueModule, "useJsonFormsControl").mockReturnValue({
    handleChange,
    control: props.control,
  });
  return {
    wrapper: mount(ArrayLayout, {
      props,
      global: {
        provide: {
          [dirtySettingsInjectionKey]: {
            getSettingState: () => vi.fn(),
            constructSettingState: () => vi.fn(),
          },
          updateData: vi.fn(),
          isTriggerActive: vi.fn(),
          sendAlert: vi.fn(),
          createArrayAtPath: vi.fn(() => ({})),
          ...provide,
        },
        stubs: {
          DispatchRenderer: true,
        },
      },
    }),
    handleChange,
  };
};

describe("ArrayLayout.vue", () => {
  let wrapper, handleChange;

  beforeEach(async () => {
    const component = await mountArrayLayout({
      props: { control },
    });
    wrapper = component.wrapper;
    handleChange = component.handleChange;
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  it("renders", () => {
    expect(wrapper.getComponent(ArrayLayout).exists()).toBe(true);
  });

  const schemaDefaultValue = {
    borderStyle: "DASHED",
    color: "blue",
    label: undefined,
    size: 1,
    value: "0",
    _id: expect.any(String),
  };

  it("renders an add button", () => {
    const addItem = vi.fn(() => () => {});
    const { wrapper } = mountArrayLayout({
      props: { control },
      provide: { arrayControlMocks: { addItem } },
    });
    const addButton = wrapper.find(".array > button");
    expect(addButton.text()).toBe("New");
    addButton.element.click();

    expect(addItem).toHaveBeenCalledWith(
      control.value.path,
      schemaDefaultValue,
    );
  });

  it("uses provided default value if present", () => {
    const addItem = vi.fn(() => () => {});
    control.value.uischema.providedOptions = ["elementDefaultValue"];

    let provideDefault;
    const addStateProviderListener = vi.fn((_id, callback) => {
      provideDefault = callback;
    });
    const { wrapper } = mountArrayLayout({
      props: { control },
      provide: { addStateProviderListener, arrayControlMocks: { addItem } },
    });
    expect(addStateProviderListener).toHaveBeenCalledWith(
      {
        scope: control.value.uischema.scope,
        providedOptionName: "elementDefaultValue",
      },
      expect.anything(),
    );
    const providedDefault = {
      borderStyle: "DASHED",
      color: "red",
      label: "My default Label",
      size: 1,
      value: "0",
      _id: expect.any(String),
    };
    const button = wrapper.find(".array > button").element;

    button.click();
    provideDefault(providedDefault);
    button.click();
    expect(addItem).toHaveBeenNthCalledWith(
      1,
      control.value.path,
      schemaDefaultValue,
    );

    expect(addItem).toHaveBeenNthCalledWith(
      2,
      control.value.path,
      providedDefault,
    );
  });

  it("sets add button text", () => {
    const customAddButtonText = "My add button text";
    control.value.uischema.options.addButtonText = customAddButtonText;
    const { wrapper } = mountArrayLayout({
      props: { control },
    });
    const addButton = wrapper.find(".array > button");
    expect(addButton.text()).toBe(customAddButtonText);
  });

  it("adds default item", async () => {
    const addItem = vi.fn(() => () => {});
    const { wrapper } = mountArrayLayout({
      props: { control },
      provide: {
        arrayControlMocks: {
          addItem,
        },
      },
    });
    wrapper.vm.addDefaultItem();
    expect(addItem).toHaveBeenCalledWith(control.value.path, {
      borderStyle: "DASHED",
      color: "blue",
      size: 1,
      value: "0",
      _id: expect.any(String),
    });
    await flushPromises();
  });

  it("deletes item", async () => {
    const removeItems = vi.fn(() => () => {});
    const { wrapper } = mountArrayLayout({
      props: { control },
      provide: {
        arrayControlMocks: {
          removeItems,
        },
      },
    });
    const index = 1;
    wrapper.vm.deleteItem(index);
    expect(removeItems).toHaveBeenCalledWith(expect.anything(), [index]);
    await flushPromises();
  });

  it("moves item up", async () => {
    const moveUp = vi.fn(() => () => {});
    const { wrapper } = mountArrayLayout({
      props: { control },
      provide: {
        arrayControlMocks: {
          moveUp,
        },
      },
    });
    const index = 1;
    await wrapper
      .findAllComponents(ArrayLayoutItemControls)
      .at(index)
      .vm.$emit("moveUp");
    expect(moveUp).toHaveBeenCalledWith(control.value.path, index);
    await flushPromises();
  });

  it("moves item down", async () => {
    const moveDown = vi.fn(() => () => {});
    const { wrapper } = mountArrayLayout({
      props: { control },
      provide: {
        arrayControlMocks: {
          moveDown,
        },
      },
    });
    const index = 1;
    await wrapper
      .findAllComponents(ArrayLayoutItemControls)
      .at(index)
      .vm.$emit("moveDown");
    expect(moveDown).toHaveBeenCalledWith(control.value.path, index);
    await flushPromises();
  });

  it("renders an edit/reset button if configured to do so", () => {
    control.value.uischema.options.withEditAndReset = true;
    const { wrapper } = mountArrayLayout({
      props: { control },
    });
    const itemControls = wrapper.findAllComponents(ArrayLayoutItemControls);
    const firstDispatchedProps = itemControls
      .at(0)
      .findComponent(DispatchRenderer)
      .props();
    expect(firstDispatchedProps.uischema.options.format).toBe(
      editResetButtonFormat,
    );
    expect(firstDispatchedProps.uischema.scope).toBe("#/properties/_edit");
    expect(firstDispatchedProps.path).toBe("view/referenceLines.0");
    expect(firstDispatchedProps.schema.properties._edit.type).toBe("boolean");
  });

  it("renders a checkbox next to the header when given an elementCheckboxScope", () => {
    const elementCheckboxScope = "#/properties/booleanInput";
    control.value.uischema.options.elementCheckboxScope = elementCheckboxScope;
    const { wrapper } = mountArrayLayout({
      props: { control },
    });
    const firstDispatchedProps = wrapper
      .findAllComponents(ArrayLayoutItem)
      .at(0)
      .findComponent(DispatchRenderer)
      .props();
    expect(firstDispatchedProps.uischema.options.format).toBe(
      elementCheckboxFormat,
    );
    expect(firstDispatchedProps.uischema.scope).toBe(elementCheckboxScope);
  });

  it("does not render sort buttons when showSortButtons is not present or false", () => {
    const { wrapper } = mountArrayLayout({
      props: { control },
    });
    const numberDataItems = control.value.data.length;
    const itemControls = wrapper.findAllComponents(ArrayLayoutItemControls);

    expect(itemControls).toHaveLength(numberDataItems);
    const itemControlsWithArrowUp = itemControls.filter((wrapper) =>
      wrapper.findComponent(ArrowUpIcon).exists(),
    );
    const itemControlsWithArrowDown = itemControls.filter((wrapper) =>
      wrapper.findComponent(ArrowDownIcon).exists(),
    );

    expect(itemControlsWithArrowUp).toHaveLength(0);
    expect(itemControlsWithArrowDown).toHaveLength(0);
  });

  it("renders headers and uses card styled items when elementLayout is VERTICAL_CARD (default)", () => {
    const { wrapper } = mountArrayLayout({
      props: { control },
    });
    expect(wrapper.find(".item-header").exists()).toBeTruthy();
    expect(wrapper.find(".item-header").text()).toBe("ElementTitle 1");
    expect(wrapper.vm.useCardLayout).toBeTruthy();
  });

  it("does not render headers or card layout when elementLayout is HORIZONTAL_SINGLE_LINE", () => {
    delete control.value.uischema.options.arrayElementTitle;
    control.value.uischema.options.elementLayout = "HORIZONTAL_SINGLE_LINE";
    const { wrapper } = mountArrayLayout({
      props: { control },
    });
    expect(wrapper.find(".item-header").exists()).toBeFalsy();
    expect(wrapper.vm.useCardLayout).toBeFalsy();
  });

  it("does not render card layout or header if elementLayout is HORIZONTAL_SINGLE_LINE, even if arrayElementTitle is set", () => {
    control.value.uischema.options.elementLayout = "HORIZONTAL_SINGLE_LINE";
    control.value.uischema.options.arrayElementTitle = "ShouldNotShow";
    const { wrapper } = mountArrayLayout({
      props: { control },
    });
    expect(wrapper.find(".item-header").exists()).toBeFalsy();
    expect(wrapper.vm.useCardLayout).toBeFalsy();
  });

  it.each([
    {
      button: "move up button",
      position: "the first",
      itemNum: 0,
      moveUpDisabled: true,
      moveDownDisabled: false,
    },
    {
      button: "none of the sort buttons",
      position: "any non-boundary",
      itemNum: 1,
      moveUpDisabled: false,
      moveDownDisabled: false,
    },
    {
      button: "move down button",
      position: "the last",
      itemNum: 2,
      moveUpDisabled: false,
      moveDownDisabled: true,
    },
  ])(
    "disables $button for $position item when showSortButtons is true",
    ({ itemNum, moveUpDisabled, moveDownDisabled }) => {
      control.value.uischema.options.showSortButtons = true;
      const { wrapper } = mountArrayLayout({
        props: { control },
      });
      const itemControls = wrapper.findAll(".item-controls");
      const itemControlsButtons = itemControls
        .at(itemNum)
        .findAllComponents(FunctionButton);
      expect(itemControlsButtons.at(0).vm.disabled).toBe(moveUpDisabled);
      expect(itemControlsButtons.at(1).vm.disabled).toBe(moveDownDisabled);
    },
  );

  it.each([
    {
      render: "not render",
      condition: "present and true",
      value: true,
      numberIcons: 0,
    },
    {
      render: "render",
      condition: "false",
      value: false,
      numberIcons: controlDataLength,
    },
    {
      render: "render",
      condition: "not present",
      value: null,
      numberIcons: controlDataLength,
    },
  ])(
    "does $render add and delete buttons when hasFixedSize is $condition",
    ({ value, numberIcons }) => {
      if (value === null) {
        delete control.value.uischema.options.hasFixedSize;
      } else {
        control.value.uischema.options.hasFixedSize = value;
      }

      const { wrapper } = mountArrayLayout({
        props: { control },
      });

      const itemControls = wrapper.findAllComponents(ArrayLayoutItemControls);
      const itemControlsWithTrash = itemControls.filter((wrapper) =>
        wrapper.findComponent(TrashIcon).exists(),
      );

      expect(itemControlsWithTrash).toHaveLength(numberIcons);
    },
  );

  it("displays provided title and subtitle", async () => {
    control.value.uischema.providedOptions = [
      "arrayElementTitle",
      "elementSubTitle",
    ];

    const provideState = [];
    const addStateProviderListener = vi.fn((_id, callback) => {
      provideState.push(callback);
    });
    const { wrapper } = mountArrayLayout({
      props: { control },
      provide: { addStateProviderListener },
    });
    expect(addStateProviderListener).toHaveBeenCalledWith(
      {
        providedOptionName: "arrayElementTitle",
        scope: control.value.uischema.scope,
        indexIds: expect.anything(),
        indices: [0],
      },
      expect.anything(),
    );
    expect(addStateProviderListener).toHaveBeenCalledWith(
      {
        providedOptionName: "elementSubTitle",
        scope: control.value.uischema.scope,
        indexIds: expect.anything(),
        indices: [0],
      },
      expect.anything(),
    );
    const providedState = "provided";
    provideState.forEach((callback) => callback(providedState));
    await flushPromises();
    expect(wrapper.find(".item-header").text()).toBe(
      providedState + providedState,
    );
  });

  it("sets initia ids", () => {
    expect(handleChange).toHaveBeenCalledTimes(controlDataLength);
  });

  describe("edit/reset buttons initial state", () => {
    let resolveIsActivePromise = () => {};

    beforeEach(() => {
      vi.useFakeTimers();
      control.value.uischema.options.withEditAndReset = true;
      const component = mountArrayLayout({
        props: { control },
        provide: {
          isTriggerActive: vi.fn().mockReturnValue(
            new Promise((resolve) => {
              resolveIsActivePromise = resolve;
            }),
          ),
        },
      });

      wrapper = component.wrapper;
    });

    afterEach(() => {
      vi.useRealTimers();
    });

    const getEditResetButtons = () =>
      wrapper
        .findAllComponents(DispatchRenderer)
        .filter((c) => c.props("schema").properties?._edit?.type === "boolean");

    it("shows loading icon while loading", async () => {
      const editResetButtons = getEditResetButtons();
      expect(editResetButtons.length).toBe(3);
      expect(editResetButtons[0].attributes("is-loading")).toBeUndefined();
      vi.runAllTimers();
      await wrapper.vm.$nextTick();
      expect(editResetButtons[0].attributes("is-loading")).toBe("");
      // simulate setting ids
      control.value = {
        ...control.value,
        data: control.value.data.map((data, index) => ({
          ...data,
          _id: `id-${index}`,
        })),
      };
      await wrapper.vm.$nextTick();
      resolveIsActivePromise({
        state: "SUCCESS",
        result: control.value.data.map((_, index) => ({
          indices: [`id-${index}`],
          isActive: index === 0,
        })),
      });
      await flushPromises();
      const editResetButtonsAfterResolve = getEditResetButtons();
      expect(
        editResetButtonsAfterResolve[0].attributes("is-loading"),
      ).toBeUndefined();
      expect(
        editResetButtonsAfterResolve[0].attributes("initial-is-edited"),
      ).toBe("");
      expect(
        editResetButtonsAfterResolve[1].attributes("initial-is-edited"),
      ).toBeUndefined();
    });
  });
});
