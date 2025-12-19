import * as monaco from "monaco-editor";

/**
 * A static completion item for Monaco editor autocomplete.
 *
 * Items are displayed as either functions or constants based on whether arguments are provided:
 * - **Function**: Include `arguments` (comma-separated) and optionally `returnType`
 *   - Example: `name: "get_data"`, `arguments: "port, index"`, `returnType: "DataFrame"`
 *   - Displays as: `get_data(port, index) -> DataFrame`
 * - **Constant/Variable**: Omit `arguments`, optionally include `returnType`
 *   - Example: `name: "knime_context"`, `returnType: "Context"`
 *   - Displays as: `knime_context: Context`
 *
 * @property name - The name to display and insert (required)
 * @property arguments - Comma-separated parameter names for functions (optional)
 * @property description - Help text shown in completion details, supports HTML (required)
 * @property returnType - Return type annotation displayed in the completion list (optional)
 */
export type StaticCompletionItem = {
  name: string;
  arguments?: string;
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
  return Boolean(item && item.name);
};

/**
 * Creates Monaco completion items from static completion item data (without range).
 * @param items Array of static completion items
 * @returns Array of Monaco completion items without range property
 */
const createCompletionSuggestionsWithoutRange = (
  items: StaticCompletionItem[],
): Array<Omit<monaco.languages.CompletionItem, "range">> => {
  return items.filter(isValidCompletionItem).map((item) => {
    const isFunction = item.arguments !== undefined;

    if (isFunction) {
      // Function completion
      const snippetArgs = convertArgsToSnippet(item.arguments || "");
      const signature = buildSignature(item.arguments || "", item.returnType);

      return {
        label: {
          label: item.name,
          detail: signature,
        },
        kind: monaco.languages.CompletionItemKind.Function,
        documentation: {
          value: item.description || "",
          isTrusted: true,
          supportHtml: true,
        },
        insertText: `${item.name}(${snippetArgs})`,
        insertTextRules:
          monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet,
      };
    } else {
      // Constant/variable completion
      const typeHint = item.returnType ? `: ${item.returnType}` : "";

      return {
        label: {
          label: item.name,
          detail: typeHint,
        },
        kind: monaco.languages.CompletionItemKind.Constant,
        documentation: {
          value: item.description || "",
          isTrusted: true,
          supportHtml: true,
        },
        insertText: item.name,
      };
    }
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
  const cachedSuggestions = createCompletionSuggestionsWithoutRange(items);

  return monaco.languages.registerCompletionItemProvider(language, {
    provideCompletionItems: (model, position) => {
      const word = model.getWordUntilPosition(position);
      const range = {
        startLineNumber: position.lineNumber,
        startColumn: word.startColumn,
        endLineNumber: position.lineNumber,
        endColumn: word.endColumn,
      };

      // Add range to pre-computed suggestions
      const suggestions = cachedSuggestions.map((item) => ({
        ...item,
        range,
      }));

      return { suggestions };
    },
  });
};
