import type { Ref } from "vue";

import inject from "../../../utils/inject";
import type { BackendType, FolderAndError, PathAndError } from "../types";
import { FSCategory } from "../types/FileChooserProps";

interface ListItemsConfig {
  /**
   * the endings with respect to which the files are filtered. If empty or null, no filters
   * will be applied.
   */
  extensions: string[] | null;
  /**
   * Setting this will impact whether non-readable or non-writable files are not displayed
   */
  isWriter: boolean;
  /**
   * If this is set to true, workflow-aware file systems will only show workflow-like entities and others will
   * show files ending with ".knwf". Note that the extensions are ignored in this mode.
   */
  isWorkflowFilterMode: boolean;
}

export type FileChooserListItems = (params: {
  method: "fileChooser.listItems";
  options: [
    /**
     * The id of the used file system.
     */
    BackendType,
    /**
     * The current path or null to reference the root level
     */
    string | null,
    /**
     * The name of the to be accessed folder relative to the path or ".." if the parent folder
     * should be accessed. Set to null in order to access the path directly.
     */
    string | null,
    /**
     *  additional configuration for the filters applied to the listed files
     */
    ListItemsConfig,
    /**
     * The path to which the file path is relative. If null, the absolute path is returned.
     */
    string | null,
  ];
}) => Promise<FolderAndError | undefined>;

export type FileChooserGetFilePath = (params: {
  method: "fileChooser.getFilePath";
  options: [
    /**
     * The id of the used file system.
     */
    BackendType,
    /**
     * The path of the folder containing the file.
     */
    string | null,
    /**
     * The name of the to be accessed file relative to the path.
     */
    string,
    /**
     * A file extension that is added to the filename whenever it does not already exist or end with the extension.
     */
    string | null,
    /**
     * The path to which the file path is relative. If null, the absolute path is returned.
     */
    string | null,
  ];
}) => Promise<PathAndError>;

export type FileChooserResolveRelativePath = (params: {
  method: "fileChooser.resolveRelativePath";
  options: [
    /**
     * The id of the used file system.
     */
    BackendType,
    /**
     * The relative path to resolve.
     */
    string,
    /**
     * The base path to resolve against.
     */
    string,
  ];
}) => Promise<string>;

export default ({
  filteredExtensions,
  appendedExtension,
  isWriter,
  isWorkflowFilterMode,
  backendType,
}: {
  /**
   * The extensions by which files listed in a folder are filtered
   */
  filteredExtensions: Ref<string[]>;
  /**
   * The extension to append when selecting a file.
   * Only appended if the file does not already exist or end with the extension.
   */
  appendedExtension: Ref<string | null>;
  isWriter: Ref<boolean>;
  isWorkflowFilterMode?: Ref<boolean>;
  backendType: Ref<BackendType>;
}) => {
  const getData = inject("getData") as FileChooserGetFilePath &
    FileChooserListItems &
    FileChooserResolveRelativePath;

  const listItems = (
    path: string | null,
    nextFolder: string | null,
    relativeTo: string | null,
  ) => {
    return getData({
      method: "fileChooser.listItems",
      options: [
        backendType.value,
        path,
        nextFolder,
        {
          extensions: filteredExtensions.value,
          isWriter: isWriter.value,
          isWorkflowFilterMode: isWorkflowFilterMode?.value ?? false,
        },
        relativeTo,
      ],
    });
  };
  const getFilePath = (
    path: string | null,
    fileName: string,
    relativeTo: string | null,
  ) => {
    return getData({
      method: "fileChooser.getFilePath",
      options: [
        backendType.value,
        path,
        fileName,
        appendedExtension?.value,
        relativeTo,
      ],
    });
  };
  const resolveRelativePath = (path: string, relativeTo: string) => {
    return getData({
      method: "fileChooser.resolveRelativePath",
      options: [backendType.value, path, relativeTo],
    });
  };

  return {
    listItems,
    getFilePath,
    resolveRelativePath,
  };
};

export const getBackendType = (
  fsCategory: keyof typeof FSCategory,
  portIndex?: number,
): BackendType => {
  if (fsCategory === "LOCAL") {
    return "local";
  }
  if (fsCategory === "CONNECTED") {
    return `connected${portIndex ?? -1}`;
  }
  if (fsCategory === "relative-to-embedded-data") {
    return "embedded";
  }
  return "relativeToCurrentHubSpace";
};

/**
 * Combined File Chooser API type
 */
export type FileChooserRpcMethods = FileChooserListItems &
  FileChooserGetFilePath &
  FileChooserResolveRelativePath;
