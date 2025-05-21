import { describe, expect, it, vi } from "vitest";
import { nextTick } from "vue";
import { flushPromises } from "@vue/test-utils";

import {
  type VueControlTestProps,
  getControlBase,
  mountJsonFormsControl,
} from "@knime/jsonforms/testing";

import DBTableChooserControl from "../DBTableChooserControl.vue";
import DBTableChooserFileExplorer from "../DBTableChooserFileExplorer.vue";
import {
  type DBItem,
  type DbTableChooserBackend,
  type ItemTypeResult,
  type ListItemsResult,
} from "../useDbTableChooserBackend";

const shouldUseCatalogs = vi.hoisted(() => vi.fn(() => true));

const itemTreeProvider = vi.hoisted(() =>
  vi.fn((usesCatalogues: boolean) => {
    const itemTree: Record<string, any> = {
      testCatalog: {
        type: "CATALOG",
        children: {
          testSchema: {
            type: "SCHEMA",
            children: {
              testTable: {
                type: "TABLE",
                name: "testTable",
              },
              anotherTable: {
                type: "TABLE",
                name: "anotherTable",
              },
            },
          },
          anotherSchema: {
            type: "SCHEMA",
            children: {},
          },
        },
      },
      anotherCatalog: {
        type: "CATALOG",
        children: {
          testSchema: {
            type: "SCHEMA",
            children: {},
          },
        },
      },
    };

    return usesCatalogues
      ? itemTree
      : (itemTree.testCatalog.children as Record<string, any>);
  }),
);

vi.mock("../useDbTableChooserBackend.ts", () => {
  const thingy: DbTableChooserBackend = {
    isDbConnected: vi.fn(() => Promise.resolve(true)),
    supportsCatalogs: vi.fn(() => Promise.resolve(shouldUseCatalogs())),
    listItems: vi.fn((pathParts: string[]) => {
      let currentNode = itemTreeProvider(shouldUseCatalogs());
      for (const part of pathParts) {
        if (part in currentNode) {
          currentNode = currentNode[part].children as Record<string, any>;
          true;
        } else {
          const result: ListItemsResult = {
            type: "ERROR",
            message: `Path not found: ${pathParts.join("/")}`,
          };
          return Promise.resolve(result);
        }
      }

      const result: ListItemsResult = {
        type: "SUCCESS",
        data: {
          path: pathParts,
          children: Object.entries(currentNode).map(([key, val]) => ({
            name: key,
            type: val.type,
          })),
        },
      };

      return Promise.resolve(result);
    }),
    itemType: vi.fn((pathParts: string[]) => {
      let currentNode = itemTreeProvider(shouldUseCatalogs());
      for (const part of pathParts) {
        if (part in currentNode) {
          currentNode = currentNode[part].children as Record<string, any>;
        } else {
          return Promise.resolve(null);
        }
      }

      const result: ItemTypeResult = currentNode.type as DBItem["type"];

      return Promise.resolve(result);
    }),
  };

  return { useDbTableChooserBackend: () => thingy };
});

const BASE_PROPS: Omit<
  VueControlTestProps<typeof DBTableChooserControl>,
  "messages"
> = {
  control: {
    ...getControlBase("test"),
    data: {
      schemaName: "testSchema",
      tableName: "testTable",
      catalogName: "testCatalog",
    },
    schema: {
      type: "object",
      title: "File path",
      properties: {
        schemaName: {
          type: "string",
          title: "Schema Name",
          description: "The name of the schema to select.",
          default: "defaultSchema",
        },
        tableName: {
          type: "string",
          title: "Table Name",
          description: "The name of the table to select.",
          default: "defaultTable",
        },
        catalogName: {
          type: "string",
          title: "Catalog Name",
          description: "The name of the catalog to select.",
          default: "defaultCatalog",
        },
      },
    },
    uischema: {
      type: "Control",
      scope: "#/properties/view/properties/dbSelection",
      options: {
        format: "dbTableChooser",
      },
    },
  },

  disabled: false,
  isValid: true,
};

const doMount = async (
  {
    stubs,
    props,
  }: {
    stubs?: Record<string, boolean>;
    props?: Omit<VueControlTestProps<typeof DBTableChooserControl>, "messages">;
  } = {
    stubs: {},
  },
) => {
  const fullProps = {
    ...BASE_PROPS,
    ...props,
  };

  const mountedResult = mountJsonFormsControl(DBTableChooserControl, {
    // @ts-expect-error since we don't have the messages property
    props: fullProps,
    stubs: {
      DBTableChooserFileExplorer,
      ...stubs,
    },
  });

  await nextTick();
  await flushPromises();

  return mountedResult;
};

describe("DBTableChooserControl", () => {
  it("renders", async () => {
    const { wrapper } = await doMount();
    expect(wrapper.findComponent(DBTableChooserControl).exists()).toBe(true);
  });

  it("validates inputs and displays errors iff relevant", async () => {
    const props = {
      ...BASE_PROPS,
    };
    props.control.data.catalogName = "testCatalog";
    props.control.data.schemaName = "testSchema";
    props.control.data.tableName = "testTable";
    const { wrapper } = await doMount({ props });

    expect(wrapper.findAll(".error-text").length).toBe(0);

    // now mount with invalid data
    const invalidProps = {
      ...BASE_PROPS,
    };
    invalidProps.control.data.catalogName = "invalidCatalog";
    invalidProps.control.data.schemaName = "invalidSchema";
    invalidProps.control.data.tableName = "invalidTable";
    const { wrapper: invalidWrapper } = await doMount({ props: invalidProps });
    expect(invalidWrapper.findAll(".error-text").length).toBe(3);
  });

  it("displays 3 input fields for catalog, schema and table when using catalogs", async () => {
    shouldUseCatalogs.mockReturnValue(true);
    const { wrapper } = await doMount();
    expect(wrapper.findAll(".error-message").length).toBe(3);
  });

  it("displays 2 input fields for schema and table when not using catalogs", async () => {
    shouldUseCatalogs.mockReturnValue(false);
    const { wrapper } = await doMount();
    expect(wrapper.findAll(".error-message").length).toBe(2);
  });
});
