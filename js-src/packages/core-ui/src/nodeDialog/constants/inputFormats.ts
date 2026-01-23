import type { Tester } from "@jsonforms/core";

export const inputFormats = {
  button: "button",
  credentials: "credentials",
  legacyCredentials: "legacyCredentials",
  fileChooser: "fileChooser",
  stringFileChooser: "stringFileChooser",
  multiFileChooser: "multiFileChooser",
  dynamicValue: "dynamicValue",
  dynamicInput: "dynamicInput",
  custom: "custom",
} as const;

// union type of the values of the inputFormats object:
export type InputFormat = (typeof inputFormats)[keyof typeof inputFormats];

export const hasFormat =
  (format: InputFormat): Tester =>
  (uischema) =>
    uischema.options?.format === format;
