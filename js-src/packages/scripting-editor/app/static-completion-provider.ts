import * as monaco from "monaco-editor";

export type StaticCompletionItem = {
  functionName: string;
  arguments: string;
  description: string;
  returnType?: string;
};

/**
 * Converts function arguments string into Monaco snippet format with tab stops.
 * @param args Comma-separated argument names
 * @returns Snippet-formatted string with tab stops (e.g., "${1:arg1}, ${2:arg2}")
 */
const convertArgsToSnippet = (args: string): string => {
  if (!args) {
    return "";
  }

  return args
    .split(",")
    .map((arg) => arg.trim())
    .filter(Boolean)
    .map((arg, index) => `\${${index + 1}:${arg}}`)
    .join(", ");
};

/**
 * Builds a function signature display string.
 * @param args Function arguments
 * @param returnType Optional return type
 * @returns Formatted signature (e.g., "(arg1, arg2) -> str")
 */
const buildSignature = (args: string, returnType?: string): string => {
  const params = args || "";
  const returnPart = returnType ? ` -> ${returnType}` : "";
  return `(${params})${returnPart}`;
};

/**
 * Validates that a completion item has all required fields.
 * @param item The completion item to validate
 * @returns True if the item is valid
 */
const isValidCompletionItem = (
  item: StaticCompletionItem | null | undefined,
): item is StaticCompletionItem => {
  return Boolean(item && item.functionName);
};

/**
 * Creates Monaco completion items from static completion item data.
 * @param items Array of static completion items
 * @param range The text range where the completion will be inserted
 * @returns Array of Monaco completion items
 */
const createCompletionSuggestions = (
  items: StaticCompletionItem[],
  range: monaco.IRange,
): monaco.languages.CompletionItem[] => {
  return items.filter(isValidCompletionItem).map((item) => {
    const snippetArgs = convertArgsToSnippet(item.arguments || "");
    const signature = buildSignature(item.arguments, item.returnType);

    return {
      label: {
        label: item.functionName,
        detail: signature,
      },
      kind: monaco.languages.CompletionItemKind.Function,
      documentation: {
        value: item.description || "",
        isTrusted: true,
        supportHtml: true,
      },
      insertText: `${item.functionName}(${snippetArgs})`,
      insertTextRules:
        monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet,
      range,
    };
  });
};

/**
 * Registers a static completion item provider with Monaco.
 * This provider supplies custom function completions based on the provided items.
 *
 * @param language The programming language identifier for the provider
 * @param items Array of static completion items to provide
 * @returns Disposable to unregister the provider
 */
export const registerStaticCompletionProvider = (
  language: string,
  items: StaticCompletionItem[],
): monaco.IDisposable => {
  return monaco.languages.registerCompletionItemProvider(language, {
    provideCompletionItems: (model, position) => {
      const word = model.getWordUntilPosition(position);
      const range = {
        startLineNumber: position.lineNumber,
        startColumn: word.startColumn,
        endLineNumber: position.lineNumber,
        endColumn: word.endColumn,
      };

      const suggestions = createCompletionSuggestions(items, range);

      return { suggestions };
    },
  });
};
