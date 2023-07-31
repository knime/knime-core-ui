import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import {
  initializesJsonFormsControl,
  mountJsonFormsComponent,
} from "@@/test-setup/utils/jsonFormsTestUtils";
import ColumnSelect from "../ColumnSelect.vue";
import DropdownInput from "../DropdownInput.vue";
import Dropdown from "webapps-common/ui/components/forms/Dropdown.vue";

describe("ColumnSelect.vue", () => {
  let wrapper, props, path, component, updateData;

  beforeEach(async () => {
    path = "control path mock";
    props = {
      control: {
        path,
        visible: true,
        data: {
          selected: "Universe_0_0",
        },
        label: "Column Selection",
        schema: {
          type: "object",
          properties: {
            selected: {
              type: "array",
            },
          },
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
                compatibleTypes: ["Type_0_0", "OtherType_0_0"],
              },
              {
                id: "Universe_0_1",
                text: "Universe_0_1",
                compatibleTypes: ["Type_0_1", "OtherType_0_1"],
              },
              {
                id: "Universe_1_0",
                text: "Universe_1_0",
                compatibleTypes: ["Type_1_0", "OtherType_1_0"],
              },
              {
                id: "Universe_1_1",
                text: "Universe_1_1",
                compatibleTypes: ["Type_1_1", "OtherType_1_1"],
              },
            ],
          },
        },
      },
    };
    component = await mountJsonFormsComponent(ColumnSelect, { props });
    wrapper = component.wrapper;
    updateData = component.updateData;
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  it("renders", () => {
    expect(wrapper.getComponent(ColumnSelect).exists()).toBe(true);
    expect(wrapper.getComponent(DropdownInput).exists()).toBe(true);
  });

  it("initializes jsonforms on pass-through component", () => {
    initializesJsonFormsControl({
      wrapper: wrapper.getComponent(DropdownInput),
      useJsonFormsControlSpy: component.useJsonFormsControlSpy,
    });
  });

  describe("compatible types", () => {
    it("updates compatible types when mounted", () => {
      expect(updateData).toHaveBeenCalledWith(expect.anything(), path, {
        selected: "Universe_0_0",
        compatibleTypes: ["Type_0_0", "OtherType_0_0"],
      });
    });

    it("updates compatible types on value change", () => {
      const dropdownInput = wrapper.findComponent(DropdownInput);
      const dropdown = dropdownInput.findComponent(Dropdown);
      dropdown.vm.$emit("update:modelValue", "Universe_1_1");
      expect(updateData).toHaveBeenNthCalledWith(2, expect.anything(), path, {
        selected: "Universe_1_1",
        compatibleTypes: ["Type_1_1", "OtherType_1_1"],
      });
    });
  });
});
