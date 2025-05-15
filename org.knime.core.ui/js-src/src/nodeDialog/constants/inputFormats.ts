import type { Tester } from "@jsonforms/core";

export const inputFormats = {
  button: "button",
  credentials: "credentials",
  dbTableChooser: "dbTableChooser",
  legacyCredentials: "legacyCredentials",
  localFileChooser: "localFileChooser",
  fileChooser: "fileChooser",
  multiFileChooser: "multiFileChooser",
  dynamicValue: "dynamicValue",
} as const;

// union type of the values of the inputFormats object:
export type InputFormat = (typeof inputFormats)[keyof typeof inputFormats];

export const hasFormat =
  (format: InputFormat): Tester =>
  (uischema) =>
    uischema.options?.format === format;
