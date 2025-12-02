import { describe, expect, it } from "vitest";
import { shallowMount } from "@vue/test-utils";

import { KdsDataType } from "@knime/kds-components";

import SubHeaderTypeRenderer from "../components/SubHeaderTypeRenderer.vue";

const dataTypes = {
  datatype1: {
    id: "col1And2TypeId",
    name: "col1And2TypeName",
    renderers: [
      { name: "type1renderer1", id: "t1r1" },
      { name: "type1renderer2", id: "t1r2" },
      { name: "type1renderer3", id: "t1r3" },
      { name: "type1renderer4", id: "t1r4" },
    ],
    hasDataValueView: false,
  },
  datatype2: {
    id: "col3TypeId",
    name: "col3TypeName",
    renderers: [
      { name: "type2renderer1", id: "t2r1" },
      { name: "type2renderer2", id: "t2r2" },
    ],
    hasDataValueView: false,
  },
  datatype3: {
    id: "col4TypeId",
    name: "col4TypeName",
    renderers: [
      { name: "type3renderer1", id: "t3r1" },
      { name: "type3renderer2", id: "t3r2" },
      { name: "type3renderer3", id: "t3r3" },
    ],
    hasDataValueView: false,
  },
};

describe("SubHeaderTypeRenderer", () => {
  it("renders subheaders with corresponding data types", () => {
    const wrapper = shallowMount(SubHeaderTypeRenderer, {
      props: {
        dataTypes,
        subHeader: "datatype1",
      },
    });
    expect(wrapper.findComponent(KdsDataType).exists()).toBeTruthy();
    expect(wrapper.findComponent(KdsDataType).props()).toEqual({
      size: "small",
      iconName: "col1And2TypeId",
      iconTitle: "col1And2TypeName",
    });
  });
});
