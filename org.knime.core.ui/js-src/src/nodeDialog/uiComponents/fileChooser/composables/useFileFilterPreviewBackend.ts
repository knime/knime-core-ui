import type { Ref } from "vue";

import inject from "../../../utils/inject";
import type { BackendType } from "../types";
import type { FSCategory } from "../types/FileChooserProps";

type PreviewItemsConfig = {
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
   * Additional filters
   */
  additionalFilterOptions: FilterOptions;
  /**
   * The class id for the type of the additional filter options
   */
  additionalFilterOptionsClassIdentifier: string;
};

export type FilterOptions = {
  fileFormat: "all" | "custom";
  fileExtensions: string[];
  filenamePatternType: "wildcard" | "regex" | "all_pass";
  filenamePattern: string;
  filenamePatternCaseSensitive: boolean;
  includeHiddenFiles: boolean;
  folderNamePatternType: "wildcard" | "regex" | "all_pass";
  folderNamePattern: string;
  folderNamePatternCaseSensitive: boolean;
  includeHiddenFolders: boolean;
  followLinks: boolean;
  includeSubFolders: boolean;
};

export type PreviewResult = {
  items: string[];
  totalItemsBeforeFiltering: number;
  isTotalItemsOnlyLowerBound: boolean;
};

type ListItemsForPreview = (params: {
  method: "fileFilterPreview.listItemsForPreview";
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
     *  additional configuration for the filters applied to the listed files
     */
    PreviewItemsConfig,
  ];
}) => Promise<PreviewResult>;

export default ({
  filteredExtensions,
  isWriter,
  backendType,
  filterOptions,
}: {
  isWriter: Ref<boolean>;
  backendType: Ref<BackendType>;
  /**
   * The extensions by which files listed in a folder are filtered
   */
  filteredExtensions: Ref<string[]>;
  filterOptions: Ref<FilterOptions>;
}) => {
  const getData = inject("getData") as ListItemsForPreview;

  const listItemsForPreview = (path: string | null) => {
    console.log("Calling backend with path", path);
    console.log("And with additional options", filterOptions.value);
    return getData({
      method: "fileFilterPreview.listItemsForPreview",
      options: [
        backendType.value,
        path,
        {
          extensions: filteredExtensions.value,
          isWriter: isWriter.value,
          additionalFilterOptions: filterOptions.value,
          additionalFilterOptionsClassIdentifier: "org.knime.core.webui.node.dialog.defaultdialog.dataservice.filechooser.FileChooserFilters$TestFileFilter",
        },
      ],
    });
  };

  return {
    listItemsForPreview,
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
