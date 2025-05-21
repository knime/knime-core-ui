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

export const useDbTableChooserBackend = () => {
  const getData = inject("getData") as ListItems &
    SupportsCatalogs &
    IsDbConnected;

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

  return {
    listItems,
    supportsCatalogs,
    isDbConnected,
  };
};
