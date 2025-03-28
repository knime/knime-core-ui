import { beforeEach, describe, expect, it, vi } from "vitest";
import { VueWrapper, mount } from "@vue/test-utils";
import flushPromises from "flush-promises";

import {
  JsonDataService,
  SharedDataService,
} from "@knime/ui-extension-service";

import NodeDialog from "../../NodeDialog.vue";
import type { Result } from "../../api/types/Result";
import type { Update, UpdateResult, ValueReference } from "../../types/Update";
import { getOptions } from "../utils";

import { mockRegisterSettings } from "./utils/dirtySettingState";
import { dynamicImportsSettled } from "./utils/dynamicImportsSettled";

describe("transitive updates", () => {
  type Wrapper = VueWrapper<any> & {
    vm: {
      schema: {
        flowVariablesMap: Record<string, any>;
        getData(): any;
      };
    };
  };

  beforeEach(() => {
    mockRegisterSettings();
  });

  const uiSchemaKey = "ui_schema";

  const triggers = {
    Initially: "Initially",
    A: "A",
    B: "B",
  } as const;
  const updates = {
    Initially: "a",
    A: "b",
    B: "c",
  };

  const elementSchema = {
    type: "object",
    properties: {
      a: {
        type: "string",
      },
      b: {
        type: "string",
      },
      c: {
        type: "string",
      },
    },
  };

  const elementUiSchema = [
    {
      scope: "#/properties/model/properties/a",
      type: "Control",
    },
    {
      scope: "#/properties/model/properties/b",
      type: "Control",
    },
    {
      scope: "#/properties/model/properties/c",
      type: "Control",
    },
  ];

  const getGlobalUpdates = ({
    getScopes,
  }: {
    getScopes: (fieldKey: string) => string[];
  }): Update[] => [
    {
      trigger: {
        scopes: undefined,
        id: triggers.Initially,
        triggerInitially: true as const,
      },
      dependencies: [] as ValueReference[],
    },
    {
      trigger: {
        scopes: getScopes("a"),
        id: triggers.A,
        triggerInitially: undefined,
      },
      dependencies: [] as ValueReference[],
    },
    {
      trigger: {
        scopes: getScopes("b"),
        id: triggers.B,
        triggerInitially: undefined,
      },
      dependencies: [] as ValueReference[],
    },
  ];

  const mockInitialData = (initialDataJson: object) =>
    vi
      .spyOn(JsonDataService.prototype, "initialData")
      .mockResolvedValue(initialDataJson);

  const toBeResolved: (() => void)[] = [];

  const mockRpcCall = (getScopes: (fieldKey: string) => string[]) =>
    vi.spyOn(JsonDataService.prototype, "data").mockImplementation(
      ({ options } = { options: [] }) =>
        new Promise<Result<UpdateResult[]>>((resolve) => {
          const resolvePromise = () =>
            resolve({
              state: "SUCCESS",
              result: [
                {
                  scopes: getScopes(
                    updates[options[1] as keyof typeof triggers],
                  ),
                  values: [{ indices: [], value: "Updated" }],
                  id: null,
                },
              ] satisfies UpdateResult[],
            });
          toBeResolved.push(resolvePromise);
        }),
    );

  const mountNodeDialog = async ({
    initialDataJson,
    getScopes,
  }: {
    initialDataJson: object;
    getScopes: (fieldKey: string) => string[];
  }) => {
    vi.clearAllMocks();
    mockInitialData(initialDataJson);
    mockRpcCall(getScopes);
    const wrapper = mount(NodeDialog as any, getOptions()) as Wrapper;
    await dynamicImportsSettled(wrapper);
    return wrapper;
  };

  const flushNextPromise = async () => {
    expect(toBeResolved.length).toBe(1);
    toBeResolved.pop()!();
    await flushPromises();
  };

  it("triggers transitive updates", async () => {
    const getScopes = (fieldKey: string) => [
      `#/properties/view/properties/${fieldKey}`,
    ];
    const shareDataSpy = vi.spyOn(SharedDataService.prototype, "shareData");
    const wrapper = await mountNodeDialog({
      initialDataJson: {
        data: {
          view: {
            c: "InitialValue",
          },
        },
        schema: {
          type: "object",
          properties: {
            view: elementSchema,
          },
        },
        [uiSchemaKey]: {
          elements: elementUiSchema,
        },
        globalUpdates: getGlobalUpdates({
          getScopes,
        }),
        initialUpdates: [] as UpdateResult[],
        flowVariableSettings: {},
      },
      getScopes,
    });
    shareDataSpy.mockClear();

    await flushNextPromise();

    expect(wrapper.vm.getCurrentData().view).toStrictEqual({
      a: "Updated",
      c: "InitialValue",
    });
    expect(shareDataSpy).toHaveBeenCalledTimes(1);
    shareDataSpy.mockClear();

    await flushNextPromise();

    expect(wrapper.vm.getCurrentData().view).toStrictEqual({
      a: "Updated",
      b: "Updated",
      c: "InitialValue",
    });
    expect(shareDataSpy).toHaveBeenCalledTimes(1);
    shareDataSpy.mockClear();

    await flushNextPromise();

    expect(wrapper.vm.getCurrentData().view).toStrictEqual({
      a: "Updated",
      b: "Updated",
      c: "Updated",
    });
    expect(shareDataSpy).toHaveBeenCalledTimes(1);
  });

  it("triggers transitive updates within array layouts", async () => {
    const getScopes = (fieldKey: string) => [
      "#/properties/values",
      `#/properties/${fieldKey}`,
    ];
    const wrapper = await mountNodeDialog({
      initialDataJson: {
        data: {
          values: [
            {
              a: "InitialValue",
            },
          ],
        },
        schema: {
          type: "object",
          properties: {
            values: {
              type: "array",
              items: elementSchema,
            },
          },
        },
        [uiSchemaKey]: {
          elements: [
            {
              scope: "#/properties/values",
              type: "Control",
              options: {
                arrayElementTitle: "Element",
                detail: elementUiSchema,
              },
            },
          ],
        },
        globalUpdates: getGlobalUpdates({
          getScopes,
        }),
        initialUpdates: [] as UpdateResult[],
        flowVariableSettings: {},
      },
      getScopes,
    });

    await flushNextPromise();

    expect(wrapper.vm.getCurrentData().values[0]).toMatchObject({
      a: "Updated",
    });

    await flushNextPromise();

    expect(wrapper.vm.getCurrentData().values[0]).toMatchObject({
      a: "Updated",
      b: "Updated",
    });
    await flushNextPromise();

    expect(wrapper.vm.getCurrentData().values[0]).toMatchObject({
      a: "Updated",
      b: "Updated",
      c: "Updated",
    });
  });
});
