import { Position, editor, languages } from "monaco-editor";
import { describe, expect, it } from "vitest";
import { SignatureHelpTriggerKind } from "vscode-languageserver-protocol";
import {
  getSignatureHelpParams,
  mapSignatureHelpResult,
} from "../signature-help";

describe("signature-help", () => {
  describe("getSignatureHelpParams", () => {
    const editorModel = {
      uri: "file:///tmp/file.txt",
    } as unknown as editor.ITextModel; // We just mock what we need
    const position = (lineNumber = 1, column = 1) =>
      ({ lineNumber, column }) as unknown as Position;
    const context = (
      triggerKind = languages.SignatureHelpTriggerKind.Invoke,
      isRetrigger = false,
      triggerCharacter?: string,
    ) => ({
      triggerKind,
      isRetrigger,
      triggerCharacter,
    });

    it("maps uri", () => {
      const signatureHelpParams = getSignatureHelpParams(
        editorModel,
        position(),
        context(),
      );
      expect(signatureHelpParams.textDocument.uri).toBe("file:///tmp/file.txt");
    });

    it("maps position", () => {
      const signatureHelpParams = getSignatureHelpParams(
        editorModel,
        position(20, 10),
        context(),
      );
      expect(signatureHelpParams.position.line).toBe(19);
      expect(signatureHelpParams.position.character).toBe(9);
    });

    it("maps context", () => {
      const signatureHelpParams = getSignatureHelpParams(
        editorModel,
        position(20, 10),
        context(languages.SignatureHelpTriggerKind.TriggerCharacter, true, "a"),
      );
      expect(signatureHelpParams.context?.triggerKind).toBe(
        SignatureHelpTriggerKind.TriggerCharacter,
      );
      expect(signatureHelpParams.context?.isRetrigger).toBe(true);
      expect(signatureHelpParams.context?.triggerCharacter).toBe("a");
    });
  });

  it("mapSignatureHelpResult", () => {
    const signatureHelp = mapSignatureHelpResult({
      signatures: [
        {
          label: "signature 1",
          documentation: "signature 1 doc",
          parameters: [
            { label: "param 1", documentation: "param 1 doc" },
            { label: "param 2" },
          ],
          activeParameter: 1,
        },
        {
          label: "signature 2",
        },
      ],
      activeParameter: 10,
      activeSignature: 8,
    });
    const signatureHelp0 = signatureHelp.value.signatures[0];
    const signatureHelp1 = signatureHelp.value.signatures[1];
    expect(signatureHelp0.label).toBe("signature 1");
    expect(signatureHelp0.documentation).toEqual(
      expect.objectContaining({ value: "signature 1 doc" }),
    );
    expect(signatureHelp0.parameters[0].label).toBe("param 1");
    expect(signatureHelp0.parameters[0].documentation).toEqual(
      expect.objectContaining({ value: "param 1 doc" }),
    );
    expect(signatureHelp0.parameters[1].label).toBe("param 2");
    expect(signatureHelp0.activeParameter).toBe(1);
    expect(signatureHelp1.label).toBe("signature 2");
    expect(signatureHelp.value.activeParameter).toBe(10);
    expect(signatureHelp.value.activeSignature).toBe(8);
  });
});
