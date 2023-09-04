import { mount } from "@vue/test-utils";
import { describe, expect, it, vi } from "vitest";
import InputOutputItem, { type InputOutputModel } from "../InputOutputItem.vue";
import Collapser from "webapps-common/ui/components/Collapser.vue";
import { scriptingServiceMock } from "@/__mocks__/scripting-service";

vi.mock("monaco-editor");
vi.mock("@/scripting-service");

describe("InputOutputItem", () => {
  const inputOutputItemMinimal: InputOutputModel = {
    name: "mocked item",
  };

  const inputOutputItemWithRowsAndAlias: InputOutputModel = {
    name: "supermock",
    codeAlias: "super.mock",
    subItems: [
      { name: "row 1", type: "String", codeAlias: "alias 1" },
      { name: "row 2", type: "Double", codeAlias: "alias 2" },
    ],
  };

  describe("with collapser", () => {
    it("renders collapser if item contains rows / columnInfo", () => {
      const wrapper = mount(InputOutputItem, {
        props: { inputOutputItem: inputOutputItemWithRowsAndAlias },
      });
      expect(wrapper.findComponent(Collapser).exists()).toBeTruthy();
    });

    it("renders codeAlias in title if it exists", () => {
      const wrapper = mount(InputOutputItem, {
        props: {
          inputOutputItem: inputOutputItemWithRowsAndAlias,
        },
      });
      expect(
        wrapper.find(".top-card").find(".code-alias").exists(),
      ).toBeTruthy();
    });

    it("pastes codeAlias to editor when clicking on title", () => {
      const wrapper = mount(InputOutputItem, {
        props: {
          inputOutputItem: inputOutputItemWithRowsAndAlias,
        },
      });
      const codeAliasInTitle = wrapper.find(".top-card").find(".code-alias");
      codeAliasInTitle.trigger("click");
      expect(scriptingServiceMock.pasteToEditor).toHaveBeenCalledWith(
        inputOutputItemWithRowsAndAlias.codeAlias,
      );
    });

    it("pastes codeAlias to editor when clicking on subItem", () => {
      const wrapper = mount(InputOutputItem, {
        props: {
          inputOutputItem: inputOutputItemWithRowsAndAlias,
        },
      });
      const codeAliasInSubItem = wrapper.findAll(".sub-item")[0];
      codeAliasInSubItem.trigger("click");
      expect(scriptingServiceMock.pasteToEditor).toHaveBeenCalledWith(
        (inputOutputItemWithRowsAndAlias.subItems as any)[0].codeAlias,
      );
    });
  });

  describe("no collapser", () => {
    it("does not render collapser if item contains no subitems", () => {
      const wrapper = mount(InputOutputItem, {
        props: { inputOutputItem: inputOutputItemMinimal },
      });
      expect(wrapper.findComponent(Collapser).exists()).toBeFalsy();
    });

    it("renders codeAlias in title if it exists", () => {
      const wrapper = mount(InputOutputItem, {
        props: {
          inputOutputItem: {
            ...inputOutputItemMinimal,
            codeAlias: "myAlias",
          },
        },
      });
      expect(wrapper.find(".code-alias").exists()).toBeTruthy();
    });

    it("pastes codeAlias to editor on click", () => {
      const wrapper = mount(InputOutputItem, {
        props: {
          inputOutputItem: {
            ...inputOutputItemMinimal,
            codeAlias: "myAlias",
          },
        },
      });
      const codeAlias = wrapper.find(".code-alias");
      codeAlias.trigger("click");
      expect(scriptingServiceMock.pasteToEditor).toHaveBeenCalledWith(
        "myAlias",
      );
    });
  });
});
