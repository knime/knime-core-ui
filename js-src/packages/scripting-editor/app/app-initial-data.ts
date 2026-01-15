import {
  type GenericInitialData,
  type StaticCompletionItem,
  getInitialData,
} from "../lib/main";

export type AppInitialData = GenericInitialData & {
  language: string;
  fileName: string;
  staticCompletionItems?: StaticCompletionItem[];
};

export const getAppInitialData = () => getInitialData() as AppInitialData;
