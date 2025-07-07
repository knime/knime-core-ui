import { type Ref, computed, ref } from "vue";
import { computedAsync } from "@vueuse/core";

import type { DBItemType } from "./useDbTableChooserBackend";

type ReturnType = {
  catalogueErrorMessage: Readonly<Ref<string | null>>;
  catalogueErrorMessageIsLoading: Readonly<Ref<boolean>>;
  schemaErrorMessage: Readonly<Ref<string | null>>;
  schemaErrorMessageIsLoading: Readonly<Ref<boolean>>;
  tableErrorMessage: Readonly<Ref<string | null>>;
  tableErrorMessageIsLoading: Readonly<Ref<boolean>>;
};

const DB_NOT_CONNECTED_ERROR = "Database is not connected.";

export default ({
  itemTypeExtractor,
  cataloguesSupported,
  catalogueName,
  schemaName,
  tableName,
  isDbConnected,
  validateSchema,
  validateTable,
}: {
  itemTypeExtractor: (path: string[]) => Promise<DBItemType | null>;
  cataloguesSupported: Readonly<Ref<boolean>>;
  isDbConnected: Readonly<Ref<boolean>>;
  catalogueName: Readonly<Ref<string>>;
  schemaName: Readonly<Ref<string>>;
  tableName: Readonly<Ref<string>>;
  validateSchema: Readonly<Ref<boolean>>;
  validateTable: Readonly<Ref<boolean>>;
}): ReturnType => {
  const catalogueErrorMessageIsLoading = ref(false);
  const catalogueErrorMessage = computedAsync(
    async () => {
      if (!cataloguesSupported.value) {
        return null;
      } else if (!isDbConnected.value) {
        return DB_NOT_CONNECTED_ERROR;
      }

      const pathUpToCatalogue = [catalogueName.value ?? ""];

      if (!catalogueName.value) {
        return "Please select a database.";
      } else if ((await itemTypeExtractor(pathUpToCatalogue)) === null) {
        return "Database does not exist.";
      } else {
        return null;
      }
    },
    null,
    catalogueErrorMessageIsLoading,
  );

  const schemaErrorMessageIsLoading = ref(false);
  const schemaErrorMessage = computedAsync(
    async () => {
      if (!isDbConnected.value) {
        return DB_NOT_CONNECTED_ERROR;
      }

      const pathUpToSchema = [
        ...(cataloguesSupported.value ? [catalogueName.value] : []),
        schemaName.value ?? "",
      ];

      // schema can be blank, so we only check if it exists
      if (catalogueErrorMessage.value) {
        return "There are errors in the database selection.";
      } else if (
        validateSchema.value &&
        (await itemTypeExtractor(pathUpToSchema)) === null
      ) {
        return "Schema does not exist in database.";
      } else {
        return null;
      }
    },
    null,
    schemaErrorMessageIsLoading,
  );

  const tableErrorMessageIsLoading = ref(false);
  const tableErrorMessage = computedAsync(
    async () => {
      if (!isDbConnected.value) {
        return DB_NOT_CONNECTED_ERROR;
      }

      const pathUpToTable = [
        ...(cataloguesSupported.value ? [catalogueName.value ?? ""] : []),
        schemaName.value ?? "",
        tableName.value ?? "",
      ];

      if (schemaErrorMessage.value) {
        return "There are errors in the schema selection.";
      } else if (
        validateTable.value &&
        (await itemTypeExtractor(pathUpToTable)) === null
      ) {
        return "Table does not exist in schema.";
      } else {
        return null;
      }
    },
    null,
    tableErrorMessageIsLoading,
  );

  return {
    catalogueErrorMessage,
    catalogueErrorMessageIsLoading,
    schemaErrorMessage,
    schemaErrorMessageIsLoading: computed(
      () =>
        schemaErrorMessageIsLoading.value ||
        catalogueErrorMessageIsLoading.value,
    ),
    tableErrorMessage,
    tableErrorMessageIsLoading: computed(
      () =>
        tableErrorMessageIsLoading.value ||
        schemaErrorMessageIsLoading.value ||
        catalogueErrorMessageIsLoading.value,
    ),
  };
};
