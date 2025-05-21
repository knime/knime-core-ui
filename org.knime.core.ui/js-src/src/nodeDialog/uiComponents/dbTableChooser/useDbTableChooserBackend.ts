import inject from "../../utils/inject";

export type DBItemType = "TABLE" | "SCHEMA" | "CATALOG";

export type DBItem = {
  name: string;
  type: DBItemType;
};

export type DBContainer = {
  path: string[];
  children: DBItem[];
};

export type ListItemsResult =
  | {
      type: "SUCCESS";
      data: DBContainer;
    }
  | {
      type: "ERROR";
      message: string;
    };

export type ItemTypeResult = DBItemType | null;

type ListItems = (params: {
  method: "dbTableChooser.listItems";
  options: [
    /**
     * The path of the folder containing the file.
     */
    string[],
  ];
}) => Promise<ListItemsResult>;

type SupportsCatalogs = (params: {
  method: "dbTableChooser.supportsCatalogs";
  options: [];
}) => Promise<boolean>;

type IsDbConnected = (params: {
  method: "dbTableChooser.isDbConnected";
  options: [];
}) => Promise<boolean>;

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
  listItems: (path: string[]) => Promise<ListItemsResult>;
  supportsCatalogs: () => Promise<boolean>;
  isDbConnected: () => Promise<boolean>;
  itemType: (path: string[]) => Promise<ItemTypeResult>;
};

export const useDbTableChooserBackend = (): DbTableChooserBackend => {
  const getData = inject("getData") as ListItems &
    SupportsCatalogs &
    IsDbConnected &
    ItemType;

  const listItems = (path: string[]) => {
    return getData({
      method: "dbTableChooser.listItems",
      options: [path],
    });
  };

  const supportsCatalogs = () => {
    return getData({
      method: "dbTableChooser.supportsCatalogs",
      options: [],
    });
  };

  const isDbConnected = () => {
    return getData({
      method: "dbTableChooser.isDbConnected",
      options: [],
    });
  };

  const itemType = (path: string[]) => {
    return getData({
      method: "dbTableChooser.itemType",
      options: [path],
    });
  };

  return {
    listItems,
    supportsCatalogs,
    isDbConnected,
    itemType,
  };
};
