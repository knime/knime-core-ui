import { useRpcService } from "../../rpc";

type ValidationCacheState =
  | {
      isValid: boolean;
    }
  | {
      pendingPromise: Promise<boolean>;
    };

/**
 * Callback to update cell validity.
 * Uses stable IDs instead of indices to handle row/column deletion during async validation.
 */
export type ValidityUpdateCallback = (
  columnId: string,
  rowId: string,
  isValid: boolean,
  value: string,
) => void;

const useValidationCache = (cacheSize: number) => {
  /**
   * Per data type (outer map) and value (inner map) cache of validation results.
   */
  const cache: Map<string, Map<string, ValidationCacheState>> = new Map();
  /**
   * Eviction list ordered newest-first. Each entry is a callback that removes
   * its corresponding value from the cache. When the cache exceeds cacheSize,
   * the oldest (last) entry is popped and called.
   */
  const evictionCallbacks: (() => void)[] = [];

  const setCachedResult = (
    dataType: string,
    value: string,
    result: ValidationCacheState,
  ): void => {
    if (!cache.has(dataType)) {
      cache.set(dataType, new Map());
    }
    const isNew = !cache.get(dataType)!.has(value);
    cache.get(dataType)!.set(value, result);
    if (isNew) {
      evictionCallbacks.unshift(() => {
        cache.get(dataType)?.delete(value);
      });
      if (evictionCallbacks.length > cacheSize) {
        evictionCallbacks.pop()!();
      }
    }
  };

  const getCachedResult = (
    dataType: string,
    value: string,
  ): ValidationCacheState | null => {
    return cache.get(dataType)?.get(value) ?? null;
  };

  const startValidation = (
    dataType: string,
    value: string,
  ): ((isValid: boolean) => void) => {
    let resolvePendingPromise = (_isValid: boolean) => {};
    const pendingPromise = new Promise<boolean>((resolve) => {
      resolvePendingPromise = resolve;
    });
    setCachedResult(dataType, value, { pendingPromise });
    return resolvePendingPromise;
  };

  const finishValidation = (
    dataType: string,
    value: string,
    isValid: boolean,
    resolvePendingPromise: (isValid: boolean) => void,
  ): void => {
    setCachedResult(dataType, value, { isValid });
    resolvePendingPromise(isValid);
  };

  return {
    getCachedResult,
    startValidation,
    finishValidation,
  };
};

/**
 * Composable for managing cell validation with caching.
 * Caches validation results by dataType + value to avoid redundant backend calls.
 */
export const useValidationBackend = ({
  onValidityUpdate,
  cacheSize = 100,
}: {
  onValidityUpdate: ValidityUpdateCallback;
  cacheSize?: number;
}) => {
  const rpcService = useRpcService();

  const { getCachedResult, startValidation, finishValidation } =
    useValidationCache(cacheSize);

  const resolveFromCacheIsSuccessful = (
    dataType: string,
    value: string,
    columnId: string,
    rowId: string,
  ): boolean => {
    const cached = getCachedResult(dataType, value);
    if (cached) {
      if ("pendingPromise" in cached) {
        cached.pendingPromise.then((isValid) => {
          onValidityUpdate(columnId, rowId, isValid, value);
        });
      } else {
        onValidityUpdate(columnId, rowId, cached.isValid, value);
      }
      return true;
    }
    return false; // No cache, need to validate
  };

  /**
   * Validates a cell value asynchronously.
   * Updates via callback when the result is available (cached or from backend).
   */
  const validateCell = (
    dataType: string,
    value: string,
    columnId: string,
    rowId: string,
  ): void => {
    if (resolveFromCacheIsSuccessful(dataType, value, columnId, rowId)) {
      return;
    }
    const resolvePendingPromise = startValidation(dataType, value);
    rpcService.validateCellFromStringValue(dataType, value).then((isValid) => {
      finishValidation(dataType, value, isValid, resolvePendingPromise);
      onValidityUpdate(columnId, rowId, isValid, value);
    });
  };

  /**
   * To avoid validating parts of a column which are already cached, we can resolve the cached values beforehand and only validate the remaining ones.
   *
   * @returns An object containing the values to validate and their corresponding row IDs. Both arrays are aligned, i.e. the value at index i corresponds to the row ID at index i.
   */
  const resolveCached = (
    columnId: string,
    dataType: string,
    values: (string | null)[],
    rowIds: string[],
  ) => {
    const valuesToValidate: string[] = [];
    const correspondingRowIds: string[] = [];
    values.forEach((value, index) => {
      if (
        value !== null &&
        !resolveFromCacheIsSuccessful(dataType, value, columnId, rowIds[index])
      ) {
        valuesToValidate.push(value);
        correspondingRowIds.push(rowIds[index]);
      }
    });
    return { valuesToValidate, correspondingRowIds };
  };

  const validateColumn = (
    dataType: string,
    values: (string | null)[],
    columnId: string,
    rowIds: string[],
  ) => {
    const { valuesToValidate, correspondingRowIds } = resolveCached(
      columnId,
      dataType,
      values,
      rowIds,
    );

    if (valuesToValidate.length === 0) {
      return; // All values were cached, no need to call the backend
    }
    const resolvePendingPromises = valuesToValidate.map((value) =>
      startValidation(dataType, value),
    );
    rpcService
      .validateCellsFromStringValues(dataType, valuesToValidate)
      .then((results) => {
        results.forEach((isValid, index) => {
          const value = valuesToValidate[index];
          const rowId = correspondingRowIds[index];
          const resolvePendingPromise = resolvePendingPromises[index];
          finishValidation(dataType, value, isValid, resolvePendingPromise);
          onValidityUpdate(columnId, rowId, isValid, value);
        });
      });
  };

  const validateArea = (
    params: {
      dataType: string;
      values: (string | null)[];
      columnId: string;
      rowIds: string[];
    }[],
  ) => {
    params.forEach(({ dataType, values, columnId, rowIds }) =>
      validateColumn(dataType, values, columnId, rowIds),
    );
  };

  return {
    /**
     * Triggers an asynchronous validation of the given cell value. The result will be returned via the provided callback when available.
     */
    validateCell,
    /**
     * Triggers an asynchronous validation of multiple cell values within the same column.
     * The results will be returned via the provided callback when available.
     * Use this method to validate multiple values within the same column at once.
     * This can also be done for only parts of the column
     * (when multiple new values appear in the same column, e.g. after pasting))
     */
    validateColumn,
    /**
     * Convenience method to validate multiple columns at once, e.g. after pasting.
     * Currently not optimized so this will perform one backend call per column.
     */
    validateArea,
  };
};
