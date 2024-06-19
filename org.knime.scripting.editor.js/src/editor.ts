import "./editor.css";
import * as monaco from "monaco-editor";
import {
  computed,
  onMounted,
  onUnmounted,
  readonly,
  ref,
  shallowRef,
  type Ref,
} from "vue";

// ====== TYPES ======

type ContainerParams = {
  /** The HTML element to mount the editor to. */
  container: Ref<HTMLDivElement | undefined>;
};

export type UseCodeEditorParams = ContainerParams &
  (
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

  /**
   * Inserts the specified function reference at the current cursor position in the editor.
   * Uses snippets to give really nice insertion behavior for function arguments. If the
   * arguments are null, the function will be inserted without arguments OR brackets.
   * If the arguments are an empty list, the function will be inserted with empty brackets.
   *
   * @param functionName
   * @param functionArgs the arguments to the function.
   */
  insertFunctionReference: (
    functionName: string,
    functionArgs: string[] | null,
  ) => void;
};

export type UseDiffEditorParams = ContainerParams & {
  originalModel: monaco.editor.ITextModel;
  modifiedFileName: string;
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

const EDITOR_OPTIONS = {
  minimap: { enabled: false },
  automaticLayout: true,
  glyphMargin: false,
  lightbulb: {
    enabled: true,
  },
  scrollBeyondLastLine: true,
  fixedOverflowWidgets: true,
  suggest: { showWords: false }, // Disable word suggestions - better suggestions are provided by the language server
  fontFamily: '"Roboto Mono", serif',
  fontWeight: "400",
  lineNumbersMinChars: 3,
  lineDecorationsWidth: "0.0ch",
} satisfies monaco.editor.IEditorConstructionOptions;

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
    // Start with an edit that inserts the text at the current cursor position
    // replacing the current selection if there is one
    const requiredEdits = [] as monaco.editor.IIdentifiedSingleEditOperation[];

    // Add requiredImport before the text so it is inserted before the text
    // even if the cursor is at the top of the editor
    if (requiredImport) {
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

const createInsertFunctionReferenceFunction = (
  editor: Ref<monaco.editor.IStandaloneCodeEditor | undefined>,
) => {
  return (functionName: string, args: string[] | null) => {
    const snippetController = editor.value?.getContribution(
      "snippetController2",
    ) as any; // The Monaco API doesn't expose the type for us :(

    if (args === null) {
      snippetController?.insert(functionName);
    } else {
      snippetController?.insert(
        `${functionName}(${args.map((arg, i) => `$\{${i + 1}:${arg}}`).join(", ")})`,
      );
    }
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
      ...EDITOR_OPTIONS,
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
    insertFunctionReference: createInsertFunctionReferenceFunction(editor),
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
      originalEditable: false,
      ...EDITOR_OPTIONS,
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
export const useMainCodeEditor = (params: UseCodeEditorParams) => {
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
