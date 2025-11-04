import { type Position, editor, languages } from "monaco-editor";
import {
  ParameterInformation,
  type SignatureHelp,
  type SignatureHelpClientCapabilities,
  type SignatureHelpContext,
  type SignatureHelpParams,
  SignatureHelpTriggerKind,
  SignatureInformation,
} from "vscode-languageserver-protocol";

import { getTextDocPositionParms, mapMarkupToMonaco } from "./mapping-utils";

export const signatureHelpClientCapabilities: SignatureHelpClientCapabilities =
  {
    dynamicRegistration: false,
    contextSupport: true,
    signatureInformation: {
      documentationFormat: ["markdown", "plaintext"],
      parameterInformation: {
        labelOffsetSupport: true,
      },
    },
  };

const mapSignatureHelpTriggerKind = (
  triggerKind: languages.SignatureHelpTriggerKind,
): SignatureHelpTriggerKind => {
  switch (triggerKind) {
    case languages.SignatureHelpTriggerKind.Invoke:
      return SignatureHelpTriggerKind.Invoked;
    case languages.SignatureHelpTriggerKind.TriggerCharacter:
      return SignatureHelpTriggerKind.TriggerCharacter;
    case languages.SignatureHelpTriggerKind.ContentChange:
      return SignatureHelpTriggerKind.ContentChange;
    default:
      return SignatureHelpTriggerKind.Invoked;
  }
};

const mapSignatureHelpContext = (
  context: languages.SignatureHelpContext,
): SignatureHelpContext => {
  return {
    triggerKind: mapSignatureHelpTriggerKind(context.triggerKind),
    triggerCharacter: context.triggerCharacter,
    isRetrigger: context.isRetrigger,
  };
};

const mapSignatureParamter = (
  parameter: ParameterInformation,
): languages.ParameterInformation => {
  return {
    label: parameter.label,
    ...(parameter.documentation
      ? { documentation: mapMarkupToMonaco(parameter.documentation) }
      : {}),
  };
};

const mapSignatureInformation = (
  signature: SignatureInformation,
): languages.SignatureInformation => {
  return {
    label: signature.label,
    ...(signature.documentation
      ? { documentation: mapMarkupToMonaco(signature.documentation) }
      : {}),
    parameters: signature.parameters
      ? signature.parameters.map(mapSignatureParamter)
      : [],
    activeParameter: signature.activeParameter,
  };
};

const mapSignatureHelp = (help: SignatureHelp): languages.SignatureHelp => {
  return {
    signatures: help.signatures.map(mapSignatureInformation),
    activeSignature: help.activeSignature ?? 0,
    activeParameter: help.activeParameter ?? 0,
  };
};

export const getSignatureHelpParams = (
  model: editor.ITextModel,
  position: Position,
  context: languages.SignatureHelpContext,
): SignatureHelpParams => {
  return {
    ...getTextDocPositionParms(model, position),
    context: mapSignatureHelpContext(context),
  };
};

export const mapSignatureHelpResult = (
  result: SignatureHelp,
): languages.SignatureHelpResult => {
  return {
    value: mapSignatureHelp(result),
    dispose: () => {},
  };
};
