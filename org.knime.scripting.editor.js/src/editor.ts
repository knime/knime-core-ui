import "./editor.css";
import {
  type Ref,
  computed,
  onMounted,
  onUnmounted,
  readonly,
  ref,
  shallowRef,
} from "vue";
import * as monaco from "monaco-editor";

// Force monaco to remeasure fonts after they are loaded
document.fonts?.ready.then(() => {
  monaco.editor.remeasureFonts();
});

// ====== TYPES ======

type ContainerParams = {
  /** The HTML element to mount the editor to. */
  container: Ref<HTMLDivElement | undefined>;
};

export type UseCodeEditorParams = ContainerParams & {
  extraEditorOptions?: monaco.editor.IStandaloneEditorConstructionOptions;
} & (
    | {
        language: string;
        fileName: string;
      }
    | {
        editorModel: monaco.editor.ITextModel;
      }
  );

export type UseCodeEditorReturn = {
  editorModel: monaco.editor.ITextModel;
  editor: Ref<monaco.editor.IStandaloneCodeEditor | undefined>;

  /**
   * The current text in the editor. Allows updating the full text. The user
   * will be able to undo the change.
   */
  text: Ref<string>;

  /**
   * The current selection in the editor.
   */
  selection: Readonly<Ref<string>>;

  /**
   * All currently selected lines in the editor from first to last column.
   * Also includes partly selected lines.
   */
  selectedLines: Readonly<Ref<string>>;

  /**
   * Set the initial text of the editor. The user won't be able to undo this.
   * @param text the text to set
   */
  setInitialText: (text: string) => void;

  /**
   * Inserts the specified text at the current cursor position in the editor.
   * @param text the text to insert.
   */
  insertText: (text: string) => void;

  /**
   * Inserts the specified text at the current cursor position in the editor,
   * and adds the required import if one is provided.
   * @param text the text to insert.
   * @param requiredImport the import to add before the text. Can be left out.
   */
  insertColumnReference: (
    textToInsert: string,
    requiredImport?: string,
  ) => void;
};

export type UseDiffEditorParams = ContainerParams & {
  originalModel: monaco.editor.ITextModel;
  modifiedFileName: string;
  extraEditorOptions?: monaco.editor.IDiffEditorConstructionOptions;
};

export type UseDiffEditorReturn = {
  editorModel: monaco.editor.IDiffEditorModel;
  editor: Ref<monaco.editor.IStandaloneDiffEditor | undefined>;

  /**
   * The current modified text. Allows updating the full text. The user
   * will be able to undo the change.
   */
  modifiedText: Ref<string>;

  /**
   * Set the initial modified text of the editor. The user won't be able to undo this.
   * @param text the text to set
   */
  setInitialModifiedText: (modifiedText: string) => void;
};

// ====== CONSTANTS ======

const EDITOR_OPTIONS_COMMON: monaco.editor.IEditorConstructionOptions = {
  minimap: { enabled: false },
  automaticLayout: true,
  glyphMargin: false,
  lightbulb: {
    enabled: true,
  },
  scrollBeyondLastLine: true,
  fixedOverflowWidgets: true,
  suggest: {
    showWords: false, // Disable word suggestions - better suggestions are provided by the language server
    snippetsPreventQuickSuggestions: false,
  },
  fontFamily: '"Roboto Mono", serif',
  fontWeight: "400",
  lineNumbersMinChars: 3,
  lineDecorationsWidth: "0.0ch",
  scrollbar: {
    alwaysConsumeMouseWheel: false,
  },
};

const EDITOR_OPTIONS_DIFF: monaco.editor.IDiffEditorConstructionOptions = {
  ...EDITOR_OPTIONS_COMMON,
  renderOverviewRuler: false,
  overviewRulerBorder: false,
  originalEditable: false,
};

// ====== HELPERS ======

/** Helper to get the appropriate editor model for the given parameters */
const getEditorModel = (params: UseCodeEditorParams) => {
  if ("editorModel" in params) {
    // Use the given model
    return params.editorModel;
  } else {
    // No model given - if the file is already open, use that model, otherwise create a new one
    const uri = monaco.Uri.parse(`inmemory://model/${params.fileName}`);
    return (
      monaco.editor.getModel(uri) ??
      monaco.editor.createModel("", params.language, uri)
    );
  }
};

/** Helper to create a readonly ref and setter function for the editor model value */
const syncWithModel = (model: monaco.editor.ITextModel) => {
  const textFromModel = ref(model.getValue());
  model.onDidChangeContent(() => {
    textFromModel.value = model.getValue();
  });

  const text = computed({
    get: () => textFromModel.value,
    set: (newText: string) => {
      // NB: We push a stack element to allow undoing the set call
      model.pushStackElement();
      model.pushEditOperations(
        [],
        [{ range: model.getFullModelRange(), text: newText }],
        () => null,
      );
    },
  });

  // This allows setting the value without allowing undoing it
  const setInitialText = (newValue: string) => model.setValue(newValue);
  return { text, setInitialText };
};

/** Assertion function to assert that the HTML element is not undefined */
const assertElementProvided: (
  element: Ref<HTMLDivElement | undefined>,
) => asserts element is Ref<HTMLDivElement> = (element) => {
  if (typeof element.value === "undefined") {
    throw new Error(
      "Could not create code editor because no element was provided.",
    );
  }
};

/** Helper to create a function that disposes the editor and models if they are unused */
const createDisposeFn = (
  editor: Ref<monaco.editor.IEditor | undefined>,
  editorModels: monaco.editor.ITextModel[],
) => {
  return () => {
    if (editor.value) {
      editor.value.dispose();
    }

    for (const model of editorModels) {
      // NB: The model might still be attached to another editor
      if (!model.isAttachedToEditor()) {
        model.dispose();
      }
    }
  };
};

/** Helper to insert a column reference at the current cursor position */
const createInsertColumnReferenceFunction = (
  editor: Ref<monaco.editor.IStandaloneCodeEditor | undefined>,
) => {
  return (textToInsert: string, requiredImport?: string) => {
    if (editor.value?.getOption(monaco.editor.EditorOption.readOnly)) {
      return;
    }

    // Start with an edit that inserts the text at the current cursor position
    // replacing the current selection if there is one
    const requiredEdits = [] as monaco.editor.IIdentifiedSingleEditOperation[];

    // Add requiredImport before the text so it is inserted before the text
    // even if the cursor is at the top of the editor
    if (
      requiredImport &&
      !editor.value?.getModel()?.getValue().includes(requiredImport)
    ) {
      requiredEdits.push({
        range: new monaco.Selection(1, 1, 1, 1),
        text: `${requiredImport}\n`,
        forceMoveMarkers: true,
      });
    }

    requiredEdits.push({
      range: editor.value?.getSelection(),
      text: textToInsert,
      forceMoveMarkers: true,
    } as monaco.editor.IIdentifiedSingleEditOperation);

    editor.value?.getModel()?.pushEditOperations([], requiredEdits, () => null);

    // Create an undo stop so the user can undo the insert
    editor.value?.getModel()?.pushStackElement();
  };
};

const createInsertTextFunction = (
  editor: Ref<monaco.editor.IStandaloneCodeEditor | undefined>,
) => {
  return (textToInsert: string) => {
    if (editor.value?.getOption(monaco.editor.EditorOption.readOnly)) {
      return;
    }

    // Inserts the text at the current cursor position
    // replacing the current selection if there is one
    const requiredEdits = [
      {
        range: editor.value?.getSelection(),
        text: textToInsert,
        forceMoveMarkers: true,
      },
    ] as monaco.editor.IIdentifiedSingleEditOperation[];

    editor.value?.getModel()?.pushEditOperations([], requiredEdits, () => null);

    // Create an undo stop so the user can undo the insert
    editor.value?.getModel()?.pushStackElement();
  };
};

// ====== COMPOSABLES ======

export const useCodeEditor = (
  params: UseCodeEditorParams,
): UseCodeEditorReturn => {
  const editor = shallowRef<monaco.editor.IStandaloneCodeEditor>();
  const editorModel = getEditorModel(params);

  const selection = ref("");
  const selectedLines = ref("");

  onMounted(() => {
    assertElementProvided(params.container);

    editor.value = monaco.editor.create(params.container.value, {
      model: editorModel,
      ...EDITOR_OPTIONS_COMMON,
      ...(params.extraEditorOptions ?? {}),
    });

    editor.value.onDidChangeCursorSelection((e) => {
      selection.value = editorModel.getValueInRange(e.selection);
      selectedLines.value = editorModel.getValueInRange({
        startLineNumber: e.selection.startLineNumber,
        startColumn: 0,
        endLineNumber: e.selection.endLineNumber,
        endColumn: editorModel.getLineLastNonWhitespaceColumn(
          e.selection.endLineNumber,
        ),
      });
    });
  });

  onUnmounted(createDisposeFn(editor, [editorModel]));

  return {
    editor,
    editorModel,
    selection: readonly(selection),
    selectedLines: readonly(selectedLines),
    insertColumnReference: createInsertColumnReferenceFunction(editor),
    insertText: createInsertTextFunction(editor),
    ...syncWithModel(editorModel),
  };
};

export const useDiffEditor = (
  params: UseDiffEditorParams,
): UseDiffEditorReturn => {
  const editor = shallowRef<monaco.editor.IStandaloneDiffEditor>();
  const original = params.originalModel;
  const modified = monaco.editor.createModel(
    "",
    original.getLanguageId(),
    monaco.Uri.parse(`inmemory://model/${params.modifiedFileName}`),
  );
  const editorModel = { original, modified };

  onMounted(() => {
    assertElementProvided(params.container);
    editor.value = monaco.editor.createDiffEditor(params.container.value, {
      ...EDITOR_OPTIONS_DIFF,
      ...(params.extraEditorOptions ?? {}),
    });
    editor.value.setModel(editorModel);
  });

  onUnmounted(createDisposeFn(editor, [original, modified]));

  const { text: modifiedText, setInitialText: setInitialModifiedText } =
    syncWithModel(modified);
  return { editor, editorModel, modifiedText, setInitialModifiedText };
};

// ====== MAIN EDITOR STATE ======

const mainEditorStore = shallowRef<UseCodeEditorReturn>();

// Exported in the lib for everyone to use
export const useMainCodeEditorStore = () => {
  return mainEditorStore;
};

// Only exported to be used in the ScriptingEditor.vue component
export const useMainCodeEditor = (
  params: UseCodeEditorParams,
): UseCodeEditorReturn => {
  const editorState = useCodeEditor(params);
  mainEditorStore.value = editorState;
  return editorState;
};

// ====== DEFAULT EXPORT ======

// NB: The default export is re-exported in the lib
export default {
  useCodeEditor,
  useDiffEditor,
  useMainCodeEditorStore,
};
