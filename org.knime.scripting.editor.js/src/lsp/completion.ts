import { type IRange, Position, editor, languages } from "monaco-editor";
import {
  type CompletionClientCapabilities,
  type CompletionContext,
  CompletionItem,
  CompletionItemKind,
  CompletionItemTag,
  CompletionList,
  type CompletionParams,
  CompletionTriggerKind,
  InsertTextFormat,
  InsertTextMode,
} from "vscode-languageserver-protocol";

import { getTextDocPositionParms, mapMarkupToMonaco } from "./mapping-utils";

const getCompletionRange = (
  model: editor.ITextModel,
  position: Position,
): IRange => {
  const word = model.getWordUntilPosition(position);
  return {
    startLineNumber: position.lineNumber,
    startColumn: word.startColumn,
    endLineNumber: position.lineNumber,
    endColumn: word.endColumn,
  };
};

const mapCompletionItemKind = (
  kind: CompletionItemKind,
): languages.CompletionItemKind => {
  // Find the key for the number value
  const key = (
    Object.keys(CompletionItemKind) as (keyof typeof CompletionItemKind)[]
  ).find((key) => CompletionItemKind[key] === kind);
  if (!key) {
    return languages.CompletionItemKind.Text;
  }

  // Find the value for the key in the monaco enum
  return languages.CompletionItemKind[
    key as keyof typeof languages.CompletionItemKind
  ];
};

const mapCompletionItemTags = (
  tags: CompletionItemTag[],
): languages.CompletionItemTag[] =>
  tags.flatMap((tag) => {
    if (tag === CompletionItemTag.Deprecated) {
      return [languages.CompletionItemTag.Deprecated];
    } else {
      return [];
    }
  });

const mapTriggerKind = (
  kind: languages.CompletionTriggerKind,
): CompletionTriggerKind => {
  switch (kind) {
    case languages.CompletionTriggerKind.Invoke:
      return CompletionTriggerKind.Invoked;
    case languages.CompletionTriggerKind.TriggerCharacter:
      return CompletionTriggerKind.TriggerCharacter;
    case languages.CompletionTriggerKind.TriggerForIncompleteCompletions:
      return CompletionTriggerKind.TriggerForIncompleteCompletions;
    default:
      return CompletionTriggerKind.Invoked;
  }
};

const mapCompletionContext = (
  context: languages.CompletionContext,
): CompletionContext => ({
  triggerKind: mapTriggerKind(context.triggerKind),
  triggerCharacter: context.triggerCharacter,
});

const mapCompletionItem = (
  item: CompletionItem,
  range: IRange | languages.CompletionItemRanges,
): languages.CompletionItem & { data: any } => {
  const label: languages.CompletionItemLabel = {
    label: item.label,
    detail: item.labelDetails?.detail,
    description: item.labelDetails?.description,
  };

  const tags = mapCompletionItemTags(item.tags ?? []);
  // prettier-ignore
  if (item.deprecated) { // NOSONAR: server might use the deprecated property
    tags.push(languages.CompletionItemTag.Deprecated);
  }

  let insertTextRules = languages.CompletionItemInsertTextRule.None;
  if (item.insertTextFormat === InsertTextFormat.Snippet) {
    insertTextRules = languages.CompletionItemInsertTextRule.InsertAsSnippet;
  } else if (item.insertTextMode === InsertTextMode.adjustIndentation) {
    insertTextRules = languages.CompletionItemInsertTextRule.KeepWhitespace;
  }

  return {
    label,
    kind: mapCompletionItemKind(item.kind ?? CompletionItemKind.Text),
    tags,
    detail: item.detail,
    ...(item.documentation
      ? { documentation: mapMarkupToMonaco(item.documentation) }
      : {}),
    sortText: item.sortText,
    filterText: item.filterText,
    preselect: item.preselect,
    insertText: item.insertText ?? item.label,
    insertTextRules,
    range,
    commitCharacters: item.commitCharacters,
    data: item.data,
  };
};

export const completionCapibilities: CompletionClientCapabilities = {
  dynamicRegistration: false,
  completionItem: {
    commitCharactersSupport: true,
    deprecatedSupport: true,
    tagSupport: {
      valueSet: [CompletionItemTag.Deprecated],
    },
    documentationFormat: ["markdown", "plaintext"],
    preselectSupport: true,
    labelDetailsSupport: true,
    resolveSupport: {
      properties: ["documentation", "detail"],
    },
  },
  insertTextMode: InsertTextMode.asIs,
  contextSupport: false,
};

export const getCompletionParams = (
  model: editor.ITextModel,
  position: Position,
  context: languages.CompletionContext,
): CompletionParams => ({
  ...getTextDocPositionParms(model, position),
  context: mapCompletionContext(context),
});

export const mapCompletionResult = (
  result: CompletionList | CompletionItem[],
  model: editor.ITextModel,
  position: Position,
): languages.CompletionList => {
  const range = getCompletionRange(model, position);
  if (Array.isArray(result)) {
    // Option: CompletionItem[]
    return {
      suggestions: result.map((item) => mapCompletionItem(item, range)),
    };
  } else {
    // Must be a CompletionList
    return {
      suggestions: result.items.map((item) => mapCompletionItem(item, range)),
      incomplete: result.isIncomplete,
    };
  }
};

export const getCompletionResolveParams = (
  item: languages.CompletionItem,
): CompletionItem => {
  const params: CompletionItem = {
    label: typeof item.label === "string" ? item.label : item.label.label,
  };
  if (item.hasOwnProperty("data")) {
    // @ts-ignore -- we checked for the data property before accessing it
    params.data = item.data;
  }
  return params;
};

export const mapCompletionResolveResult = (
  result: CompletionItem,
  range: IRange | languages.CompletionItemRanges,
): languages.CompletionItem => mapCompletionItem(result, range);
