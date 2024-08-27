import { getSettingsHelper } from "@/settings-helper";
import type { InputOutputModel } from "@/components/InputOutputItem.vue";

export type PortViewConfig = {
  label: string;
  portViewIdx: number;
};

export type PortConfig = {
  /**
   * null if no node is connected to an input port
   */
  nodeId: string | null;
  portIdx: number;
  portViewConfigs: PortViewConfig[];
  portName: string;
};

export type PortConfigs = {
  inputPorts: PortConfig[];
};

export type KAIConfig = {
  codeAssistantEnabled: boolean;
  codeAssistantInstalled: boolean;
  hubId: string;
  loggedIn: boolean;
};

export type GenericInitialData = {
  inputPortConfigs: PortConfigs;
  inputObjects: InputOutputModel[];
  flowVariables: InputOutputModel;
  inputsAvailable: boolean;
  outputObjects?: InputOutputModel[];
  kAiConfig: KAIConfig;
  // [key: string]: any;
};

const loadDataPromise = getSettingsHelper()
  .getInitialDataAndSettings()
  .then((data): GenericInitialData => data.initialData);

const initialDataService = {
  getInitialData: () => loadDataPromise,
};
export type InitialDataServiceType = typeof initialDataService;

export const getInitialDataService = (): InitialDataServiceType =>
  initialDataService;
