import {
  Position,
  editor,
  languages,
  type IMarkdownString,
  type IRange,
} from "monaco-editor";
import {
  Hover,
  MarkedString,
  MarkupContent,
  type HoverClientCapabilities,
  type HoverParams,
} from "vscode-languageserver-protocol";

import {
  getTextDocPositionParms,
  mapMarkupToMonaco,
  mapRangeToMonaco,
} from "./mapping-utils";

export const hoverCapibilities: HoverClientCapabilities = {
  dynamicRegistration: false,
  contentFormat: ["markdown", "plaintext"],
};

export const getHoverParams = (
  editorModel: editor.ITextModel,
  position: Position,
): HoverParams => {
  return getTextDocPositionParms(editorModel, position);
};

export const mapHoverResult = (result: Hover): languages.Hover => {
  const mapMarkedString = (markedString: MarkedString): IMarkdownString => {
    let value: string;
    if (typeof markedString === "string") {
      value = markedString;
    } else {
      // Encapsulate in code block
      value = `\`\`\`${markedString.language}\n${markedString.value}\n\`\`\``;
    }
    return mapMarkupToMonaco(value);
  };

  // Get the range if it is defined
  let range: IRange | undefined;
  if (typeof result.range !== "undefined") {
    range = mapRangeToMonaco(result.range);
  }

  // Go through the different types of contents
  if (
    typeof result.contents === "string" ||
    (result.contents.hasOwnProperty("language") &&
      result.contents.hasOwnProperty("value"))
  ) {
    // Option MarkedString
    return {
      contents: [mapMarkedString(result.contents as MarkedString)],
      range,
    };
  } else if (Array.isArray(result.contents)) {
    // Option MarkedString[]
    return {
      contents: result.contents.map(mapMarkedString),
      range,
    };
  } else if (result.contents.hasOwnProperty("value")) {
    // Option MarkupContent
    // NB: We do not care about the kind
    const content = result.contents as MarkupContent;
    return {
      contents: [mapMarkupToMonaco(content)],
      range,
    };
  } else {
    throw Error(`Got invalid hover result: ${JSON.stringify(result)}`);
  }
};
