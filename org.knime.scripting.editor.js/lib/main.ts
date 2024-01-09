import CompactTabBar from "@/components/CompactTabBar.vue";
import OutputConsole, {
  type ConsoleHandler,
  type ConsoleText,
} from "@/components/OutputConsole.vue";
import ScriptingEditor from "@/components/ScriptingEditor.vue";
import { type SettingsMenuItem } from "@/components/SettingsPage.vue";

import type {
  UseCodeEditorParams,
  UseCodeEditorReturn,
  UseDiffEditorParams,
  UseDiffEditorReturn,
} from "@/editor";
import editor from "@/editor";

import type { NodeSettings, ScriptingServiceType } from "@/scripting-service";
import { getScriptingService } from "@/scripting-service";
import * as scriptingServiceBrowserMock from "@/scripting-service-browser-mock";
import { consoleHandler } from "@/consoleHandler";

export {
  CompactTabBar,
  OutputConsole,
  ScriptingEditor,
  editor,
  getScriptingService,
  scriptingServiceBrowserMock,
  consoleHandler,
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
