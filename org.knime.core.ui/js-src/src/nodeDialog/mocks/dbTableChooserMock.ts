import type {
  DBItem,
  DBItemType,
  DbTableChooserBackend,
  ListItemsResult,
} from "../uiComponents/dbTableChooser/useDbTableChooserBackend";

type DBItemMock = Omit<DBItem, "name"> &
  (
    | {
        type: "TABLE";
        children?: never;
      }
    | {
        type: "SCHEMA" | "CATALOG";
        children: Record<string, DBItemMock>;
      }
  );

export const mockedItemTreeProvider = (
  usesCatalogues: boolean,
): Record<string, DBItemMock> => {
  const itemTree = {
    testCatalog: {
      type: "CATALOG",
      children: {
        testSchema: {
          type: "SCHEMA",
          children: {
            testTable: {
              type: "TABLE",
              tableMetadata: {
                tableType: "TABLE",
                containingSchema: "differentSchema",
                containingCatalogue: "testCatalog",
              },
            },
            testView: {
              type: "TABLE",
              tableMetadata: {
                tableType: "VIEW",
                containingSchema: "testSchema",
                containingCatalogue: "testCatalog",
              },
            },
          },
          tableMetadata: null,
        },
        anotherSchema: {
          type: "SCHEMA",
          children: {},
          tableMetadata: null,
        },
      },
      tableMetadata: null,
    },
    anotherCatalog: {
      type: "CATALOG",
      children: {
        testSchema: {
          type: "SCHEMA",
          children: {},
          tableMetadata: null,
        },
      },
      tableMetadata: null,
    },
  } as const;

  return usesCatalogues ? itemTree : itemTree.testCatalog.children;
};

const mapChildrenToItems = (currentNode: Record<string, DBItemMock>) => {
  return Object.entries(currentNode).map(([key, val]) => ({
    name: key,
    type: val.type,
    tableMetadata: val.type === "TABLE" ? val.tableMetadata : null,
  })) as DBItem[];
};

export const makeTableChooserBackendMock = (
  shouldUseCatalogs: boolean,
): DbTableChooserBackend => ({
  listItems: (pathParts: (string | null)[]) => {
    let currentNode = mockedItemTreeProvider(shouldUseCatalogs);
    const validParts: string[] = [];
    for (const part of pathParts) {
      if (
        part !== null &&
        part in currentNode &&
        currentNode[part].type !== "TABLE"
      ) {
        currentNode = currentNode[part].children!;
        validParts.push(part);
      } else {
        return Promise.resolve({
          errorMessage: `Path not found: ${pathParts.join("/")}`,
          nextValidData: {
            children: mapChildrenToItems(currentNode),
            pathParts: validParts,
          },
        } satisfies ListItemsResult);
      }
    }

    return Promise.resolve({
      nextValidData: {
        pathParts: validParts,
        children: mapChildrenToItems(currentNode),
      },
      errorMessage: null,
    } satisfies ListItemsResult);
  },
  itemType: (pathParts: (string | null)[]) => {
    let currentNodes = mockedItemTreeProvider(shouldUseCatalogs);
    let currentType: DBItemType | null = null;
    for (const part of pathParts) {
      if (part !== null && part in currentNodes) {
        currentType = currentNodes[part].type;
        if (currentNodes[part].type !== "TABLE") {
          currentNodes = currentNodes[part].children!;
        }
      } else {
        return Promise.resolve(null);
      }
    }
    return Promise.resolve(currentType);
  },
});
