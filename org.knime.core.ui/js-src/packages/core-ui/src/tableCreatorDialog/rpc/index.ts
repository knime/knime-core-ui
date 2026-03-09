import { inject } from "vue";

import {
  JsonDataService,
  type UIExtensionService,
} from "@knime/ui-extension-service";

import { createTableCreatorRpcService } from "./TableCreatorRpcService";

export { type TableCreatorRpcMethods } from "./TableCreatorRpcService";

export const useRpcService = () => {
  const getKnimeService = inject<() => UIExtensionService>("getKnimeService")!;
  const jsonDataService = new JsonDataService(getKnimeService());
  return createTableCreatorRpcService(jsonDataService);
};
