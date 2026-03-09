import {
  type Mock,
  afterEach,
  beforeEach,
  describe,
  expect,
  it,
  vi,
} from "vitest";
import { defineComponent, h, nextTick } from "vue";
import type { VueWrapper } from "@vue/test-utils";
import flushPromises from "flush-promises";

import type { VueControlProps } from "@knime/jsonforms";
import {
  getControlBase,
  mountJsonFormsControl,
} from "@knime/jsonforms/testing";

import CustomWidget from "../CustomWidget.vue";

// Use vi.hoisted to create a shared mutable mock object
const { mockWidgetModule, mockJsonData } = vi.hoisted(() => ({
  mockWidgetModule: {
    default: null as any,
    injectStyles: null as any,
    getInjectedStyles: null as any,
    resetInjectedStyles: null as any,
  },
  mockJsonData: vi.fn(),
}));

// Helper to wait for dynamic imports to settle
const dynamicImportsSettled = async () => {
  await flushPromises();
  await nextTick();
  await vi.dynamicImportSettled();
  await flushPromises();
};

// Mock useDynamicImport to return our test widget
vi.mock("../../useDynamicImport", async () => {
  const implementation = await import("./mockCustomWidget");
  // Populate the hoisted object with implementation details
  Object.assign(mockWidgetModule, implementation);

  return {
    useDynamicImport: () => ({
      dynamicImport: () => mockWidgetModule,
      clearCache: () => {},
    }),
  };
});

// Mock ResourceService
vi.mock("@knime/ui-extension-service", () => ({
  ResourceService: vi.fn().mockImplementation(() => ({
    getResourceUrl: vi.fn().mockResolvedValue("/customWidget/testWidget.js"),
  })),
  JsonDataService: vi.fn().mockImplementation(() => ({
    data: mockJsonData,
  })),
}));

describe("CustomWidget", () => {
  let wrapper: VueWrapper<any>,
    props: VueControlProps<unknown>,
    mockGetKnimeService: Mock,
    mockShadowRoot: ShadowRoot;

  const mountCustomWidget = () =>
    mountJsonFormsControl(CustomWidget, {
      props,
      provide: {
        // @ts-expect-error getKnimeService is injected when the app is mounted as shadow app
        getKnimeService: mockGetKnimeService,
        shadowRoot: mockShadowRoot,
      },
    });

  beforeEach(() => {
    // Create a mock shadow root
    mockShadowRoot = {
      appendChild: vi.fn(),
    } as unknown as ShadowRoot;

    // Mock the getKnimeService injection
    mockGetKnimeService = vi.fn().mockReturnValue({});

    // Clear mock calls
    vi.clearAllMocks();

    const uischema = {
      type: "Control" as const,
      scope: "#/properties/customWidget",
      options: {
        widgetResource: "testWidget.js",
      },
    };

    const schema = {
      properties: {
        customWidget: {
          type: "object",
          title: "Custom Widget Test",
        },
      },
    };

    const controlBase = getControlBase("test");

    props = {
      control: {
        ...controlBase,
        data: {},
        schema,
        uischema,
      },
      changeValue: vi.fn(),
      disabled: false,
      handleChange: vi.fn(),
      isValid: true,
      messages: { errors: [] },
      onRegisterValidation: vi.fn(),
    };
  });

  afterEach(() => {
    if (wrapper) {
      wrapper.unmount();
    }
    vi.clearAllMocks();
  });

  it("should render the custom widget component", async () => {
    const component = await mountCustomWidget();
    wrapper = component.wrapper;

    // Wait for dynamic import to complete and component to render
    await dynamicImportsSettled();

    // Check that the custom widget content is rendered
    expect(wrapper.find(".custom-widget-test").exists()).toBe(true);
    expect(wrapper.find(".widget-label").text()).toBe("Custom Widget");
    expect(wrapper.find(".widget-input").exists()).toBe(true);
  });

  it("should inject styles into shadow root", async () => {
    const component = await mountCustomWidget();
    wrapper = component.wrapper;

    // Wait for dynamic import and style injection
    await dynamicImportsSettled();

    // Verify that styles were appended to shadow root
    expect(mockShadowRoot.appendChild).toHaveBeenCalled();

    // Verify the injected style element
    const injectedStyle = mockWidgetModule.getInjectedStyles();
    expect(injectedStyle).not.toBeNull();
    expect(injectedStyle?.textContent).toContain("custom-widget-test");
    expect(injectedStyle?.textContent).toContain("rgb(255, 0, 0)");
  });

  it("should not inject styles if shadowRoot is not provided", async () => {
    // Reset the injected styles tracker
    mockWidgetModule.resetInjectedStyles();

    // Mount without shadowRoot
    const component = await mountJsonFormsControl(CustomWidget, {
      props,
      provide: {
        // @ts-expect-error getKnimeService is injected when the app is mounted as shadow app
        getKnimeService: mockGetKnimeService,
        // No shadowRoot provided
      },
    });
    wrapper = component.wrapper;

    // Wait for dynamic import
    await dynamicImportsSettled();

    // Verify that styles were not injected
    const injectedStyle = mockWidgetModule.getInjectedStyles();
    expect(injectedStyle).toBeNull();
  });

  it("should pass props to the custom widget component", async () => {
    // Update the mock widget to verify props
    const originalDefault = mockWidgetModule.default;
    const componentWithProps = defineComponent({
      props: ["control", "schema", "uischema"],
      setup(componentProps) {
        return () =>
          h("div", { class: "custom-widget-with-props" }, [
            h(
              "span",
              { class: "prop-scope" },
              componentProps.control.uischema?.scope || "",
            ),
          ]);
      },
    });

    mockWidgetModule.default = componentWithProps;

    const component = await mountCustomWidget();
    wrapper = component.wrapper;

    // Wait for dynamic import
    await dynamicImportsSettled();

    // Verify props are passed through
    expect(wrapper.find(".custom-widget-with-props").exists()).toBe(true);
    expect(wrapper.find(".prop-scope").text()).toBe(
      "#/properties/customWidget",
    );

    // Restore
    mockWidgetModule.default = originalDefault;
  });

  it("should provide rpcService and proxy calls", async () => {
    // Enable rpcServiceName in props
    const originalDefault = mockWidgetModule.default;
    props.control.uischema.options = {
      ...props.control.uischema.options,
      rpcServiceName: "testService",
    };

    // Create a component that consumes the provided service
    const componentWithRpc = defineComponent({
      props: ["control", "schema", "uischema"],
      setup() {
        // @ts-expect-error window does not have Vue type here
        const { inject, onMounted } = window.Vue;
        const rpcService: any = inject("rpcService");

        onMounted(() => {
          if (rpcService) {
            rpcService.myMethod("arg1", 123);
          }
        });

        return () => h("div", { class: "rpc-widget" });
      },
    });

    mockWidgetModule.default = componentWithRpc;

    const component = await mountCustomWidget();
    wrapper = component.wrapper;

    await dynamicImportsSettled();

    // Verify stub exists
    expect(wrapper.find(".rpc-widget").exists()).toBe(true);

    // Verify RPC call
    expect(mockJsonData).toHaveBeenCalledWith({
      method: "testService.myMethod",
      options: ["arg1", 123],
    });

    // Restore
    mockWidgetModule.default = originalDefault;
  });
});
