import inject from "../../utils/inject";

export type DBItemType = "TABLE" | "SCHEMA" | "CATALOG";
export type DBTableType = "TABLE" | "VIEW";
export interface DBTableMetadata {
  tableType: DBTableType;
  containingSchema: string | null;
  containingCatalogue: string | null;
}

export type ContainerDBItem = {
  name: string;
  type: "CATALOG" | "SCHEMA";
  tableMetadata: null;
};

export type TableDBItem = {
  name: string;
  type: "TABLE";
  tableMetadata: DBTableMetadata;
};

export type DBItem = ContainerDBItem | TableDBItem;

export type DBContainer = {
  pathParts: string[];
  children: DBItem[];
};

export type ListItemsResult = {
  nextValidData: DBContainer | null;
  errorMessage: string | null;
};

export type ItemTypeResult = DBItemType | null;

export type DBTableChooserListItems = (params: {
  method: "dbTableChooser.listItems";
  options: [
    /**
     * The path of the folder containing the file.
     */
    (string | null)[],
    /**
     * The selected table name, which will affect the error message iff the path leads
     * to a schema and the table does not exist. It will not affect the data returned
     * and may be empty, in which case it is ignored.
     */
    string | null,
  ];
}) => Promise<ListItemsResult>;

export type DBTableChooserItemType = (params: {
  method: "dbTableChooser.itemType";
  options: [
    /**
     * The path of the item.
     */
    (string | null)[],
  ];
}) => Promise<ItemTypeResult>;

export type DbTableChooserBackend = {
  listItems: (
    path: (string | null)[],
    table: string | null,
  ) => Promise<ListItemsResult>;
  itemType: (path: (string | null)[]) => Promise<ItemTypeResult>;
};

export const useDbTableChooserBackend = (): DbTableChooserBackend => {
  const getData = inject("getData") as DBTableChooserListItems &
    DBTableChooserItemType;

  const listItems = (path: (string | null)[], table: string | null) =>
    getData({
      method: "dbTableChooser.listItems",
      options: [path, table],
    });

  const itemType = (path: (string | null)[]) =>
    getData({
      method: "dbTableChooser.itemType",
      options: [path],
    });

  return {
    listItems,
    itemType,
  };
};

/**
 * Combined DB Table Chooser API type
 */
export type DBTableChooserRpcMethods = DBTableChooserListItems &
  DBTableChooserItemType;
