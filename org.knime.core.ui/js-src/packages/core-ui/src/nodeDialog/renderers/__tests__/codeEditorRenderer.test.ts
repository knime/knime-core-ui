import { describe, expect, it } from "vitest";

import { determineRenderer } from "./setup";

describe("CodeEditorControl", () => {
  const schema = {
    type: "object",
    properties: {
      codeEditor: {
        type: "string",
      },
    },
  };

  it("does not match without format option", () => {
    const uiSchema = {
      type: "Control",
      scope: "#/properties/codeEditor",
    };

    expect(determineRenderer(uiSchema, schema)).toBe("TextControl");
  });

  it("matches with codeEditor format option", () => {
    const uiSchema = {
      type: "Control",
      scope: "#/properties/codeEditor",
      options: {
        format: "codeEditor",
      },
    };

    expect(determineRenderer(uiSchema, schema)).toBe("CodeEditorControl");
  });

  it("matches with codeEditor format and language option", () => {
    const uiSchema = {
      type: "Control",
      scope: "#/properties/codeEditor",
      options: {
        format: "codeEditor",
        language: "json",
      },
    };

    expect(determineRenderer(uiSchema, schema)).toBe("CodeEditorControl");
  });
});
