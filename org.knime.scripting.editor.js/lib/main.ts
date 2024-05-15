import CompactTabBar from "@/components/CompactTabBar.vue";
import OutputConsole, {
  type ConsoleHandler,
  type ConsoleText,
} from "@/components/OutputConsole.vue";
import ScriptingEditor from "@/components/ScriptingEditor.vue";
import { type SettingsMenuItem } from "@/components/SettingsPage.vue";
import useShouldFocusBePainted from "@/components/utils/shouldFocusBePainted";

import type {
  UseCodeEditorParams,
  UseCodeEditorReturn,
  UseDiffEditorParams,
  UseDiffEditorReturn,
} from "@/editor";
import editor from "@/editor";

import { consoleHandler } from "@/consoleHandler";
import {
  getScriptingService,
  type NodeSettings,
  type ScriptingServiceType,
} from "@/scripting-service";
import { setActiveEditorStoreForAi } from "@/store/ai-bar";

export {
  CompactTabBar,
  consoleHandler,
  editor,
  getScriptingService,
  OutputConsole,
  ScriptingEditor,
  useShouldFocusBePainted,
  setActiveEditorStoreForAi,
};
export type {
  ConsoleHandler,
  ConsoleText,
  NodeSettings,
  ScriptingServiceType,
  SettingsMenuItem,
  UseCodeEditorParams,
  UseCodeEditorReturn,
  UseDiffEditorParams,
  UseDiffEditorReturn,
};
