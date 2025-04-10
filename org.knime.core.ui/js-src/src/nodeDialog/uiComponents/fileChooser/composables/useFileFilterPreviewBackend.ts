import type { Ref } from "vue";

import inject from "../../../utils/inject";
import type { BackendType } from "../types";
import type { FSCategory } from "../types/FileChooserProps";

export type FilterOptions = {
  [key: string]: any;
};

type PreviewItemsConfig = {
  /**
   * Additional filters
   */
  additionalFilterOptions: FilterOptions;
  /**
   * The class id for the type of the additional filter options
   */
  additionalFilterOptionsClassIdentifier: string;
};

export type PreviewResult =
  | {
      itemsAfterFiltering: string[];
      numItemsBeforeFiltering: number;
      numFilesBeforeFilteringIsOnlyLowerBound: boolean;
      numFilesAfterFilteringIsOnlyLowerBound: boolean;
      resultType: "SUCCESS";
    }
  | {
      resultType: "ERROR";
      errorMessage: string;
    };

type ListItemsForPreview = (params: {
  method: "fileFilterPreview.listItemsForPreview";
  options: [
    /**
     * The id of the used file system.
     */
    backendType: BackendType,
    /**
     * The current path or null to reference the root level
     */
    path: string | null,
    /**
     * The extensions by which files listed in a folder are filtered. If empty, no filters will be applied.
     */
    extensions: string[],
    isWriter: boolean,
    /**
     * Whether to include items in subfolders recursively in the preview
     */
    includeSubFolders: boolean,
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
  additionalFilterOptionsClassIdentifier,
  includeSubFolders,
}: {
  isWriter: Ref<boolean>;
  backendType: Ref<BackendType>;
  filteredExtensions: Ref<string[]>;
  filterOptions: Ref<FilterOptions>;
  additionalFilterOptionsClassIdentifier: string;
  includeSubFolders: Ref<boolean>;
}) => {
  const getData = inject("getData") as ListItemsForPreview;

  const listItemsForPreview = (path: string | null) => {
    const options: ListItemsForPreview["arguments"]["params"]["options"] = [
      backendType.value,
      path,
      filteredExtensions.value,
      isWriter.value,
      includeSubFolders.value,
      {
        additionalFilterOptions: filterOptions.value,
        additionalFilterOptionsClassIdentifier,
      } satisfies PreviewItemsConfig,
    ];

    return getData({
      method: "fileFilterPreview.listItemsForPreview",
      options,
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
