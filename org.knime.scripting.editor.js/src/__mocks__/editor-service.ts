import { vi } from "vitest";

export const editorServiceMock = {
  initEditorService: vi.fn(() => {}),
  getScript: vi.fn(() => "myScript"),
  getSelectedLines: vi.fn(() => "mySelectedLines"),
  setScript: vi.fn(() => {}),
};
