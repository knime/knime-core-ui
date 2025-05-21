import { vi } from "vitest";

import type {
  DBItemType,
  DbTableChooserBackend,
  ListItemsResult,
} from "../useDbTableChooserBackend";

export const mockedItemTreeProvider = (
  usesCatalogues: boolean,
): Record<string, any> => {
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
};

const mapChildrenToItems = (currentNode: Record<string, any>) => {
  return Object.entries(currentNode).map(([key, val]) => ({
    name: key,
    type: val.type,
  }));
};

export const makeTableChooserBackendMock = (
  shouldUseCatalogs: boolean,
): DbTableChooserBackend => ({
  listItems: vi.fn((pathParts: string[]) => {
    let currentNode = mockedItemTreeProvider(shouldUseCatalogs);
    for (const part of pathParts) {
      if (part in currentNode) {
        currentNode = currentNode[part].children as Record<string, any>;
      } else {
        return Promise.resolve({
          errorMessage: `Path not found: ${pathParts.join("/")}`,
          nextValidData: {
            children: mapChildrenToItems(currentNode),
            pathParts,
          },
        } satisfies ListItemsResult);
      }
    }

    return Promise.resolve({
      nextValidData: {
        pathParts,
        children: mapChildrenToItems(currentNode),
      },
      errorMessage: null,
    } satisfies ListItemsResult);
  }),
  itemType: vi.fn((pathParts: string[]) => {
    let currentNode = mockedItemTreeProvider(shouldUseCatalogs);
    for (const part of pathParts) {
      if (part in currentNode) {
        currentNode = currentNode[part].children as Record<string, any>;
      } else {
        return Promise.resolve(null);
      }
    }

    return Promise.resolve(currentNode.type as DBItemType);
  }),
});
