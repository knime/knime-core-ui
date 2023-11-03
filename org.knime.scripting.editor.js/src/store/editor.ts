import * as monaco from "monaco-editor";
import { reactive } from "vue";

const editorStore = reactive({
  selection: "",
});

export const useEditorStore = () => editorStore;
export const initEditorStore = ({
  editor,
  editorModel,
}: {
  editor: monaco.editor.IStandaloneCodeEditor;
  editorModel: monaco.editor.ITextModel;
}) => {
  // setup event listeners
  editor.onDidChangeCursorSelection((event) => {
    editorStore.selection = editorModel.getValueInRange(event.selection) ?? "";
  });
};
