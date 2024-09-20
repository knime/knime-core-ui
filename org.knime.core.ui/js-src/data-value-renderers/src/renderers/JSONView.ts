import JSONWorker from "monaco-editor/esm/vs/language/json/json.worker?worker";
import EditorWorker from "monaco-editor/esm/vs/editor/editor.worker?worker";
import { editor } from "monaco-editor";
import "../styles.css";
import {
  AlertingService,
  JsonDataService,
  SharedDataService,
} from "@knime/ui-extension-service";

self.MonacoEnvironment = {
  getWorker(_workerId: string, label: string): Worker {
    switch (label) {
      case "json":
        return new JSONWorker();
      default:
        return new EditorWorker();
    }
  },
};

export class JSONView {
  editorContainerElement: HTMLElement;
  editorElement: HTMLElement;
  editor!: editor.IStandaloneCodeEditor;
  jsonDataService!: JsonDataService;
  sharedDataService!: SharedDataService;

  constructor(editorContainerElement: HTMLElement, editorElement: HTMLElement) {
    if (!editorContainerElement) {
      const message = "Editor container element not found";
      AlertingService.getInstance().then((service) => {
        service.sendAlert({ message });
      });
      throw new Error(message);
    }
    this.editorContainerElement = editorContainerElement;
    this.editorElement = editorElement;
  }

  async init() {
    this.jsonDataService = await JsonDataService.getInstance();
    const initialData = await this.jsonDataService.initialData();

    this.sharedDataService = await SharedDataService.getInstance();

    this.editor = editor.create(this.editorElement, {
      value: JSON.stringify(initialData, null, 4),
      language: "json",
      readOnly: true,
      scrollBeyondLastLine: false,
      lineNumbersMinChars: 1,
    });

    const { width, height, top, left } = document
      .getElementsByTagName("body")[0]
      .getBoundingClientRect();
    this.editor.layout({
      height: Math.ceil(height) - top,
      width: Math.ceil(width) - left,
    });
  }
}
