import { describe, expect, it } from "vitest";
import { shallowMount } from "@vue/test-utils";

// @ts-ignore
import { LoadingIcon } from "@knime/components";

import EmptyDataState from "../EmptyDataState.vue";

describe("EmptyDataState.vue", () => {
  it("shows 'No data to display' when data is loaded but empty", () => {
    const wrapper = shallowMount(EmptyDataState, {
      props: { isDataLoaded: true, loadingAnimationEnabled: false },
    });
    expect(wrapper.find("h4").exists()).toBeTruthy();
    expect(wrapper.find("h4").text()).toBe("No data to display");
    expect(wrapper.findComponent(LoadingIcon).exists()).toBeFalsy();
  });

  it("shows loading icon when loadingAnimationEnabled is true", () => {
    const wrapper = shallowMount(EmptyDataState, {
      props: { isDataLoaded: false, loadingAnimationEnabled: true },
    });
    expect(wrapper.find("h4").exists()).toBeFalsy();
    expect(wrapper.findComponent(LoadingIcon).exists()).toBeTruthy();
  });

  it("shows nothing when data is not yet loaded and loading animation is disabled", () => {
    const wrapper = shallowMount(EmptyDataState, {
      props: { isDataLoaded: false, loadingAnimationEnabled: false },
    });
    expect(wrapper.find("h4").exists()).toBeFalsy();
    expect(wrapper.findComponent(LoadingIcon).exists()).toBeFalsy();
  });
});
