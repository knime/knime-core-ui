import { describe, expect, it } from "vitest";

import shallowMountReportingComponent from "@@/test-setup/utils/shallowMountReportingComponent";
import TileView from "../TileView.vue";
import TileViewInteractive from "../TileViewInteractive.vue";
import TileViewReport from "../TileViewReport.vue";

describe("TileView.vue", () => {
  it("renders the interactive tile view", () => {
    const { wrapper } = shallowMountReportingComponent(TileView);
    expect(wrapper.findComponent(TileViewInteractive).exists()).toBeTruthy();
    expect(wrapper.findComponent(TileViewReport).exists()).toBeFalsy();
  });

  it("renders the report tile view", () => {
    const { wrapper } = shallowMountReportingComponent(TileView, true);
    expect(wrapper.findComponent(TileViewReport).exists()).toBeTruthy();
    expect(wrapper.findComponent(TileViewInteractive).exists()).toBeFalsy();
  });

  it("sets reporting content when rendered", async () => {
    const { wrapper, setRenderCompleted } = shallowMountReportingComponent(
      TileView,
      true,
    );
    await wrapper.findComponent(TileViewReport).vm.$emit("rendered");
    expect(setRenderCompleted).toHaveBeenCalled();
  });
});
