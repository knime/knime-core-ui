import { editor as monaco, type IRange } from "monaco-editor";

export class EditorService {
  editor?: monaco.IStandaloneCodeEditor;
  editorModel?: monaco.ITextModel;

  public initEditorService({
    editor,
    editorModel,
  }: {
    editor: monaco.IStandaloneCodeEditor;
    editorModel: monaco.ITextModel;
  }) {
    this.editor = editor;
    this.editorModel = editorModel;
  }

  /**
   * @returns the entire editor content or null if monaco has not yet been initialized
   */
  public getScript(): string | null {
    if (
      typeof this.editor === "undefined" ||
      typeof this.editorModel === "undefined"
    ) {
      return null;
    }

    return this.editorModel.getValue();
  }

  /**
   * @returns all currently selected lines in the editor from first to last column. Also returns partly selected lines.
   * Returns null if monaco has not yet been initialized.
   */
  public getSelectedLines(): string | null {
    if (
      typeof this.editor === "undefined" ||
      typeof this.editorModel === "undefined"
    ) {
      return null;
    }

    const selection = this.editor.getSelection();
    if (selection === null) {
      return null;
    }

    const [selectionStartLine, selectionEndLine] = [
      selection.startLineNumber,
      selection.endLineNumber,
    ];

    const selectionLineRange: IRange = {
      startLineNumber: selectionStartLine,
      startColumn: 0,
      endLineNumber: selectionEndLine,
      endColumn:
        this.editorModel.getLineLastNonWhitespaceColumn(selectionEndLine),
    };

    return this.editorModel.getValueInRange(selectionLineRange);
  }

  /**
   * Set a new script which will replace the full editor content.
   * Can be undone via Ctrl+Z.
   *
   * @param newScript the new script
   */
  public setScript(newScript: string) {
    if (typeof this.editorModel === "undefined") {
      return;
    }

    this.editorModel.pushEditOperations(
      [],
      [
        {
          range: this.editorModel.getFullModelRange(),
          text: newScript,
        },
      ],
      () => null,
    );
  }

  public pasteToEditor(textToPaste: string): void {
    if (typeof this.editor === "undefined") {
      return;
    }
    const selection = this.editor.getSelection();
    let range: IRange;
    if (selection === null) {
      const position = this.editor.getPosition();
      if (position === null) {
        return;
      }
      range = {
        startLineNumber: position.lineNumber,
        startColumn: position.column,
        endLineNumber: position.lineNumber,
        endColumn: position.column,
      };
    } else {
      range = selection;
    }

    this.editor.pushUndoStop();
    this.editor.executeEdits("", [{ range, text: textToPaste }]);
  }
}
