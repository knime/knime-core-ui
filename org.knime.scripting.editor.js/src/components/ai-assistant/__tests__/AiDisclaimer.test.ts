import { afterEach, describe, expect, it, vi } from "vitest";
import AiDisclaimer from "@/components/ai-assistant/AiDisclaimer.vue";
import { flushPromises, mount } from "@vue/test-utils";

const doMount = async () => {
  const wrapper = mount(AiDisclaimer);
  await flushPromises();
  return wrapper;
};

describe("AiDisclaimer", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("should emit the close event when the accept button is clicked", async () => {
    const wrapper = await doMount();

    const button = wrapper.find("[data-testid='ai-disclaimer-accept-button']");
    await button.trigger("click");

    expect(wrapper.emitted("accept-disclaimer")).toHaveLength(1);
  });
});
