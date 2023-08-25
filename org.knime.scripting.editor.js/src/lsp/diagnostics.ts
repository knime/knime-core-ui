import { MarkerSeverity, MarkerTag, editor } from "monaco-editor";
import {
  DiagnosticSeverity,
  DiagnosticTag,
  type Diagnostic,
  type PublishDiagnosticsClientCapabilities,
} from "vscode-languageserver-protocol";

import { mapRangeToMonaco } from "./mapping-utils";

const mapDiagnosticSeverity = (
  severity: DiagnosticSeverity,
): MarkerSeverity => {
  switch (severity) {
    case DiagnosticSeverity.Error:
      return MarkerSeverity.Error;
    case DiagnosticSeverity.Warning:
      return MarkerSeverity.Warning;
    case DiagnosticSeverity.Information:
      return MarkerSeverity.Info;
    case DiagnosticSeverity.Hint:
      return MarkerSeverity.Hint;
    default:
      return MarkerSeverity.Info;
  }
};

const mapDiagnosticTags = (tags: DiagnosticTag[]): MarkerTag[] => {
  return tags.flatMap((tag) => {
    switch (tag) {
      case DiagnosticTag.Unnecessary:
        return [MarkerTag.Unnecessary];
      case DiagnosticTag.Deprecated:
        return [MarkerTag.Deprecated];
      default:
        return [];
    }
  });
};

export const publishDiagnosticsCapibilities: PublishDiagnosticsClientCapabilities =
  {
    relatedInformation: false,
    codeDescriptionSupport: false,
    versionSupport: false,
    dataSupport: false,
    tagSupport: {
      valueSet: [DiagnosticTag.Unnecessary, DiagnosticTag.Deprecated],
    },
  };

export const mapDiagnosticToMarkerData = (
  diagnostics: Diagnostic[],
): editor.IMarkerData[] => {
  const map = (diagnostic: Diagnostic): editor.IMarkerData => {
    // NB: We do not map the codeDescription because we cannot open links anyway

    // Map the code
    let code: string | undefined;
    if (diagnostic.code) {
      if (typeof diagnostic.code === "number") {
        code = `${diagnostic.code}`;
      } else {
        code = diagnostic.code;
      }
    }

    return {
      code,
      severity: mapDiagnosticSeverity(
        diagnostic.severity ?? DiagnosticSeverity.Information,
      ),
      message: diagnostic.message,
      source: diagnostic.source,
      tags: mapDiagnosticTags(diagnostic.tags ?? []),
      ...mapRangeToMonaco(diagnostic.range),
    };
  };

  return diagnostics.map(map);
};
