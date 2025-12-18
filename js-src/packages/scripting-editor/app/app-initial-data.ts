import { type GenericInitialData, getInitialData } from "../lib/main";

import type { StaticCompletionItem } from "./static-completion-provider";

export type AppInitialData = GenericInitialData & {
  language: string;
  fileName: string;
  staticCompletionItems?: StaticCompletionItem[];
};

export const getAppInitialData = () => getInitialData() as AppInitialData;
