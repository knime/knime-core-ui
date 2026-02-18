import type { JsonDataService } from "@knime/ui-extension-service";

/**
 * Type definition for the Table Creator RPC methods available on the backend.
 * Each method here corresponds to a Java RPC method.
 */
export interface TableCreatorRpcMethods {
  /**
   * Validates whether the given string value can be converted to a cell of the given data type.
   * @param dataType - The target data type identifier (e.g., "org.knime.core.data.def.StringCell")
   * @param stringValue - The string value to validate
   * @returns true if the value is valid for the given type
   */
  validateCellFromStringValue(
    dataType: string,
    stringValue: string,
  ): Promise<boolean>;

  /**
   * Validates whether each of the given string values can be converted to a cell of the given data type.
   * @param dataType - The target data type identifier
   * @param stringValues - The string values to validate
   * @returns an array of booleans, where true indicates a valid value
   */
  validateCellsFromStringValues(
    dataType: string,
    stringValues: string[],
  ): Promise<boolean[]>;
}

/**
 * Creates a type-safe RPC service proxy that automatically converts method calls
 * to JsonDataService.data() requests.
 *
 * Usage:
 * ```ts
 * const rpcService = createTableCreatorRpcService(jsonDataService);
 * const isValid = await rpcService.validateCellFromStringValue(dataType, value);
 * ```
 *
 * Each call to `rpcService.methodName(arg1, arg2)` is translated to:
 * ```ts
 * jsonDataService.data({ method: "methodName", options: [arg1, arg2] })
 * ```
 */
export const createTableCreatorRpcService = (
  jsonDataService: JsonDataService,
): TableCreatorRpcMethods => {
  return new Proxy({} as TableCreatorRpcMethods, {
    get(_target, methodName: string) {
      return (...args: unknown[]) =>
        jsonDataService.data({
          method: methodName,
          // eslint-disable-next-line no-undefined
          options: args.length > 0 ? args : undefined,
        });
    },
  });
};
