import { describe, expect, it } from "vitest";
import { mount } from "@vue/test-utils";

import FileSelectionPreview from "../FileSelectionPreview.vue";
import type { PreviewResult } from "../composables/useFileFilterPreviewBackend";

const DEFAULT_SUCCESS_DATA: PreviewResult = {
  resultType: "SUCCESS",
  numFilesAfterFilteringIsOnlyLowerBound: false,
  numFilesBeforeFilteringIsOnlyLowerBound: false,
  itemsAfterFiltering: ["test1.txt", "test2.txt"],
  numItemsBeforeFiltering: 3,
};

const doMount = ({
  previewData = DEFAULT_SUCCESS_DATA,
  isLoading = false,
  expandByDefault = true,
}: {
  previewData?: PreviewResult;
  isLoading?: boolean;
  expandByDefault?: boolean;
}) => {
  return mount(FileSelectionPreview, {
    props: {
      previewData,
      isLoading,
      expandByDefault,
    },
  });
};

describe("FileSelectionPreview", () => {
  it("renders the preview data correctly", () => {
    const wrapper = doMount({});

    expect(wrapper.findAll(".visible-item").length).toBe(2);
    expect(wrapper.findAll(".visible-item")[0].text()).toBe("test1.txt");
    expect(wrapper.findAll(".visible-item")[1].text()).toBe("test2.txt");
  });

  it("displays sensible header text when all items are shown", () => {
    const wrapper = doMount({
      previewData: {
        ...DEFAULT_SUCCESS_DATA,
        numItemsBeforeFiltering: 2,
      },
    });

    expect(wrapper.find(".header-text").text()).toBe("2 of 2 files");
  });

  it("displays sensible header text when not all items are shown", () => {
    const wrapper = doMount({
      previewData: {
        ...DEFAULT_SUCCESS_DATA,
        numItemsBeforeFiltering: 3,
      },
    });

    expect(wrapper.find(".header-text").text()).toBe("2 of 3 files");
  });

  it("displays sensible header text when items are filtered out", () => {
    const wrapperWithPostLowerBound = doMount({
      previewData: {
        ...DEFAULT_SUCCESS_DATA,
        numFilesAfterFilteringIsOnlyLowerBound: true,
      },
    });
    expect(wrapperWithPostLowerBound.find(".header-text").text()).toBe(
      "2+ of 3 files",
    );

    const wrapperWithPreLowerBound = doMount({
      previewData: {
        ...DEFAULT_SUCCESS_DATA,
        numFilesBeforeFilteringIsOnlyLowerBound: true,
      },
    });
    expect(wrapperWithPreLowerBound.find(".header-text").text()).toBe(
      "2 of 3+ files",
    );

    const wrapperWithBothLowerBounds = doMount({
      previewData: {
        ...DEFAULT_SUCCESS_DATA,
        numFilesAfterFilteringIsOnlyLowerBound: true,
        numFilesBeforeFilteringIsOnlyLowerBound: true,
      },
    });
    expect(wrapperWithBothLowerBounds.find(".header-text").text()).toBe(
      "2+ of 3+ files",
    );
  });

  it("displays the loading state", () => {
    const wrapper = doMount({ isLoading: true });

    expect(wrapper.find(".loading-spinner").exists()).toBeTruthy();
  });

  it("respects the expandByDefault prop", () => {
    const collapsedWrapper = doMount({ expandByDefault: false });
    expect(collapsedWrapper.find(".visible-item").exists()).toBeFalsy();

    const expandedWrapper = doMount({ expandByDefault: true });
    expect(expandedWrapper.find(".visible-item").exists()).toBeTruthy();
  });

  it("displays the error message", () => {
    const wrapper = doMount({
      previewData: {
        resultType: "ERROR",
        errorMessage: "Test error message",
      },
    });

    expect(wrapper.find("[data-test-id='error-message']").text()).toBe(
      "Test error message",
    );

    const wrapperWithNoError = doMount({});
    expect(
      wrapperWithNoError.find("[data-test-id='error-message']").exists(),
    ).toBe(false);
  });

  it("displays the empty state", () => {
    const wrapper = doMount({
      previewData: {
        ...DEFAULT_SUCCESS_DATA,
        itemsAfterFiltering: [],
      },
    });

    expect(wrapper.find(".empty-list").exists()).toBeTruthy();
    expect(wrapper.find(".empty-list").text()).toBe("No files");
  });
});
