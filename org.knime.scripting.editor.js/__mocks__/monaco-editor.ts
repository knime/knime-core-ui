import { vi } from "vitest";

export const editor = {
  createModel: vi.fn(() => ({
    getValue: vi.fn(() => "myInitialScript"),
  })),
  create: vi.fn((element: HTMLElement) => {
    element.innerHTML = "SCRIPTING EDITOR MOCK";
    return "myEditor";
  }),
};

export const Uri = {
  parse: vi.fn((path: string) => path),
};
