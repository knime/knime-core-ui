import { afterEach, describe, expect, it, vi } from "vitest";
import { flushPromises, mount } from "@vue/test-utils";

import InfinityLoadingBar from "@/components/InfinityLoadingBar.vue";
import AiDisclaimer from "@/components/ai-assistant/AiDisclaimer.vue";
import { getScriptingService } from "@/scripting-service";

vi.mock("@/scripting-service");

const doMount = async () => {
  const wrapper = mount(AiDisclaimer);
  await flushPromises();
  return wrapper;
};

describe("AiDisclaimer", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("should render the provided disclaimer text", async () => {
    vi.mocked(getScriptingService().getAiDisclaimer).mockReturnValue(
      Promise.resolve("THIS IS A DISCLAIMER"),
    );
    const wrapper = await doMount();

    expect(wrapper.text()).toContain("THIS IS A DISCLAIMER");
  });

  it("should show a loading indicator while the disclaimer is being fetched", async () => {
    vi.mocked(getScriptingService().getAiDisclaimer).mockReturnValue(
      new Promise(() => {}),
    );
    const wrapper = await doMount();

    // Shows loading bar
    expect(wrapper.findComponent(InfinityLoadingBar).exists()).toBe(true);

    // Does not show any disclaimer text yet
    expect(wrapper.text()).not.toContain("Disclaimer");

    // Does not show the accept button yet
    const button = wrapper.find("[data-testid='ai-disclaimer-accept-button']");
    expect(button.exists()).toBe(false);
  });

  it("should emit the close event when the accept button is clicked", async () => {
    const wrapper = await doMount();

    const button = wrapper.find("[data-testid='ai-disclaimer-accept-button']");
    await button.trigger("click");

    expect(wrapper.emitted("accept-disclaimer")).toHaveLength(1);
  });
});
