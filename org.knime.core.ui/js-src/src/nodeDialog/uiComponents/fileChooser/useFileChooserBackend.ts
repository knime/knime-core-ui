import {
  JsonDataService,
  type KnimeService,
} from "@knime/ui-extension-service";

export interface Item {
  isDirectory: boolean;
  path: string;
}

type ListItems = (params: {
  method: "fileChooser.listItems";
  options: [
    /**
     * The path relative to the file system root.
     */
    string,
  ];
}) => Promise<Item[]>;

type GetRootItems = (params: {
  method: "fileChooser.getRootItems";
  options: [];
}) => Promise<Item[]>;

export default (knimeService: KnimeService) => {
  const jsonDataService = new JsonDataService(knimeService);
  const listItems = (path: string) => {
    return (jsonDataService.data as ListItems)({
      method: "fileChooser.listItems",
      options: [path],
    });
  };
  const getRootItems = () => {
    return (jsonDataService.data as GetRootItems)({
      method: "fileChooser.getRootItems",
      options: [],
    });
  };
  return { listItems, getRootItems };
};
