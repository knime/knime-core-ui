import type { Ref } from "vue";

import inject from "../../../utils/inject";
import type { BackendType } from "../types";

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
  backendType,
  filterOptions,
  additionalFilterOptionsClassIdentifier,
  includeSubFolders,
}: {
  backendType: Ref<BackendType>;
  filterOptions: Ref<FilterOptions>;
  additionalFilterOptionsClassIdentifier: string;
  includeSubFolders: Ref<boolean>;
}) => {
  const getData = inject("getData") as ListItemsForPreview;

  const listItemsForPreview = (path: string | null) => {
    const options: ListItemsForPreview["arguments"]["params"]["options"] = [
      backendType.value,
      path,
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
