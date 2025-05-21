import inject from "../../utils/inject";

export type DBItemType = "TABLE" | "SCHEMA" | "CATALOG";

export type DBItem = {
  name: string;
  type: DBItemType;
};

export type DBContainer = {
  pathParts: string[];
  children: DBItem[];
};

export type ListItemsResult = {
  nextValidData: DBContainer | null;
  errorMessage: string | null;
};

export type ItemTypeResult = DBItemType | null;

type ListItems = (params: {
  method: "dbTableChooser.listItems";
  options: [
    /**
     * The path of the folder containing the file.
     */
    string[],
    /**
     * The selected table name, which will affect the error message iff the path leads
     * to a schema and the table does not exist. It will not affect the data returned
     * and may be empty, in which case it is ignored.
     */
    string | null,
  ];
}) => Promise<ListItemsResult>;

type ItemType = (params: {
  method: "dbTableChooser.itemType";
  options: [
    /**
     * The path of the item.
     */
    string[],
  ];
}) => Promise<ItemTypeResult>;

export type DbTableChooserBackend = {
  listItems: (path: string[], table: string | null) => Promise<ListItemsResult>;
  itemType: (path: string[]) => Promise<ItemTypeResult>;
};

export const useDbTableChooserBackend = (): DbTableChooserBackend => {
  const getData = inject("getData") as ListItems & ItemType;

  const listItems = (path: string[], table: string | null) =>
    getData({
      method: "dbTableChooser.listItems",
      options: [path, table],
    });

  const itemType = (path: string[]) =>
    getData({
      method: "dbTableChooser.itemType",
      options: [path],
    });

  return {
    listItems,
    itemType,
  };
};
