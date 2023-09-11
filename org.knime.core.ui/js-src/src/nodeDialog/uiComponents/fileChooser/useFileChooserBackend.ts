import {
  JsonDataService,
  type KnimeService,
} from "@knime/ui-extension-service";

export interface Item {
  isDirectory: boolean;
  path: string;
}

export enum Entity {
  WORKFLOW = "Workflow",
  COMPONENT = "Component",
  WORKFLOW_GROUP = "Workflow group",
  METANODE = "Metanode",
  SPACE = "Space",
  WORKFLOW_TEMPLATE = "Workflow template",
  DATA = "Data",
}

export interface WorkflowAwareItem {
  entity: Entity;
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

type ListItemsWorkflowAware = (params: {
  method: "fileChooser.listItemsWorkflowAware";
  options: [
    /**
     * The path relative to the file system root.
     */
    string,
  ];
}) => Promise<WorkflowAwareItem[]>;

type GetRootWorkflowAwareItems = (params: {
  method: "fileChooser.getRootWorkflowAwareItems";
  options: [];
}) => Promise<WorkflowAwareItem[]>;

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

  const listItemsWorkflowAware = (path: string) => {
    return (jsonDataService.data as ListItemsWorkflowAware)({
      method: "fileChooser.listItemsWorkflowAware",
      options: [path],
    });
  };
  const getRootItems = () => {
    return (jsonDataService.data as GetRootItems)({
      method: "fileChooser.getRootItems",
      options: [],
    });
  };

  const getRootWorkflowAwareItems = () => {
    return (jsonDataService.data as GetRootWorkflowAwareItems)({
      method: "fileChooser.getRootWorkflowAwareItems",
      options: [],
    });
  };
  return {
    listItems,
    listItemsWorkflowAware,
    getRootItems,
    getRootWorkflowAwareItems,
  };
};
