import { beforeEach, describe, expect, it, vi } from "vitest";
import { VueWrapper, mount } from "@vue/test-utils";
import flushPromises from "flush-promises";

import {
  JsonDataService,
  SharedDataService,
} from "@knime/ui-extension-service";

import NodeDialog from "../../NodeDialog.vue";
import type { Result } from "../../api/types/Result";
import type { Trigger, Update, UpdateResult } from "../../types/Update";
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

  const getUpdatedScope = ({
    trigger,
    getScope,
  }: {
    trigger: Trigger;
    getScope: (fieldKey: string) => string;
  }) => {
    if (!("scope" in trigger)) {
      return getScope("a");
    }
    if (trigger.scope.endsWith("a")) {
      return getScope("b");
    }
    if (trigger.scope.endsWith("b")) {
      return getScope("c");
    }
    throw new Error("Unexpected trigger scope");
  };

  const getGlobalUpdates = ({
    getScope,
  }: {
    getScope: (fieldKey: string) => string;
  }): Update[] => [
    {
      trigger: {
        id: triggers.Initially,
      },
      triggerInitially: true as const,
      dependencies: [] as string[],
    },
    {
      trigger: {
        scope: getScope("a"),
      },
      triggerInitially: undefined,
      dependencies: [] as string[],
    },
    {
      trigger: {
        scope: getScope("b"),
      },
      triggerInitially: undefined,
      dependencies: [] as string[],
    },
  ];

  const mockInitialData = (initialDataJson: object) =>
    vi
      .spyOn(JsonDataService.prototype, "initialData")
      .mockResolvedValue(initialDataJson);

  const toBeResolved: (() => void)[] = [];

  const mockRpcCall = (getScope: (fieldKey: string) => string) =>
    vi.spyOn(JsonDataService.prototype, "data").mockImplementation(
      ({ options } = { options: [] }) =>
        new Promise<Result<UpdateResult[]>>((resolve) => {
          const resolvePromise = () => {
            return resolve({
              state: "SUCCESS",
              result: [
                {
                  scope: getUpdatedScope({ trigger: options[1], getScope }),
                  values: [{ indices: [], value: "Updated" }],
                  id: null,
                },
              ] satisfies UpdateResult[],
            });
          };
          toBeResolved.push(resolvePromise);
        }),
    );

  const mountNodeDialog = async ({
    initialDataJson,
    getScope,
  }: {
    initialDataJson: object;
    getScope: (fieldKey: string) => string;
  }) => {
    vi.clearAllMocks();
    mockInitialData(initialDataJson);
    mockRpcCall(getScope);
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
    const getScope = (fieldKey: string) =>
      `#/properties/view/properties/${fieldKey}`;
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
          getScope,
        }),
        initialUpdates: [] as UpdateResult[],
        flowVariableSettings: {},
      },
      getScope,
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
    const getScope = (fieldKey: string) =>
      `#/properties/values/items/properties/${fieldKey}`;
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
          getScope,
        }),
        initialUpdates: [] as UpdateResult[],
        flowVariableSettings: {},
      },
      getScope,
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
