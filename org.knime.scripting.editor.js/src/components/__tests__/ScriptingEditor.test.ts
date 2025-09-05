import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { nextTick, ref } from "vue";
import { flushPromises, mount } from "@vue/test-utils";
import { useElementBounding } from "@vueuse/core";

import { SplitPanel } from "@knime/components";

import { defaultPortConfig } from "@/__mocks__/scripting-service";
import { DEFAULT_INITIAL_DATA } from "@/initial-data-service-browser-mock";
import { getScriptingService } from "@/scripting-service";
import {
  DEFAULT_INITIAL_SETTINGS,
  registerSettingsMock,
} from "@/settings-service-browser-mock";
import CodeEditorControlBar from "../CodeEditorControlBar.vue";
import InputOutputPane from "../InputOutputPane.vue";
import MainEditorPane from "../MainEditorPane.vue";
import ScriptingEditor from "../ScriptingEditor.vue";
import ScriptingEditorBottomPane, {
  type BottomPaneTabSlotName,
} from "../ScriptingEditorBottomPane.vue";
import SettingsPage from "../SettingsPage.vue";

const mocks = vi.hoisted(() => {
  return {
    useElementBounding: vi.fn(),
  };
});

vi.mock("@vueuse/core", async (importOriginal) => {
  const original = await importOriginal<typeof import("@vueuse/core")>();
  return {
    ...original,
    useElementBounding: mocks.useElementBounding,
    onKeyStroke: vi.fn(),
  };
});

vi.mock("@/scripting-service");
vi.mock("@/editor");

vi.mock("@/settings-service", () => ({
  getSettingsService: vi.fn(() => ({
    getSettings: vi.fn(() => Promise.resolve(DEFAULT_INITIAL_SETTINGS)),
    registerSettingsGetterForApply: vi.fn(() => Promise.resolve()),
    registerSettings: vi.fn(registerSettingsMock),
  })),
}));

vi.mock("@/initial-data-service", () => ({
  getInitialDataService: vi.fn(() => ({
    getInitialData: vi.fn(() => Promise.resolve(DEFAULT_INITIAL_DATA)),
  })),
}));

vi.mock("@/display-mode", () => ({ displayMode: ref("large") }));

describe("ScriptingEditor", () => {
  beforeEach(() => {
    vi.mocked(useElementBounding).mockReturnValue({
      width: ref(1000),
    } as ReturnType<typeof useElementBounding>);
    vi.mocked(getScriptingService().sendToService).mockResolvedValue({});
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  const doMount = (
    args: {
      props: Partial<InstanceType<typeof ScriptingEditor>["$props"]>;
      slots?: any;
    } = {
      props: {
        title: "myTitle",
        language: "someLanguage",
        rightPaneLayout: "fixed",
        fileName: "myFile.ts",
      },
    },
  ) => {
    const wrapper = mount(ScriptingEditor, {
      props: { title: "", language: "", fileName: "", ...args.props },
      slots: args.slots,
    });

    const allSplitpanes = wrapper.findAllComponents(SplitPanel);
    const mainSplitpane = allSplitpanes.at(0);
    const horizontalSplitpane = allSplitpanes.at(1);
    const verticalSplitpane = allSplitpanes.at(2);
    if (
      typeof mainSplitpane === "undefined" ||
      typeof horizontalSplitpane === "undefined" ||
      typeof verticalSplitpane === "undefined"
    ) {
      throw new Error("could not find an expected splitpane");
    }
    return {
      wrapper,
      mainSplitpane,
      horizontalSplitpane,
      verticalSplitpane,
    };
  };

  describe("renders", () => {
    it("displays splitpanes", () => {
      const { wrapper } = doMount();
      expect(wrapper.findComponent(SplitPanel).exists()).toBeTruthy();
    });

    it("mounts the main editor pane by default", () => {
      const { wrapper } = doMount();
      expect(wrapper.findComponent(MainEditorPane).exists()).toBeTruthy();
    });

    it("uses the provided title", () => {
      const { wrapper } = doMount();
      const heading = wrapper.find("div.title");
      expect(heading.element.textContent).toBe("myTitle");
    });

    it("display input/output pane", () => {
      const { wrapper } = doMount();
      expect(wrapper.findComponent(InputOutputPane).exists()).toBeTruthy();
    });
  });

  describe("code editor bottom control bar", () => {
    it("is visible by default", () => {
      const { wrapper } = doMount();
      expect(wrapper.findComponent(CodeEditorControlBar).exists()).toBeTruthy();
    });

    it("are visible if enabled", () => {
      const { wrapper } = doMount({
        props: { showControlBar: true },
      });
      expect(wrapper.findComponent(CodeEditorControlBar).exists()).toBeTruthy();
    });

    it("are not visible if disabled", () => {
      const { wrapper } = doMount({
        props: { showControlBar: false },
      });
      expect(wrapper.findComponent(CodeEditorControlBar).exists()).toBeFalsy();
    });

    it("displays slotted content if enabled", () => {
      const { wrapper } = doMount({
        props: {},
        slots: {
          "code-editor-controls":
            "<button class='test-class'>TestButton</button>",
        },
      });
      expect(
        wrapper
          .findComponent(CodeEditorControlBar)
          .find(".test-class")
          .exists(),
      ).toBeTruthy();
    });
  });

  it("passes slotted status label to bottom pane", () => {
    const { wrapper } = doMount({
      props: {},
      slots: {
        "bottom-pane-status-label": "<div class='test-class'>Test</div>",
      },
    });
    expect(
      wrapper
        .findComponent(ScriptingEditorBottomPane)
        .find(".test-class")
        .exists(),
    ).toBeTruthy();
  });

  it("shows settings page", async () => {
    const { wrapper } = doMount();
    (wrapper.vm as any).showSettingsPage = true;
    await wrapper.vm.$nextTick();
    expect(wrapper.findComponent(SettingsPage).exists()).toBeTruthy();
  });

  it("closes settings page", async () => {
    const { wrapper } = doMount();
    (wrapper.vm as any).showSettingsPage = true;
    await wrapper.vm.$nextTick();
    const settingsPage = wrapper.findComponent(SettingsPage);
    expect(settingsPage.exists()).toBeTruthy();
    settingsPage.vm.$emit("close-settings-page");
    await wrapper.vm.$nextTick();
    expect(settingsPage.exists()).toBeFalsy();
  });

  it("passes slotted content through to settings page", async () => {
    const { wrapper } = doMount({
      props: {
        title: "myTitle",
        language: "someLanguage",
        rightPaneLayout: "fixed",
        fileName: "myFile.ts",
      },
      slots: {
        "settings-title": "<div class='settings-title'>Settings title</div>",
        "settings-content":
          "<div class='settings-content'>Settings content</div>",
      },
    });
    (wrapper.vm as any).showSettingsPage = true;
    await wrapper.vm.$nextTick();
    const settingsPage = wrapper.findComponent(SettingsPage);
    expect(settingsPage.find(".settings-title").exists()).toBeTruthy();
    expect(settingsPage.find(".settings-content").exists()).toBeTruthy();
  });

  it("sets drop event on code editor", async () => {
    const { wrapper } = doMount();
    const inOutPane = wrapper.findComponent(InputOutputPane);
    expect(inOutPane.exists()).toBeTruthy();

    const dropEventHandlerMock = vi.fn();
    const dropEventMock = { drop: "event" };
    inOutPane.vm.$emit("drop-event-handler-created", dropEventHandlerMock);
    expect((wrapper.vm as any).dropEventHandler).toStrictEqual(
      dropEventHandlerMock,
    );
    await nextTick();

    const editorComponent = wrapper.findComponent(MainEditorPane);
    const editorTextField = editorComponent.find(".code-editor");

    expect(editorTextField.exists()).toBeTruthy();
    expect(editorComponent.props().dropEventHandler).toBe(dropEventHandlerMock);

    await editorTextField.trigger("drop", dropEventMock);

    expect(dropEventHandlerMock).toHaveBeenCalledWith(
      expect.objectContaining(dropEventMock),
    );
  });

  it("displays anything we pass through via the 'editor' slot", () => {
    const { wrapper } = doMount({
      props: {},
      slots: {
        editor: "<p id='testeditorslotinjection'>Test</p>",
      },
    });

    expect(wrapper.find("#testeditorslotinjection").exists()).toBeTruthy();
  });

  it("shows input table tabs for port views", async () => {
    const { wrapper } = doMount();

    await flushPromises();
    const makeNodePortId = (
      nodeId: string,
      portIdx: number,
    ): BottomPaneTabSlotName => `bottomPaneTabSlot:${nodeId}-${portIdx}`;
    const tabElements = wrapper.findAll(".tab-bar input");

    for (const inputPort of defaultPortConfig.inputPorts) {
      const expectedElement = tabElements.find(
        (tab) =>
          tab.attributes("value") ===
          makeNodePortId(inputPort.nodeId!, inputPort.portIdx),
      );

      expect(expectedElement).toBeDefined();
      expect(expectedElement!.isVisible()).toBeTruthy();
    }
  });
});
