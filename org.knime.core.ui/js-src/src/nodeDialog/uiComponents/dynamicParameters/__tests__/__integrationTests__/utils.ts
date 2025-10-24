import { vi } from "vitest";
import { type VueWrapper, mount } from "@vue/test-utils";

import { JsonDataService } from "@knime/ui-extension-service";

import NodeDialog from "@/nodeDialog/NodeDialog.vue";
import { mockRegisterSettings } from "@/nodeDialog/__tests__/__integrationTests__/utils/dirtySettingState";
import { dynamicImportsSettled } from "@/nodeDialog/__tests__/__integrationTests__/utils/dynamicImportsSettled";
import { getOptions } from "@/nodeDialog/__tests__/utils";
import type { InitialData } from "@/nodeDialog/types/InitialData";
import type { PersistSchema } from "@/nodeDialog/types/Persist";
import type { Update, UpdateResult } from "@/nodeDialog/types/Update";

export type NodeDialogWrapper = VueWrapper<any> & {
  vm: {
    getCurrentData(): any;
    schema: {
      flowVariablesMap: Record<string, any>;
      getData(): any;
    };
  };
};

/**
 * Helper to create initial data for a dynamic input control
 */
export const createDynamicInputInitialData = ({
  scope = "#/properties/model/properties/myDynamicSettings",
  initialValue = {},
  providedOptions = ["dynamicSettings"],
  outerPersistSchema,
}: {
  scope?: string;
  initialValue?: object;
  providedOptions?: string[];
  outerPersistSchema?: PersistSchema | null;
} = {}): InitialData => {
  const uiSchemaKey = "ui_schema";

  return {
    data: {
      model: { myDynamicSettings: initialValue },
    },
    schema: {
      type: "object",
      properties: {
        model: {
          type: "object",
          properties: {
            myDynamicSettings: {
              type: "object",
            },
          },
        },
      },
    },
    [uiSchemaKey]: {
      // @ts-ignore
      elements: [
        {
          scope,
          type: "Control",
          options: {
            format: "dynamicInput",
          },
          providedOptions,
        },
      ],
    },
    persist: outerPersistSchema || {},
    flowVariableSettings: {},
  };
};

/**
 * Creates a dynamic settings update result
 */
export const createDynamicSettingsUpdate = ({
  scope = "#/properties/model/properties/myDynamicSettings",
  data,
  schema,
  uiSchema,
  updates = null,
  persist = null,
  settingsId = null,
}: {
  scope?: string;
  data?: object | null;
  schema?: object;
  uiSchema?: object;
  updates?: {
    globalUpdates?: Update[];
    initialUpdates?: UpdateResult[];
  } | null;
  persist?: object | null;
  settingsId?: string | null;
}): UpdateResult => {
  const defaultSchema = {
    type: "object",
    properties: {
      key: { type: "string", title: "My title" },
    },
  };

  const defaultUiSchema = {
    elements: [
      {
        scope: "#/properties/key",
        type: "Control",
      },
    ],
  };

  return {
    scope,
    providedOptionName: "dynamicSettings",
    values: [
      {
        indices: [],
        value: {
          data: data ?? null,
          schema: JSON.stringify(schema ?? defaultSchema),
          uiSchema: JSON.stringify(uiSchema ?? defaultUiSchema),
          ...(updates !== null || persist !== null || settingsId !== null
            ? {
                updates: updates ? JSON.stringify(updates) : null,
                persist: persist ? JSON.stringify(persist) : null,
                settingsId,
              }
            : {}),
        },
      },
    ],
  };
};

/**
 * Mocks the initial update that triggers after opening the dialog
 */
export const mockInitialUpdate = (
  initialDataMock: InitialData,
  updateResult: UpdateResult,
  additionalHandlers?: (params: any) => Promise<any> | undefined,
) => {
  initialDataMock.globalUpdates = [
    {
      dependencies: [],
      trigger: { id: "afterOpenDialog" },
      triggerInitially: true,
    },
  ];

  // To prevent dynamic import problems, we render an extra text input initially
  // @ts-expect-error
  initialDataMock.schema.properties.model.properties.someTextInput = {
    type: "string",
  };
  // @ts-expect-error
  initialDataMock.schema.properties.model.properties.someDropDown = {
    type: "string",
  };
  // @ts-expect-error
  initialDataMock.schema.properties.someButton = {
    title: "Some Button",
  };
  // @ts-expect-error
  initialDataMock.ui_schema.elements.push({
    type: "Section",
    label: "Extra Controls",
    description:
      "These controls are only added to avoid dynamic import " +
      "issues when loading the same components dynamically later.",
    elements: [
      {
        scope: "#/properties/model/properties/someTextInput",
        type: "Control",
      },
      {
        scope: "#/properties/model/properties/someDropDown",
        type: "Control",
        options: {
          format: "dropDown",
        },
      },
      {
        scope: "#/properties/someButton",
        type: "Control",
        options: {
          format: "simpleButton",
          showTitleAndDescription: true,
          triggerId: "someButtonTriggerId",
        },
      },
    ],
  });

  return vi
    .spyOn(JsonDataService.prototype, "data")
    .mockImplementation((params) => {
      if (params?.method === "settings.update2") {
        return Promise.resolve({
          state: "SUCCESS",
          result: [updateResult],
        });
      }
      // Try additional handlers if provided
      if (additionalHandlers) {
        const result = additionalHandlers(params);
        // eslint-disable-next-line no-undefined
        if (result !== undefined) {
          return result;
        }
      }
      return Promise.resolve();
    });
};

/**
 * Mounts the NodeDialog with the provided initial data
 */
export const mountNodeDialog = async (
  initialData: InitialData,
): Promise<NodeDialogWrapper> => {
  vi.spyOn(JsonDataService.prototype, "initialData").mockResolvedValue(
    initialData,
  );
  mockRegisterSettings();
  const wrapper = mount(NodeDialog as any, getOptions());
  await dynamicImportsSettled(wrapper);
  return wrapper as NodeDialogWrapper;
};

/**
 * Creates a complete test setup with mocked update
 */
export const setupDynamicInputTest = async ({
  initialValue = {},
  dynamicData = { key: "value" },
  dynamicSchema,
  dynamicUiSchema,
  updates = null,
  persist = null,
  settingsId = null,
  outerPersistSchema = null,
  additionalDataServiceHandlers,
}: {
  initialValue?: object;
  dynamicData?: object | null;
  dynamicSchema?: object;
  dynamicUiSchema?: object;
  updates?: {
    globalUpdates?: Update[];
    initialUpdates?: UpdateResult[];
  } | null;
  persist?: object | null;
  settingsId?: string | null;
  outerPersistSchema?: PersistSchema | null;
  additionalDataServiceHandlers?: (params: any) => Promise<any> | undefined;
} = {}) => {
  const initialData = createDynamicInputInitialData({
    initialValue,
    outerPersistSchema,
  });

  const updateResult = createDynamicSettingsUpdate({
    data: dynamicData,
    schema: dynamicSchema,
    uiSchema: dynamicUiSchema,
    updates,
    persist,
    settingsId,
  });

  const dataSpy = mockInitialUpdate(
    initialData,
    updateResult,
    additionalDataServiceHandlers,
  );

  const wrapper = await mountNodeDialog(initialData);

  return { wrapper, initialData, updateResult, dataSpy };
};
