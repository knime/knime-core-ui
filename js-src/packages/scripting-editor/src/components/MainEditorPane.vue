<script setup lang="ts">
import { onMounted, ref, watch } from "vue";
import { onKeyStroke } from "@vueuse/core";

import { useMainCodeEditor } from "../editor";
import { getInitialData, getSettingsService } from "../init";
import { useReadonlyStore } from "../store/readOnly";
import {
  applyConstrainedEditing,
  calculateConstrainedRanges,
} from "../constrainedMonaco";

import { COLUMN_INSERTION_EVENT } from "./InputOutputItem.vue";
import { insertionEventHelper } from "./utils/insertionEventHelper";

interface Props {
  showControlBar: boolean;
  language: string;
  fileName: string;
  toSettings?: (settings: { script: string }) => unknown;
  dropEventHandler?: (event: DragEvent) => void;
  modelOrView: "model" | "view";
}

const props = withDefaults(defineProps<Props>(), {
  toSettings: (settings: { script: string }) => settings,
  dropEventHandler: () => {},
});

// Main editor
const editorRef = ref<HTMLDivElement>();
const codeEditorState = useMainCodeEditor({
  container: editorRef,
  fileName: props.fileName,
  language: props.language,
});

insertionEventHelper
  .getInsertionEventHelper(COLUMN_INSERTION_EVENT)
  .registerInsertionListener((event) => {
    codeEditorState.insertColumnReference(
      event.textToInsert,
      event.extraArgs?.requiredImport,
    );
  });

onMounted(() => {
  const initialSettings = getSettingsService().getSettings();
  const settingsInitialData = getSettingsService().getSettingsInitialData();
  const initialData = getInitialData();

  let script = "";
  let readonly = false;
  let overwrittenByFlowVariable = "";
  let scriptSectionsInfo: Array<{
    isEditable: boolean;
    contentOrConfigKey: string;
    lineOffset: number;
    lineCount: number;
  }> | undefined;

  // Check if we have multi-section script (new format)
  const scriptSections = initialData.scriptSections as Array<{
    isEditable: boolean;
    contentOrConfigKey: string;
  }> | undefined;

  if (scriptSections && settingsInitialData !== undefined) {
    // Multi-section script mode
    const sectionsWithContent = scriptSections.map((section) => {
      if (section.isEditable) {
        // Load content from settings using the config key
        const content =
          (settingsInitialData.data.model?.[
            section.contentOrConfigKey
          ] as string) ?? "";
        return { isEditable: true, content };
      } else {
        // Use the template text directly
        return { isEditable: false, content: section.contentOrConfigKey };
      }
    });

    // Calculate line offsets and counts for each section
    scriptSectionsInfo = [];
    let currentLine = 0;
    sectionsWithContent.forEach((section, idx) => {
      const lineCount = section.content.split("\n").length;
      scriptSectionsInfo!.push({
        isEditable: section.isEditable,
        contentOrConfigKey: scriptSections[idx].contentOrConfigKey,
        lineOffset: currentLine,
        lineCount: lineCount,
      });
      currentLine += lineCount;
    });

    // Combine all sections into a single script
    script = sectionsWithContent.map((s) => s.content).join("");

    // Apply constrained editing after the editor is initialized
    if (codeEditorState.editor.value) {
      const constrainedRanges = calculateConstrainedRanges(sectionsWithContent);
      applyConstrainedEditing(codeEditorState.editor.value, constrainedRanges);
    }
  } else if (settingsInitialData !== undefined) {
    // Single script mode (legacy)
    const scriptConfigKey = initialData.mainScriptConfigKey ?? "script";
    script =
      (settingsInitialData.data.model?.[scriptConfigKey] as string) ?? "";
    overwrittenByFlowVariable =
      settingsInitialData.flowVariableSettings?.[`model.${scriptConfigKey}`]
        ?.controllingFlowVariableName ?? "";
    readonly = Boolean(overwrittenByFlowVariable);
  }

  if (initialSettings !== undefined) {
    script = initialSettings.script;
    overwrittenByFlowVariable = initialSettings.scriptUsedFlowVariable;
    readonly = initialSettings.settingsAreOverriddenByFlowVariable ?? false;
  }

  codeEditorState.setInitialText(script);
  useReadonlyStore().value = readonly;

  codeEditorState.editor.value?.updateOptions({
    readOnly: useReadonlyStore().value,
    readOnlyMessage: {
      value: `Read-Only-Mode: The script is set by the flow variable '${overwrittenByFlowVariable}'.`,
    },
    renderValidationDecorations: "on",
  });

  // Register settings and handle script changes
  if (scriptSectionsInfo) {
    // Multi-section mode: decompose the script and save each editable section
    const register = getSettingsService().registerSettings(props.modelOrView);
    const registeredCallbacks = new Map<
      string,
      { setValue: (value: any) => void }
    >();

    // Register a callback for each editable section
    scriptSectionsInfo.forEach((section) => {
      if (section.isEditable) {
        const callback = register({ initialValue: "" });
        registeredCallbacks.set(section.contentOrConfigKey, callback);
      }
    });

    // Watch for changes and decompose the script
    watch(codeEditorState.text, () => {
      const fullText = codeEditorState.text.value ?? "";
      const lines = fullText.split("\n");

      scriptSectionsInfo!.forEach((section) => {
        if (section.isEditable) {
          // Extract the lines for this section
          const sectionLines = lines.slice(
            section.lineOffset,
            section.lineOffset + section.lineCount,
          );
          const sectionText = sectionLines.join("\n");

          // Update the corresponding setting
          const callback = registeredCallbacks.get(section.contentOrConfigKey);
          if (callback) {
            callback.setValue(sectionText);
          }
        }
      });
    });

    getSettingsService().registerSettingsGetterForApply(() => {
      const fullText = codeEditorState.text.value ?? "";
      const lines = fullText.split("\n");
      const sectionsObj: Record<string, string> = {};

      scriptSectionsInfo!.forEach((section) => {
        if (section.isEditable) {
          const sectionLines = lines.slice(
            section.lineOffset,
            section.lineOffset + section.lineCount,
          );
          sectionsObj[section.contentOrConfigKey] = sectionLines.join("\n");
        }
      });

      return props.toSettings({ script: fullText, ...sectionsObj });
    });
  } else {
    // Single script mode (legacy)
    const register = getSettingsService().registerSettings(props.modelOrView);
    const onScriptChange = register({ initialValue: script });
    watch(codeEditorState.text, () => {
      onScriptChange.setValue(codeEditorState.text.value ?? "");
    });

    getSettingsService().registerSettingsGetterForApply(() =>
      props.toSettings({ script: codeEditorState.text.value ?? "" }),
    );
  }
});

onKeyStroke("Escape", () => {
  if (codeEditorState.editor.value?.hasTextFocus()) {
    (document.activeElement as HTMLElement)?.blur();
  }
});

// register undo changes from outside the editor
onKeyStroke("z", (e) => {
  const key = navigator.userAgent.toLowerCase().includes("mac")
    ? e.metaKey
    : e.ctrlKey;

  if (key) {
    codeEditorState.editor.value?.trigger("window", "undo", {});
  }
});
</script>

<template>
  <div class="editor-container">
    <div
      ref="editorRef"
      class="code-editor"
      @drop="
        useReadonlyStore().value
          ? $event.preventDefault()
          : dropEventHandler($event)
      "
    />
  </div>
</template>

<style>
.code-editor {
  height: 100%;
}

.editor-container {
  height: 100%;
  min-height: 0;
}
</style>
