import { beforeEach, describe, expect, it, vi } from "vitest";
import { VueWrapper, mount } from "@vue/test-utils";
import type { UISchemaElement } from "@jsonforms/core";
import flushPromises from "flush-promises";

import { Label, ValueSwitch } from "@knime/components";
import { JsonDataService } from "@knime/ui-extension-service";

import type { MultiFileChooserOptions } from "@/nodeDialog/types/FileChooserUiSchema";
import NodeDialog from "../../../../../NodeDialog.vue";
import { mockRegisterSettings } from "../../../../../__tests__/__integrationTests__/utils/dirtySettingState";
import { dynamicImportsSettled } from "../../../../../__tests__/__integrationTests__/utils/dynamicImportsSettled";
import { getOptions } from "../../../../../__tests__/utils";
import type { PreviewResult } from "../../../composables/useFileFilterPreviewBackend";
import type { MultiFileSelection, PathAndError } from "../../../types";
import MultiFileChooserControl from "../../MultiFileChooserControl.vue";

describe("multi file selection", () => {
  beforeEach(() => {
    mockRegisterSettings();
  });

  const uiSchemaKey = "ui_schema";

  const initialDataJson = {
    data: {
      model: {
        value: {
          path: {
            path: "initial/path",
            fsCategory: "LOCAL",
            timeout: 0,
          },
          filterMode: "FILE",
          filters: {
            someFilterValue: "someFilterValue",
          },
          includeSubfolders: false,
        } satisfies MultiFileSelection,
      },
    },
    schema: {
      type: "object",
      properties: {
        model: {
          type: "object",
          properties: {
            value: {
              type: "object",
              properties: {
                path: {
                  title: "Source",
                  type: "object",
                  properties: {
                    path: {
                      type: "string",
                    },
                    fsCategory: {
                      type: "string",
                    },
                    timeout: {
                      type: "integer",
                    },
                  },
                },
                filterMode: {
                  type: "string",
                  title: "Type",
                  oneOf: [
                    {
                      const: "FILE",
                      title: "File",
                    },
                    {
                      const: "FOLDER",
                      title: "Folder",
                    },
                  ],
                },
                filters: {
                  type: "object",
                  properties: {
                    someFilterValue: {
                      type: "string",
                    },
                  },
                },
                includeSubfolders: {
                  title: "Include subfolders",
                  type: "boolean",
                },
              },
            },
          },
        },
      },
    },
    [uiSchemaKey]: {
      elements: [
        {
          scope: "#/properties/model/properties/value",
          type: "Control",
          options: {
            format: "multiFileChooser",
            isLocal: true,
            filters: {
              classId: "someClassIdentifier",
              uiSchema: {
                elements: [
                  {
                    type: "Control",
                    scope: "#/properties/someFilterValue",
                  },
                ],
              } as unknown as UISchemaElement,
            },
            possibleFilterModes: ["FILE", "FILES_IN_FOLDERS"],
          } satisfies MultiFileChooserOptions & {
            format: string;
          },
        },
      ],
    },
    flowVariableSettings: {},
  };

  const mockInitialData = (initialDataJson: object) =>
    vi
      .spyOn(JsonDataService.prototype, "initialData")
      .mockResolvedValue(initialDataJson);

  const methodToData: Map<
    string,
    {
      options: unknown;
      resolve: (result: unknown) => void;
    }
  > = new Map();

  const mockRpcCall = () =>
    vi.spyOn(JsonDataService.prototype, "data").mockImplementation(
      ({ options, method } = { options: [] }) =>
        new Promise<unknown>((resolve) => {
          methodToData.set(method!, {
            options,
            resolve,
          });
        }),
    );

  const mountNodeDialog = async () => {
    vi.clearAllMocks();
    mockInitialData(initialDataJson);
    mockRpcCall();
    const wrapper = mount(NodeDialog, getOptions());
    await dynamicImportsSettled(wrapper);
    return wrapper;
  };

  const findLabeledComponent = (
    wrapper: VueWrapper,
    label: string,
    container: VueWrapper = wrapper,
  ) => {
    const labels = wrapper.findAllComponents(Label);
    const labelComponent = labels.find(
      (labelComponent) => labelComponent.props("text") === label,
    );
    const labelForId = labelComponent?.emitted("labelForId")![0][0];
    expect(labelForId).toBeDefined();
    return container.find(`#${labelForId}`);
  };

  const resolveInitialBackendRequests = async () => {
    expect([...methodToData.keys()]).toStrictEqual(["fileChooser.getFilePath"]);
    const getFilePath = methodToData.get("fileChooser.getFilePath")!;
    expect(getFilePath.options).toStrictEqual([
      "local",
      null,
      "initial/path",
      null,
      null,
    ]);
    getFilePath.resolve({
      errorMessage: null,
      path: "initial/path",
    } satisfies PathAndError);
    methodToData.delete("fileChooser.getFilePath");
    await flushPromises();
  };

  it("renders multi file selection", async () => {
    const wrapper = await mountNodeDialog();
    const multiFileChooser = wrapper.findComponent(MultiFileChooserControl);
    expect(multiFileChooser.exists()).toBe(true);
    const labels = wrapper.findAllComponents(Label);
    expect(labels.map((label) => label.props("text"))).toStrictEqual([
      "Type",
      "Source",
    ]);

    const typeSwitch = findLabeledComponent(wrapper, "Type").findComponent(
      ValueSwitch,
    );
    expect(typeSwitch.props()).toMatchObject({
      modelValue: "FILE",
      possibleValues: [
        {
          id: "FILE",
          text: "File",
        },
        {
          id: "FILES_IN_FOLDERS",
          text: "Files in folders",
        },
      ],
    });

    const fileChooserControlBase = wrapper.findComponent({
      name: "FileChooserControlBase",
    });
    expect(
      findLabeledComponent(wrapper, "Source", fileChooserControlBase).exists(),
    ).toBeTruthy();
    expect(
      fileChooserControlBase.props("control").uischema.options,
    ).toMatchObject({
      selectionMode: "FILE",
    });

    await resolveInitialBackendRequests();

    typeSwitch.vm.$emit("update:model-value", "FILES_IN_FOLDERS");
    await flushPromises();
    expect([...methodToData.keys()]).toStrictEqual([
      "fileFilterPreview.listItemsForPreview",
    ]);
    const listItemsForPreview = methodToData.get(
      "fileFilterPreview.listItemsForPreview",
    )!;
    expect(listItemsForPreview.options).toStrictEqual([
      "local",
      "initial/path",
      "FILES_IN_FOLDERS",
      false,
      {
        additionalFilterOptions: {
          someFilterValue: "someFilterValue",
          filterMode: "FILES_IN_FOLDERS",
        },
        additionalFilterOptionsClassIdentifier: "someClassIdentifier",
      },
    ]);
    listItemsForPreview.resolve({
      itemsAfterFiltering: ["file1", "file2"],
      numItemsBeforeFiltering: 300,
      numFilesBeforeFilteringIsOnlyLowerBound: true,
      numFilesAfterFilteringIsOnlyLowerBound: false,
      resultType: "SUCCESS",
    } satisfies PreviewResult);
    await flushPromises();

    expect(wrapper.text()).toContain("2 of 300+ files");
    await wrapper
      .findAllComponents("button")
      .find((button) => button.text() === "Show all")!
      .trigger("click");
    expect(wrapper.text()).toContain("file1");
    expect(wrapper.text()).toContain("file2");
  });
});
