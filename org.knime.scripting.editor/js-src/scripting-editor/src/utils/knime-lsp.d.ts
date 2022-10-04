import { MonacoLanguageClient } from 'monaco-languageclient';
import type { DocumentSelector } from 'monaco-languageclient';
import type { ScriptingService, NodeSettings } from './scripting-service';
export declare const startKnimeLanguageClient: (
  scriptingService: ScriptingService<NodeSettings>,
  name: string,
  documentSelector?: string[] | DocumentSelector | undefined
) => MonacoLanguageClient;
