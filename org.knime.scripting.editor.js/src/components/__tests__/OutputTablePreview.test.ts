import { flushPromises, mount } from "@vue/test-utils";
import { afterEach, describe, expect, it, vi } from "vitest";
import OutputTablePreview from "../OutputTablePreview.vue";
import { getScriptingService } from "../../__mocks__/scripting-service";

const mocks = vi.hoisted(() => {
  const dummyPath = "something/something/";
  return {
    dummyPath,
    config: {
      nodeId: "nodeId",
      projectId: "projectId",
      workflowId: "workflowId",
      resourceInfo: {
        baseUrl: "http://localhost/",
        path: `${dummyPath}someFile.html`,
      },
    },
    initialData: {
      initialData: "initialDataString",
    },
  };
});

vi.mock("@knime/ui-extension-service", async (importOriginal) => {
  const original =
    await importOriginal<typeof import("@knime/ui-extension-service")>();
  return {
    ...original,
    JsonDataService: {
      getInstance: vi.fn().mockResolvedValue({
        baseService: {
          getConfig: vi.fn().mockResolvedValue(mocks.config),
          callNodeDataService: vi.fn().mockResolvedValue({}),
        },
        data: vi.fn().mockResolvedValue(JSON.stringify(mocks.initialData)),
      }),
    },
    AlertingService: {
      getInstance: vi.fn().mockResolvedValue({
        sendAlert: vi.fn(),
      }),
    },
  };
});

vi.mock("@/scripting-service");

describe("OutputTablePreview", () => {
  const propsForOutputTablePreview = [
    "apiLayer",
    "extensionConfig",
    "resourceLocation",
    "shadowAppStyle",
  ];

  const doMount = async () => {
    const wrapper = mount(OutputTablePreview, {
      global: {
        stubs: {
          UIExtension: {
            template: `<div class="ui-extension-stub" >
              {{ $props }}
            </div>`,
            props: propsForOutputTablePreview,
          },
        },
      },
    });
    await flushPromises();
    return { wrapper };
  };

  afterEach(() => {
    vi.clearAllMocks();
  });

  it("create output table preview", async () => {
    const { wrapper } = await doMount();
    const preEvaluationSign = wrapper.find(".pre-evaluation-sign");

    expect(preEvaluationSign.exists()).toBe(true);
    expect(preEvaluationSign.text()).toContain("To see the preview");
  });

  it("updates state when updateOutputTable event is triggered", async () => {
    const { wrapper } = await doMount();

    const triggerUpdateOutputTableUpdate = () =>
      vi.mocked(getScriptingService().registerEventHandler).mock.calls[0][1];

    const mockRows = 10;
    triggerUpdateOutputTableUpdate()(JSON.stringify(mockRows));
    await flushPromises();

    const previewWarningText = wrapper.find(".preview-warning-text");
    expect(previewWarningText.exists()).toBe(true);
    expect(previewWarningText.text()).toContain(
      `Preview computed on first ${mockRows} rows`,
    );
  });

  it("calls UIExtension with correct arguments", async () => {
    const { wrapper } = await doMount();

    const triggerUpdateOutputTableUpdate = () =>
      vi.mocked(getScriptingService().registerEventHandler).mock.calls[0][1];

    const mockRows = 10;

    triggerUpdateOutputTableUpdate()(JSON.stringify(mockRows));

    await flushPromises();

    const uiExtensionStub = wrapper.find(".ui-extension-stub");
    expect(uiExtensionStub.exists()).toBe(true);
    const renderedUiExtensionStubContent = uiExtensionStub.text();

    propsForOutputTablePreview.forEach((prop) => {
      expect(renderedUiExtensionStubContent).toContain(prop);
    });

    expect(renderedUiExtensionStubContent).toContain(mocks.dummyPath);
    expect(renderedUiExtensionStubContent).toContain(mocks.config.projectId);
    expect(renderedUiExtensionStubContent).toContain(
      mocks.config.resourceInfo.baseUrl,
    );
  });
});
