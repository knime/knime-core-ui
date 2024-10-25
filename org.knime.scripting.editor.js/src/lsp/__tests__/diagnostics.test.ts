import { describe, expect, it } from "vitest";
import { MarkerSeverity, MarkerTag } from "monaco-editor";
import {
  DiagnosticSeverity,
  DiagnosticTag,
  type Range,
} from "vscode-languageserver-protocol";

import { mapDiagnosticToMarkerData } from "../diagnostics";

describe("diagnostics", () => {
  describe("mapDiagnosticToMarkerData", () => {
    const range = (
      starLine = 0,
      startColumn = 0,
      endLine = 1,
      endColumn = 1,
    ): Range => {
      return {
        start: { line: starLine, character: startColumn },
        end: { line: endLine, character: endColumn },
      };
    };

    it("maps message", () => {
      const markerData = mapDiagnosticToMarkerData([
        { message: "foo", range: range() },
        { message: "bar", range: range() },
      ]);
      expect(markerData[0].message).toBe("foo");
      expect(markerData[1].message).toBe("bar");
    });

    it("maps marker data", () => {
      const markerData = mapDiagnosticToMarkerData([
        {
          message: "foo",
          range: range(),
          severity: DiagnosticSeverity.Hint,
          code: "code",
          source: "source",
          tags: [DiagnosticTag.Deprecated, DiagnosticTag.Unnecessary],
        },
      ]);
      expect(markerData[0].message).toBe("foo");
      expect(markerData[0].severity).toBe(MarkerSeverity.Hint);
      expect(markerData[0].code).toBe("code");
      expect(markerData[0].source).toBe("source");
      expect(markerData[0].tags).toEqual([
        MarkerTag.Deprecated,
        MarkerTag.Unnecessary,
      ]);
    });
  });
});
