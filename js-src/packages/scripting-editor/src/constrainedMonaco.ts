/**
 * Constrained Monaco Editor support for read-only regions.
 * 
 * This module provides functionality to create editors with multiple sections,
 * some editable and some read-only. This is used for template-based editors
 * like the Java Snippet node where users edit specific parts of a larger code structure.
 */

import * as monaco from "monaco-editor";

/**
 * Represents a range in the editor with read-only status.
 */
export interface ConstrainedRange {
  startLineNumber: number;
  startColumn: number;
  endLineNumber: number;
  endColumn: number;
  isReadOnly: boolean;
}

/**
 * Apply constrained editing to a Monaco editor model.
 * This prevents users from editing specified read-only regions.
 * 
 * @param editor The Monaco editor instance
 * @param constrainedRanges Array of ranges with their read-only status
 */
export function applyConstrainedEditing(
  editor: monaco.editor.IStandaloneCodeEditor,
  constrainedRanges: ConstrainedRange[],
): monaco.IDisposable {
  const model = editor.getModel();
  if (!model) {
    throw new Error("Editor model not found");
  }

  // Apply decorations to visually distinguish read-only regions
  const decorations = model.deltaDecorations(
    [],
    constrainedRanges
      .filter((range) => range.isReadOnly)
      .map((range) => ({
        range: new monaco.Range(
          range.startLineNumber,
          range.startColumn,
          range.endLineNumber,
          range.endColumn,
        ),
        options: {
          isWholeLine: false,
          className: "read-only-section",
          glyphMarginClassName: "read-only-glyph-margin",
          stickiness:
            monaco.editor.TrackedRangeStickiness.NeverGrowsWhenTypingAtEdges,
        },
      })),
  );

  // Prevent editing in read-only regions
  const disposable = editor.onDidChangeModelContent((e) => {
    const changes = e.changes;
    let hasInvalidEdit = false;

    for (const change of changes) {
      const changeRange = change.range;

      // Check if the change overlaps with any read-only region
      for (const constrainedRange of constrainedRanges) {
        if (!constrainedRange.isReadOnly) continue;

        const readOnlyRange = new monaco.Range(
          constrainedRange.startLineNumber,
          constrainedRange.startColumn,
          constrainedRange.endLineNumber,
          constrainedRange.endColumn,
        );

        // Check if ranges intersect
        if (
          changeRange.startLineNumber <= readOnlyRange.endLineNumber &&
          changeRange.endLineNumber >= readOnlyRange.startLineNumber
        ) {
          hasInvalidEdit = true;
          break;
        }
      }

      if (hasInvalidEdit) break;
    }

    // If there was an invalid edit, undo it
    if (hasInvalidEdit) {
      editor.trigger("constrained-editor", "undo", {});
    }
  });

  return {
    dispose: () => {
      disposable.dispose();
      if (model) {
        model.deltaDecorations(decorations, []);
      }
    },
  };
}

/**
 * Calculate constrained ranges from script sections.
 * 
 * @param sections Array of section definitions with content
 * @returns Array of constrained ranges
 */
export function calculateConstrainedRanges(
  sections: Array<{ isEditable: boolean; content: string }>,
): ConstrainedRange[] {
  const ranges: ConstrainedRange[] = [];
  let currentLine = 1;
  let currentColumn = 1;

  for (const section of sections) {
    const lines = section.content.split("\n");
    const numLines = lines.length;

    if (numLines === 1) {
      // Single line section
      const endColumn = currentColumn + section.content.length;
      ranges.push({
        startLineNumber: currentLine,
        startColumn: currentColumn,
        endLineNumber: currentLine,
        endColumn,
        isReadOnly: !section.isEditable,
      });
      currentColumn = endColumn;
    } else {
      // Multi-line section
      const lastLineLength = lines[lines.length - 1].length;
      ranges.push({
        startLineNumber: currentLine,
        startColumn: currentColumn,
        endLineNumber: currentLine + numLines - 1,
        endColumn: lastLineLength + 1,
        isReadOnly: !section.isEditable,
      });
      currentLine += numLines - 1;
      currentColumn = lastLineLength + 1;
    }
  }

  return ranges;
}
