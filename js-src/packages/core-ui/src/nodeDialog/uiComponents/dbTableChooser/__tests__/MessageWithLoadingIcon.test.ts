import { describe, expect, it } from "vitest";
import { mount } from "@vue/test-utils";

import { LoadingIcon } from "@knime/components";

import MessageWithLoadingIcon from "../MessageWithLoadingIcon.vue";

const doMount = (
  errorMessage: string | null = "test",
  isLoading: boolean = false,
) => {
  const props = {
    errorMessage,
    isLoading,
  };

  return mount(MessageWithLoadingIcon, {
    props,
  });
};

describe("ErrorMessageWithLoadingIcon", () => {
  it("renders", () => {
    const wrapper = doMount();
    expect(wrapper.findComponent(MessageWithLoadingIcon).exists()).toBeTruthy();
  });

  it("shows error message when isloading is false", () => {
    const wrapper = doMount("test error message", false);
    expect(wrapper.text()).toContain("test error message");
  });

  it("does not show error message when isloading is true", () => {
    const wrapper = doMount("test error message", true);
    expect(wrapper.text()).not.toContain("test error message");
  });

  it("shows loading icon when isLoading is true", () => {
    const wrapper = doMount("something", true);
    expect(wrapper.findComponent(LoadingIcon).exists()).toBeTruthy();
  });

  it("does not show loading icon when isLoading is false", () => {
    const wrapper = doMount("something", false);
    expect(wrapper.findComponent(LoadingIcon).exists()).toBeFalsy();
  });
});
