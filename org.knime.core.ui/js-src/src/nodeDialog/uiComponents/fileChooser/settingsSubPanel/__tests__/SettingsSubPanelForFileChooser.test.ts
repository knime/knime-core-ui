import {
  type Mock,
  afterEach,
  beforeEach,
  describe,
  expect,
  it,
  vi,
} from "vitest";
import { nextTick } from "vue";
import {
  VueWrapper,
  enableAutoUnmount,
  flushPromises,
  mount,
} from "@vue/test-utils";

import { Button } from "@knime/components";
import { getGlobal } from "@knime/jsonforms/testing";

import TestSettingsSubPanel, {
  type Props as TestComponentProps,
  contentId,
  expandButtonId,
} from "./TestSettingsSubPanel.vue";

enableAutoUnmount(afterEach);

describe("SettingsSubPanelForFileChoooser", () => {
  let props: TestComponentProps,
    onApply: Mock<() => Promise<void>>,
    setSubPanelExpanded: () => void;

  beforeEach(() => {
    onApply = vi.fn();
    setSubPanelExpanded = vi.fn();
    props = {
      onApply,
      settingsSubPanelConfig: {},
    };
  });

  const mountTestComponent = async () => {
    const wrapper = mount(TestSettingsSubPanel, {
      props,
      global: getGlobal({
        provide: {
          // @ts-expect-error
          setSubPanelExpanded,
        },
      }),
      attachTo: document.body,
    });
    await flushPromises();
    await nextTick();

    return wrapper;
  };

  const clickExpandButton = (wrapper: VueWrapper<any>) =>
    wrapper.find(`#${expandButtonId}`).trigger("click");
  const findContent = () => document.body.querySelector(`#${contentId}`);
  const findButtonByText = (text: string) => (wrapper: VueWrapper<any>) =>
    wrapper
      .findAllComponents(Button)
      .filter((wrapper) => wrapper.text() === text)[0];

  it("displays the expand button with the correct id", async () => {
    const wrapper = await mountTestComponent();

    const expandButton = wrapper.find(`#${expandButtonId}`);
    expect(expandButton.exists()).toBeTruthy();
    expect(expandButton.isVisible()).toBeTruthy();
  });

  it("expands subpanel on expand button click", async () => {
    const wrapper = await mountTestComponent();

    expect(findContent()).toBeNull();
    expect(setSubPanelExpanded).not.toHaveBeenCalled();
    await clickExpandButton(wrapper);

    expect(findContent()).not.toBeNull();
    expect(setSubPanelExpanded).toHaveBeenCalledWith({ isExpanded: true });
  });

  it("closes subpanel on cancel", async () => {
    const wrapper = await mountTestComponent();
    await clickExpandButton(wrapper);

    const cancelButton = findButtonByText("Cancel")(wrapper);
    await cancelButton.trigger("click");

    await flushPromises();
    await nextTick();

    expect(findContent()).toBeNull();
    expect(setSubPanelExpanded).toHaveBeenCalledWith({ isExpanded: false });
  });

  it("applies and closes subpanel on apply", async () => {
    const wrapper = await mountTestComponent();
    await clickExpandButton(wrapper);
    expect(findContent()).not.toBeNull();
    onApply.mockResolvedValue();
    const applyButton = findButtonByText("Apply")(wrapper);
    await applyButton.trigger("click");
    await flushPromises();
    await nextTick();

    await new Promise((resolve) => setTimeout(resolve, 100));

    expect(findContent()).toBeNull();
  });

  it("does not close the subpanel on an erroneous apply", async () => {
    const wrapper = await mountTestComponent();
    await clickExpandButton(wrapper);
    expect(findContent()).not.toBeNull();
    onApply.mockRejectedValue("Rejected");
    const applyButton = findButtonByText("Apply")(wrapper);
    await applyButton.trigger("click");
    await flushPromises();
    expect(findContent()).not.toBeNull();
  });

  it("disables the apply button", async () => {
    const wrapper = await mountTestComponent();
    await clickExpandButton(wrapper);
    const applyButton = findButtonByText("Apply")(wrapper);
    expect(applyButton.props().disabled).toBeFalsy();
    wrapper.vm.getApplyButton()!.disabled.value = true;
    await flushPromises();
    expect(applyButton.props().disabled).toBeTruthy();
  });

  it("sets the apply button text", async () => {
    const applyText = "My Apply";
    onApply.mockResolvedValue();
    const wrapper = await mountTestComponent();
    await clickExpandButton(wrapper);
    wrapper.vm.getApplyButton()!.text.value = applyText;
    await flushPromises();
    const applyButton = findButtonByText(applyText)(wrapper);
    await applyButton.trigger("click");
    await flushPromises();
    await nextTick();
    expect(findContent()).toBeNull();
  });
});
