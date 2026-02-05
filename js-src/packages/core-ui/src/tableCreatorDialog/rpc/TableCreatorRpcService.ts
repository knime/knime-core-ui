import type { JsonDataService } from "@knime/ui-extension-service";

/**
 * Type definition for the Table Creator RPC methods available on the backend.
 * Each method here corresponds to a Java RPC method.
 */
export interface TableCreatorRpcMethods {
  // Add RPC method signatures here as they are implemented on the backend
  // Example:
  // getColumnTypes(): Promise<Array<{ id: string; text: string }>>;
}

/**
 * Creates a type-safe RPC service proxy that automatically converts method calls
 * to JsonDataService.data() requests.
 *
 * Usage:
 * ```ts
 * const rpcService = createTableCreatorRpcService(jsonDataService);
 * const types = await rpcService.getColumnTypes();
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
          options: args.length > 0 ? args : undefined,
        });
    },
  });
};
