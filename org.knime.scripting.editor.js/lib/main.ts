import CompactTabBar from "@/components/CompactTabBar.vue";
import {
  COLUMN_INSERTION_EVENT,
  type InputOutputModel,
  type SubItem,
} from "@/components/InputOutputItem.vue";
import InputOutputPane from "@/components/InputOutputPane.vue";
import OutputConsole, {
  type ConsoleHandler,
  type ConsoleText,
} from "@/components/OutputConsole.vue";
import OutputTablePreview from "@/components/OutputTablePreview.vue";
import ScriptingEditor from "@/components/ScriptingEditor.vue";
import { type SettingsMenuItem } from "@/components/SettingsPage.vue";
import {
  type InsertionEvent,
  insertionEventHelper,
} from "@/components/utils/insertionEventHelper";
import {
  MIN_WIDTH_FOR_DISPLAYING_LEFT_PANE,
  MIN_WIDTH_FOR_DISPLAYING_PANES,
} from "@/components/utils/paneSizes";
import useShouldFocusBePainted from "@/components/utils/shouldFocusBePainted";
import { consoleHandler, setConsoleHandler } from "@/consoleHandler";
import type {
  UseCodeEditorParams,
  UseCodeEditorReturn,
  UseDiffEditorParams,
  UseDiffEditorReturn,
} from "@/editor";
import editor from "@/editor";
import {
  type GenericInitialData,
  type InitialDataServiceType,
  type InputConnectionInfo,
  type KAIConfig,
  type PortConfigs,
  getInitialDataService,
} from "@/initial-data-service";
import {
  type ScriptingServiceType,
  getScriptingService,
  initConsoleEventHandler,
} from "@/scripting-service";
import {
  type GenericNodeSettings,
  getSettingsService,
} from "@/settings-service";
import { setActiveEditorStoreForAi } from "@/store/ai-bar";
import { useReadonlyStore } from "@/store/readOnly";

export {
  COLUMN_INSERTION_EVENT,
  CompactTabBar,
  consoleHandler,
  editor,
  initConsoleEventHandler,
  getInitialDataService,
  getScriptingService,
  getSettingsService,
  insertionEventHelper,
  MIN_WIDTH_FOR_DISPLAYING_LEFT_PANE,
  MIN_WIDTH_FOR_DISPLAYING_PANES,
  OutputConsole,
  OutputTablePreview,
  ScriptingEditor,
  InputOutputPane,
  setActiveEditorStoreForAi,
  setConsoleHandler,
  useShouldFocusBePainted,
  useReadonlyStore,
};
export type {
  ConsoleHandler,
  ConsoleText,
  GenericInitialData,
  GenericNodeSettings,
  InitialDataServiceType,
  SubItem,
  InputOutputModel,
  InsertionEvent,
  KAIConfig,
  PortConfigs,
  InputConnectionInfo,
  ScriptingServiceType,
  SettingsMenuItem,
  UseCodeEditorParams,
  UseCodeEditorReturn,
  UseDiffEditorParams,
  UseDiffEditorReturn,
};
