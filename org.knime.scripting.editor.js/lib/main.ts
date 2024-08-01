import CompactTabBar from "@/components/CompactTabBar.vue";
import OutputConsole, {
  type ConsoleHandler,
  type ConsoleText,
} from "@/components/OutputConsole.vue";
import ScriptingEditor from "@/components/ScriptingEditor.vue";
import { type SettingsMenuItem } from "@/components/SettingsPage.vue";
import useShouldFocusBePainted from "@/components/utils/shouldFocusBePainted";
import {
  MIN_WIDTH_FOR_DISPLAYING_PANES,
  MIN_WIDTH_FOR_DISPLAYING_LEFT_PANE,
} from "@/components/utils/paneSizes";
import {
  type InputOutputModel,
  COLUMN_INSERTION_EVENT,
} from "@/components/InputOutputItem.vue";
import type {
  UseCodeEditorParams,
  UseCodeEditorReturn,
  UseDiffEditorParams,
  UseDiffEditorReturn,
} from "@/editor";
import editor from "@/editor";
import { useReadonlyStore } from "@/store/readOnly";

import { consoleHandler } from "@/consoleHandler";
import {
  getScriptingService,
  type ScriptingServiceType,
} from "@/scripting-service";
import { setActiveEditorStoreForAi } from "@/store/ai-bar";
import {
  insertionEventHelper,
  type InsertionEvent,
} from "@/components/utils/insertionEventHelper";
import OutputTablePreview from "@/components/OutputTablePreview.vue";
import {
  getInitialDataService,
  type InitialDataServiceType,
  type GenericInitialData,
  type KAIConfig,
  type PortConfigs,
} from "@/initial-data-service";
import {
  type GenericNodeSettings,
  getSettingsService,
} from "@/settings-service";

export {
  COLUMN_INSERTION_EVENT,
  CompactTabBar,
  consoleHandler,
  editor,
  getInitialDataService,
  getScriptingService,
  getSettingsService,
  insertionEventHelper,
  MIN_WIDTH_FOR_DISPLAYING_LEFT_PANE,
  MIN_WIDTH_FOR_DISPLAYING_PANES,
  OutputConsole,
  OutputTablePreview,
  ScriptingEditor,
  setActiveEditorStoreForAi,
  useShouldFocusBePainted,
  useReadonlyStore,
};
export type {
  ConsoleHandler,
  ConsoleText,
  GenericInitialData,
  GenericNodeSettings,
  InitialDataServiceType,
  InputOutputModel,
  InsertionEvent,
  KAIConfig,
  PortConfigs,
  ScriptingServiceType,
  SettingsMenuItem,
  UseCodeEditorParams,
  UseCodeEditorReturn,
  UseDiffEditorParams,
  UseDiffEditorReturn,
};
