import { type Ref, computed, ref, watch } from "vue";

import type { DBItemType } from "./useDbTableChooserBackend";

type UseMessagesReturnType = {
  catalogueErrorMessage: Readonly<Ref<string | null>>;
  catalogueErrorMessageIsLoading: Readonly<Ref<boolean>>;
  schemaErrorMessage: Readonly<Ref<string | null>>;
  schemaErrorMessageIsLoading: Readonly<Ref<boolean>>;
  tableErrorMessage: Readonly<Ref<string | null>>;
  tableErrorMessageIsLoading: Readonly<Ref<boolean>>;
};

const DB_NOT_CONNECTED_ERROR =
  "No database connection. Connect to a database first.";

const DEBOUNCE_TIMEOUT = 200; // milliseconds

type MaybeAsyncCallback<T = any> = (...args: T[]) => void | Promise<void>;

const debouncedWatch = (
  triggers: Ref<any>[],
  callback: MaybeAsyncCallback,
  debounceTimeout: number = DEBOUNCE_TIMEOUT,
) => {
  let timeout: ReturnType<typeof setTimeout>;
  const running = ref(false);
  const watcher = () => {
    running.value = true;
    clearTimeout(timeout);
    timeout = setTimeout(async () => {
      try {
        await Promise.resolve(callback());
      } finally {
        running.value = false;
      }
    }, debounceTimeout);
  };

  const unwatch = watch(triggers, watcher);

  return { unwatch, running };
};

export default ({
  itemTypeExtractor,
  cataloguesSupported,
  catalogueName,
  schemaName,
  tableName,
  isDbConnected,
  validateSchema,
  validateTable,
  enable,
}: {
  itemTypeExtractor: (path: string[]) => Promise<DBItemType | null>;
  cataloguesSupported: Readonly<Ref<boolean>>;
  isDbConnected: Readonly<Ref<boolean>>;
  catalogueName: Readonly<Ref<string>>;
  schemaName: Readonly<Ref<string | null>>;
  tableName: Readonly<Ref<string>>;
  validateSchema: Readonly<Ref<boolean>>;
  validateTable: Readonly<Ref<boolean>>;
  enable: Readonly<Ref<boolean>>;
}): UseMessagesReturnType => {
  const catalogueErrorMessage = ref<string | null>(null);

  const { running: catalogueErrorMessageIsLoading } = debouncedWatch(
    [cataloguesSupported, enable, catalogueName],
    async () => {
      catalogueErrorMessage.value = null;

      const pathUpToCatalogue = [catalogueName.value ?? ""];
      if (
        !cataloguesSupported.value ||
        !enable.value ||
        schemaName.value === null
      ) {
        catalogueErrorMessage.value = null;
      } else if (!isDbConnected.value) {
        catalogueErrorMessage.value = DB_NOT_CONNECTED_ERROR;
      } else if (!catalogueName.value) {
        catalogueErrorMessage.value = "Please select a database.";
      } else if ((await itemTypeExtractor(pathUpToCatalogue)) === null) {
        catalogueErrorMessage.value = "Database does not exist.";
      } else {
        catalogueErrorMessage.value = null;
      }
    },
  );

  const schemaErrorMessage = ref<string | null>(null);
  const { running: schemaErrorMessageIsLoading } = debouncedWatch(
    [
      cataloguesSupported,
      enable,
      catalogueName,
      schemaName,
      catalogueErrorMessage,
      catalogueErrorMessageIsLoading,
    ],
    async () => {
      schemaErrorMessage.value = null;

      const pathUpToSchema = [
        ...(cataloguesSupported.value ? [catalogueName.value] : []),
        schemaName.value ?? "",
      ];

      if (
        !enable.value ||
        catalogueErrorMessageIsLoading.value ||
        !validateSchema.value ||
        schemaName.value === null
      ) {
        schemaErrorMessage.value = null;
      } else if (!isDbConnected.value) {
        schemaErrorMessage.value = DB_NOT_CONNECTED_ERROR;
      } else if (/^\s*$/.test(schemaName.value)) {
        schemaErrorMessage.value = "Schema name is required.";
      } else if (catalogueErrorMessage.value) {
        schemaErrorMessage.value =
          "There are errors in the database selection.";
      } else if ((await itemTypeExtractor(pathUpToSchema)) === null) {
        schemaErrorMessage.value = "Schema does not exist in database.";
      } else {
        schemaErrorMessage.value = null;
      }
    },
  );

  const tableErrorMessage = ref<string | null>(null);

  const { running: tableErrorMessageIsLoading } = debouncedWatch(
    [
      cataloguesSupported,
      enable,
      catalogueName,
      schemaName,
      tableName,
      schemaErrorMessage,
      catalogueErrorMessage,
      schemaErrorMessageIsLoading,
      catalogueErrorMessageIsLoading,
    ],
    async () => {
      tableErrorMessage.value = null;

      const pathUpToTable = [
        ...(cataloguesSupported.value ? [catalogueName.value ?? ""] : []),
        schemaName.value ?? "",
        tableName.value ?? "",
      ];

      if (!validateTable.value || tableName.value === null || !enable.value) {
        tableErrorMessage.value = null;
      } else if (
        !enable.value ||
        schemaErrorMessageIsLoading.value ||
        catalogueErrorMessageIsLoading.value
      ) {
        tableErrorMessage.value = null;
      } else if (!isDbConnected.value) {
        tableErrorMessage.value = DB_NOT_CONNECTED_ERROR;
      } else if (/^\s*$/.test(tableName.value)) {
        tableErrorMessage.value = "Table name is required.";
      } else if (catalogueErrorMessage.value) {
        tableErrorMessage.value = "There are errors in the database selection.";
      } else if (schemaErrorMessage.value) {
        tableErrorMessage.value = "There are errors in the schema selection.";
      } else if ((await itemTypeExtractor(pathUpToTable)) === null) {
        tableErrorMessage.value = "Table does not exist in schema.";
      } else {
        tableErrorMessage.value = null;
      }
    },
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
