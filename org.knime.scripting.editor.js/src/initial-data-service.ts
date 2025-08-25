import {
  type ConnectionStatus,
  type GenericInitialData,
  type InitialDataServiceType,
  type InputConnectionInfo,
  type KAIConfig,
  type PortConfig,
  type PortConfigs,
  type PortViewConfig,
  initialDataService,
} from "./init";

export type {
  PortViewConfig,
  PortConfig,
  PortConfigs,
  ConnectionStatus,
  InputConnectionInfo,
  KAIConfig,
  GenericInitialData,
  InitialDataServiceType,
};

export const getInitialDataService = (): InitialDataServiceType =>
  initialDataService;
