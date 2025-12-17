import {
  type Mock,
  afterEach,
  beforeEach,
  describe,
  expect,
  it,
  vi,
} from "vitest";
import { nextTick, reactive } from "vue";
import { VueWrapper } from "@vue/test-utils";
import flushPromises from "flush-promises";

import { FunctionButton, SideDrawer, ValueSwitch } from "@knime/components";
import {
  type ProvidedMethods,
  type VueControlTestProps,
  getControlBase,
  mountJsonFormsControlLabelContent,
} from "@knime/jsonforms/testing";
import FolderLenseIcon from "@knime/styles/img/icons/folder-lense.svg";
import WorkflowIcon from "@knime/styles/img/icons/workflow.svg";

import type { FlowSettings } from "@/nodeDialog/api/types";
import { injectionKey as flowVariablesMapInjectionKey } from "@/nodeDialog/composables/components/useProvidedFlowVariablesMap";
import type { FileChooserOptions } from "@/nodeDialog/types/FileChooserUiSchema";
import DialogFileExplorer from "../../DialogFileExplorer.vue";
import SettingsSubPanelForFileChooser from "../../settingsSubPanel/SettingsSubPanelForFileChooser.vue";
import FileChooserControl from "../FileChooserControl.vue";
import SideDrawerContent from "../SideDrawerContent.vue";

describe("FileChooserControl.vue - Relative Path Features", () => {
  let props: VueControlTestProps<typeof FileChooserControl> & {
      control: {
        uischema: {
          options: FileChooserOptions;
        };
      };
    },
    dataServiceSpy: Mock;

  const labelForId = "myLabelForId";
  const testSpaceName = "testSpace";
  const testWorkflowPath = "workflows/myWorkflow";

  const mountFileChooserControl = ({
    props,
    provide,
    stubs,
    dataService,
  }: {
    props: VueControlTestProps<typeof FileChooserControl>;
    provide?: Partial<ProvidedMethods>;
    stubs?: Record<string, boolean>;
    dataService?: Mock;
  }) => {
    const flowVariablesMap: Record<string, FlowSettings> = reactive({});
    const getData = dataService || dataServiceSpy;
    const component = mountJsonFormsControlLabelContent(FileChooserControl, {
      props,
      provide: {
        // @ts-expect-error
        setSubPanelExpanded: vi.fn(),
        getPersistSchema: () => ({}),
        [flowVariablesMapInjectionKey as symbol]: flowVariablesMap,
        getData,
        addStateProviderListener: vi.fn(),
        ...provide,
      },
      stubs: {
        transition: false,
        "transition-group": false,
        ...stubs,
      },
    });
    return {
      flowVariablesMap,
      ...component,
    };
  };

  const mockDataService = () => {
    return vi.fn((params: { method?: string | undefined } | undefined) => {
      if (params?.method === "fileChooser.listItems") {
        return Promise.resolve({
          folder: {
            items: [
              { name: "file1.txt", isDirectory: false },
              { name: "folder1", isDirectory: true },
            ],
            path: "/some/path",
            parentFolders: [
              { path: null, name: null },
              { path: "/some", name: "some" },
              { path: "/some/path", name: "path" },
            ],
          },
          filePathRelativeToFolder: "",
          errorMessage: null,
        });
      }
      if (params?.method === "fileChooser.getFilePath") {
        return Promise.resolve({
          path: "/some/path/file.txt",
          errorMessage: null,
        });
      }
      if (params?.method === "fileChooser.resolveRelativePath") {
        return Promise.resolve("/resolved/path");
      }
      return Promise.resolve({});
    });
  };

  beforeEach(() => {
    dataServiceSpy = mockDataService();

    props = {
      control: {
        ...getControlBase("test"),
        data: {
          path: {
            path: "myPath",
            timeout: 1000,
            fsCategory: "relative-to-current-hubspace",
          },
        },
        schema: {
          type: "object",
          title: "File path",
          properties: {
            path: {
              type: "object",
            },
          },
          default: "default value",
        },
        uischema: {
          type: "Control",
          scope: "#/properties/view/properties/filePath",
          options: {
            format: "fileChooser",
            spaceFSOptions: {
              mountId: testSpaceName,
              spacePath: "/space/path",
              relativeWorkflowPath: testWorkflowPath,
            },
          },
        },
      },
      labelForId,
      disabled: false,
      isValid: true,
    };
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  const hasFolderLenseIcon = (
    wrapper: ReturnType<VueWrapper["findComponent"]>,
  ) => wrapper.findComponent(FolderLenseIcon).exists();

  const findFolderLenseButton = (wrapper: VueWrapper) =>
    wrapper
      .findAllComponents(FunctionButton)
      .filter((fb) => hasFolderLenseIcon(fb))[0];

  const expandSideDrawer = async (wrapper: VueWrapper) => {
    await findFolderLenseButton(wrapper).vm.$emit("click");
    return wrapper.findComponent(SideDrawer);
  };

  describe("relative to Switch Visibility", () => {
    it("shows the relative to switch only in Space tab when workflow path is available", async () => {
      const { wrapper } = mountFileChooserControl({
        props,
      });

      await expandSideDrawer(wrapper);
      await flushPromises();

      const sideDrawer = wrapper.findComponent(SideDrawerContent);
      const fileExplorer = sideDrawer.findComponent(DialogFileExplorer);

      expect(fileExplorer.exists()).toBeTruthy();
      expect(fileExplorer.props().relativeToOptions).toBeDefined();
      expect(fileExplorer.props().relativeToOptions?.rootLabel).toBe("Space");
      expect(fileExplorer.props().relativeToOptions?.relativeRootLabel).toBe(
        "Workflow",
      );

      const valueSwitch = fileExplorer.findComponent(ValueSwitch);
      expect(valueSwitch.exists()).toBeTruthy();
      expect(valueSwitch.props().possibleValues).toHaveLength(2);
      expect(valueSwitch.props().possibleValues?.[0].text).toBe("Space");
      expect(valueSwitch.props().possibleValues?.[1].text).toBe("Workflow");
    });

    it("does not show the relative to switch in Local tab", async () => {
      props.control.data.path.fsCategory = "LOCAL";
      props.control.uischema.options!.isLocal = true;

      const { wrapper } = mountFileChooserControl({
        props,
      });

      await expandSideDrawer(wrapper);
      await flushPromises();

      const sideDrawer = wrapper.findComponent(SideDrawerContent);
      const fileExplorer = sideDrawer.findComponent(DialogFileExplorer);

      expect(fileExplorer.exists()).toBeTruthy();
      expect(fileExplorer.props().relativeToOptions).toBeNull();

      const valueSwitch = fileExplorer.findComponent(ValueSwitch);
      expect(valueSwitch.exists()).toBeFalsy();
    });

    it("does not show the relative to switch in Embedded tab", async () => {
      props.control.data.path.fsCategory = "relative-to-embedded-data";

      const { wrapper } = mountFileChooserControl({
        props,
      });

      await expandSideDrawer(wrapper);
      await flushPromises();

      const sideDrawer = wrapper.findComponent(SideDrawerContent);
      const fileExplorer = sideDrawer.findComponent(DialogFileExplorer);

      expect(fileExplorer.exists()).toBeTruthy();
      expect(fileExplorer.props().relativeToOptions).toBeNull();

      const valueSwitch = fileExplorer.findComponent(ValueSwitch);
      expect(valueSwitch.exists()).toBeFalsy();
    });

    it("does not show the relative to switch when workflow path is not available", async () => {
      delete props.control.uischema.options!.spaceFSOptions!
        .relativeWorkflowPath;

      const { wrapper } = mountFileChooserControl({
        props,
      });

      await expandSideDrawer(wrapper);
      await flushPromises();

      const sideDrawer = wrapper.findComponent(SideDrawerContent);
      const fileExplorer = sideDrawer.findComponent(DialogFileExplorer);

      expect(fileExplorer.exists()).toBeTruthy();
      expect(fileExplorer.props().relativeToOptions).toBeNull();

      const valueSwitch = fileExplorer.findComponent(ValueSwitch);
      expect(valueSwitch.exists()).toBeFalsy();
    });
  });

  describe("relative Breadcrumbs Display", () => {
    it("switches to relative-to-workflow mode and updates breadcrumbs", async () => {
      props.control.data.path.path = "data/file.txt";

      const customDataService = vi.fn(
        (
          params:
            | { method?: string | undefined; options?: unknown[] }
            | undefined,
        ) => {
          if (params?.method === "fileChooser.listItems") {
            const relativeTo = params?.options?.[4];
            const parentFolders = relativeTo
              ? [
                  { path: testWorkflowPath, name: null },
                  { path: null, name: "data" },
                ]
              : [
                  { path: null, name: null },
                  { path: "/space/path/data", name: "data" },
                ];

            return Promise.resolve({
              folder: {
                items: [{ name: "file.txt", isDirectory: false }],
                path: "/space/path/data",
                parentFolders,
              },
              filePathRelativeToFolder: "",
              errorMessage: null,
            });
          }
          return Promise.resolve({});
        },
      );

      const { wrapper, handleChange } = mountFileChooserControl({
        props,
        dataService: customDataService,
      });

      await expandSideDrawer(wrapper);
      await flushPromises();

      const sideDrawer = wrapper.findComponent(SideDrawerContent);
      let fileExplorer = sideDrawer.findComponent(DialogFileExplorer);

      expect(fileExplorer.props().relativeToOptions).toBeDefined();
      expect(fileExplorer.props().relativeToOptions?.isRelativeTo).toBe(false);

      const initialBreadcrumbRoot = fileExplorer.props().breadcrumbRoot;
      expect(initialBreadcrumbRoot).toBe("/space/path");

      fileExplorer.vm.$emit("update:isRelativeTo", true);
      await nextTick();
      await flushPromises();

      fileExplorer = sideDrawer.findComponent(DialogFileExplorer);
      expect(fileExplorer.props().relativeToOptions?.isRelativeTo).toBe(true);

      const updatedBreadcrumbRoot = fileExplorer.props().breadcrumbRoot;
      expect(updatedBreadcrumbRoot).toBeNull();

      expect(fileExplorer.props().relativeToOptions?.relativeToPath).toBe(
        testWorkflowPath,
      );
      expect(fileExplorer.props().relativeToOptions?.relativeRootIcon).toBe(
        WorkflowIcon,
      );

      await wrapper
        .findComponent(SettingsSubPanelForFileChooser)
        .vm.$emit("apply");
      await flushPromises();

      expect(handleChange).toHaveBeenCalledWith(
        "test.path",
        expect.objectContaining({
          fsCategory: "relative-to-workflow",
        }),
      );
    });

    it("preserves relative-to-workflow selection when switching tabs", async () => {
      props.control.data.path.fsCategory = "relative-to-workflow";
      props.control.uischema.options!.isLocal = false;

      const { wrapper } = mountFileChooserControl({
        props,
      });

      await expandSideDrawer(wrapper);
      await flushPromises();

      const sideDrawer = wrapper.findComponent(SideDrawerContent);

      sideDrawer.vm.$emit("update:modelValue", {
        ...props.control.data.path,
        fsCategory: "relative-to-embedded-data",
      });
      await nextTick();

      sideDrawer.vm.$emit("update:modelValue", {
        ...props.control.data.path,
        fsCategory: "relative-to-workflow",
      });
      await nextTick();

      const fileExplorer = sideDrawer.findComponent(DialogFileExplorer);
      expect(fileExplorer.props().relativeToOptions).toBeDefined();
      expect(fileExplorer.props().relativeToOptions?.isRelativeTo).toBe(true);
    });
  });

  describe("initial Path Resolution", () => {
    it("opens file chooser in relative-to-workflow mode with relative path", async () => {
      props.control.data.path.fsCategory = "relative-to-workflow";
      props.control.data.path.path = "../../data/output.csv";

      const resolvePathSpy = vi.fn(() => Promise.resolve("/resolved/path"));
      const customDataService = vi.fn(
        (params: { method?: string | undefined } | undefined) => {
          if (params?.method === "fileChooser.resolveRelativePath") {
            return resolvePathSpy();
          }
          if (params?.method === "fileChooser.listItems") {
            return Promise.resolve({
              folder: {
                items: [],
                path: "/data",
                parentFolders: [
                  { path: testWorkflowPath, name: null },
                  { path: "/data", name: "data" },
                ],
              },
              filePathRelativeToFolder: "",
              errorMessage: null,
            });
          }
          return Promise.resolve({});
        },
      );

      const { wrapper } = mountFileChooserControl({
        props,
        dataService: customDataService,
      });

      await expandSideDrawer(wrapper);
      await flushPromises();

      const sideDrawer = wrapper.findComponent(SideDrawerContent);
      const fileExplorer = sideDrawer.findComponent(DialogFileExplorer);

      expect(fileExplorer.exists()).toBeTruthy();
      expect(fileExplorer.props().relativeToOptions).toBeDefined();
      expect(fileExplorer.props().relativeToOptions?.relativeToPath).toBe(
        testWorkflowPath,
      );
    });

    it("displays correct folder and breadcrumbs after switching from Embedded tab back to Space tab", async () => {
      props.control.data.path.fsCategory = "relative-to-workflow";
      props.control.data.path.path = "data/file.txt";

      const { wrapper } = mountFileChooserControl({
        props,
      });

      await expandSideDrawer(wrapper);
      await flushPromises();

      const sideDrawer = wrapper.findComponent(SideDrawerContent);
      let fileExplorer = sideDrawer.findComponent(DialogFileExplorer);
      const initialBreadcrumbs = fileExplorer.props().breadcrumbRoot;

      sideDrawer.vm.$emit("update:modelValue", {
        ...props.control.data.path,
        fsCategory: "relative-to-embedded-data",
      });
      await nextTick();
      await flushPromises();

      sideDrawer.vm.$emit("update:modelValue", {
        ...props.control.data.path,
        fsCategory: "relative-to-workflow",
      });
      await nextTick();
      await flushPromises();

      fileExplorer = sideDrawer.findComponent(DialogFileExplorer);
      expect(fileExplorer.props().relativeToOptions).toBeDefined();
      expect(fileExplorer.props().relativeToOptions?.isRelativeTo).toBe(true);
      expect(fileExplorer.props().breadcrumbRoot).toBe(initialBreadcrumbs);
    });
  });
});
