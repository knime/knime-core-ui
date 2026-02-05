import { ref, type Ref } from "vue";
import type { TableCreatorRpcMethods } from "../rpc/TableCreatorRpcService";

/**
 * Cache key combining data type and value.
 */
type CacheKey = `${string}:${string}`;

/**
 * Creates a cache key from data type and value.
 */
const createCacheKey = (dataType: string, value: string): CacheKey =>
  `${dataType}:${value}`;

/**
 * Validation result that can be cached.
 */
type ValidationResult = {
  isValid: boolean;
  /** Whether validation is still pending */
  isPending: boolean;
};

/**
 * Callback to update cell validity.
 * Uses stable IDs instead of indices to handle row/column deletion during async validation.
 */
export type ValidityUpdateCallback = (
  columnId: string,
  rowId: string,
  isValid: boolean,
) => void;

/**
 * Configuration for the validation cache.
 */
export interface ValidationCacheConfig {
  /** RPC service for backend validation */
  rpcService: TableCreatorRpcMethods | null;
  /** Callback to update cell validity in the data model */
  onValidityUpdate: ValidityUpdateCallback;
}

/**
 * Composable for managing cell validation with caching.
 * Caches validation results by dataType + value to avoid redundant backend calls.
 */
export const useValidationCache = (
  config: Ref<ValidationCacheConfig>,
) => {
  // Cache: Map<dataType, Map<value, ValidationResult>>
  const cache = ref<Map<string, Map<string, ValidationResult>>>(new Map());

  // Pending validations to debounce rapid changes
  const pendingValidations = ref<Map<CacheKey, AbortController>>(new Map());

  /**
   * Gets a cached validation result if available.
   */
  const getCachedResult = (
    dataType: string,
    value: string,
  ): ValidationResult | undefined => {
    return cache.value.get(dataType)?.get(value);
  };

  /**
   * Sets a cached validation result.
   */
  const setCachedResult = (
    dataType: string,
    value: string,
    result: ValidationResult,
  ): void => {
    if (!cache.value.has(dataType)) {
      cache.value.set(dataType, new Map());
    }
    cache.value.get(dataType)!.set(value, result);
  };

  /**
   * Validates a cell value asynchronously.
   * Returns immediately with optimistic result, then updates via callback when backend responds.
   *
   * @param dataType - The data type to validate against
   * @param value - The string value to validate
   * @param columnId - Stable column ID for callback
   * @param rowId - Stable row ID for callback
   * @returns The immediate (possibly cached or optimistic) validity
   */
  const validateCell = (
    dataType: string,
    value: string,
    columnId: string,
    rowId: string,
  ): boolean => {
    // Empty/null values are always valid (missing values are allowed)
    if (value === null || value === undefined || value === "") {
      config.value.onValidityUpdate(columnId, rowId, true);
      return true;
    }

    // Check cache first
    const cached = getCachedResult(dataType, value);
    if (cached && !cached.isPending) {
      // We have a definitive cached result
      config.value.onValidityUpdate(columnId, rowId, cached.isValid);
      return cached.isValid;
    }

    // No RPC service available - assume valid
    if (!config.value.rpcService) {
      return true;
    }

    const cacheKey = createCacheKey(dataType, value);

    // Cancel any pending validation for this cell
    const existingController = pendingValidations.value.get(cacheKey);
    if (existingController) {
      existingController.abort();
    }

    // Create new abort controller for this validation
    const abortController = new AbortController();
    pendingValidations.value.set(cacheKey, abortController);

    // Mark as pending in cache
    setCachedResult(dataType, value, { isValid: true, isPending: true });

    // Start async validation
    config.value.rpcService
      .validateCellFromStringValue(dataType, value)
      .then((isValid) => {
        // Check if this validation was aborted
        if (abortController.signal.aborted) {
          return;
        }

        // Update cache with final result
        setCachedResult(dataType, value, { isValid, isPending: false });

        // Update the cell via callback (using stable IDs)
        config.value.onValidityUpdate(columnId, rowId, isValid);

        // Clean up pending map
        pendingValidations.value.delete(cacheKey);
      })
      .catch((error) => {
        // On error, assume valid (don't block user)
        if (!abortController.signal.aborted) {
          console.warn("Validation error:", error);
          setCachedResult(dataType, value, { isValid: true, isPending: false });
          config.value.onValidityUpdate(columnId, rowId, true);
          pendingValidations.value.delete(cacheKey);
        }
      });

    // Return optimistic result (true = valid) for immediate UI feedback
    return true;
  };

  /**
   * Checks if a value is valid based on cached results only.
   * Does not trigger a new validation.
   */
  const isCachedValid = (dataType: string, value: string): boolean | null => {
    if (value === null || value === undefined || value === "") {
      return true;
    }
    const cached = getCachedResult(dataType, value);
    if (cached && !cached.isPending) {
      return cached.isValid;
    }
    return null; // Unknown
  };

  /**
   * Clears the entire validation cache.
   */
  const clearCache = (): void => {
    cache.value.clear();
    // Abort all pending validations
    pendingValidations.value.forEach((controller) => controller.abort());
    pendingValidations.value.clear();
  };

  return {
    validateCell,
    isCachedValid,
    clearCache,
  };
};
