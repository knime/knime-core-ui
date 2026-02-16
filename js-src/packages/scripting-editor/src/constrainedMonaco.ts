/**
 * Constrained Monaco Editor support for read-only regions.
 * 
 * This module provides functionality to create editors with multiple sections,
 * some editable and some read-only. This is used for template-based editors
 * like the Java Snippet node where users edit specific parts of a larger code structure.
 */

import ConstrainedEditor from "constrained-editor-plugin";
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
 * Restriction format for the constrained-editor-plugin.
 * Restrictions define the EDITABLE regions (inverted from read-only).
 */
interface EditorRestriction {
  range: [number, number, number, number]; // [startLine, startCol, endLine, endCol]
  allowMultiline?: boolean;
  label?: string;
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

  // Convert our ConstrainedRange format to the plugin's restriction format
  // Note: The plugin uses restrictions to define EDITABLE regions,
  // so we only include ranges where isReadOnly = false
  const restrictions: EditorRestriction[] = constrainedRanges
    .filter((range) => !range.isReadOnly) // Only editable regions become restrictions
    .map((range) => ({
      range: [
        range.startLineNumber,
        range.startColumn,
        range.endLineNumber,
        range.endColumn,
      ] as [number, number, number, number],
      allowMultiline: true,
    }));

  // Create the constrained editor instance
  const constrainedEditor = new ConstrainedEditor(editor);
  constrainedEditor.addRestrictionsTo(model, restrictions);

  // Apply visual decorations to read-only regions
  const readOnlyDecorations = model.deltaDecorations(
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

  return {
    dispose: () => {
      constrainedEditor.dispose();
      if (model) {
        model.deltaDecorations(readOnlyDecorations, []);
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
