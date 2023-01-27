import * as monaco from 'monaco-editor';
import type { FlowVariable } from '../components/FlowVariables.vue';

const flowVariableCompletion = (objectIdx: number, objectInfo: FlowVariable) => {
    const documentation = `Access the object at index ${objectIdx}.

Current type : \`${objectInfo.name}\`  
Current value : \` ${objectInfo.value} \`
    `;
    return {
        label: objectInfo.name,
        kind: monaco.languages.CompletionItemKind.Snippet,
        documentation: { value: documentation }, // NB: Implement IMarkdownString
        insertText: objectInfo.name
    };
};

export const registerMonacoInputFlowVariableCompletions = (inputFlowVariables: FlowVariable[]) => {
    // Pre-compute the completions but without a range
    type CompletionItemWithoutRange = Omit<monaco.languages.CompletionItem, 'range'>;
    let objectIdx = 0;
    const completions = inputFlowVariables?.flatMap((flowVariable): CompletionItemWithoutRange[] => {
        const objectInfo = flowVariable as FlowVariable;
        return [flowVariableCompletion(objectIdx++, objectInfo)];
    });

    // Register the completion provider using the computed completions
    monaco.languages.registerCompletionItemProvider('python', {
        provideCompletionItems(model, position) {
            const word = model.getWordUntilPosition(position);
            const range = {
                startLineNumber: position.lineNumber,
                endLineNumber: position.lineNumber,
                startColumn: word.startColumn,
                endColumn: word.endColumn
            };
            return { suggestions: completions.map((c) => ({ range, ...c })) };
        }
    });
};

/*
TODO(AP-20083)
export interface ScriptingCompletion {
export const registerMonacoInputColumnCompletions = (completionRule: (input:  ) => ,inputCompletions: ScriptingCompletion[]) => {
*/
