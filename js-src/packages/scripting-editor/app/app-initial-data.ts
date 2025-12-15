import { type GenericInitialData, getInitialData } from "../lib/main";

export type AppInitialData = GenericInitialData & {
  language: string;
  fileName: string;
};

export const getAppInitialData = () => getInitialData() as AppInitialData;
