import type { Component, Ref } from "vue";

import type { FileExplorerItem } from "@knime/components";
import FileTextIcon from "@knime/styles/img/icons/file-text.svg";
import FolderIcon from "@knime/styles/img/icons/folder.svg";
import WorkflowIcon from "@knime/styles/img/icons/workflow.svg";

export default (isWorkflowFilterMode: Ref<boolean>) => {
  const getItemIcon = (item: FileExplorerItem): Component => {
    if (item.isDirectory) {
      return FolderIcon;
    }
    return isWorkflowFilterMode.value ? WorkflowIcon : FileTextIcon;
  };

  return {
    getItemIcon,
  };
};
