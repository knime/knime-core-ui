import { describe, expect, it } from "vitest";

import { determineRenderer } from "./setup";

describe("dbTableChooserRenderer", () => {
  it("detects dbTableChooser format", () => {
    const uiSchema = {
      type: "Control",
      scope: "#/properties/myDBTableChooser",
      options: {
        format: "dbTableChooser",
      },
    };

    expect(determineRenderer(uiSchema, {})).toBe("DBTableChooserControl");
  });
});
