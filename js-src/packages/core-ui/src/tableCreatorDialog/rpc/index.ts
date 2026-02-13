import {
  JsonDataService,
  type UIExtensionService,
} from "@knime/ui-extension-service";
import { inject } from "vue";

import { createTableCreatorRpcService } from "./tableCreatorRpcService";

export { type TableCreatorRpcMethods } from "./tableCreatorRpcService";

export const useRpcService = () => {
  const getKnimeService = inject<() => UIExtensionService>("getKnimeService")!;
  const jsonDataService = new JsonDataService(getKnimeService());
  return createTableCreatorRpcService(jsonDataService);
};
