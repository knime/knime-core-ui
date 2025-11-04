import { afterEach, describe, expect, it } from "vitest";

import { createDragGhost, removeDragGhost } from "../dragGhost";

const createDragGhostContent = (text: string) => {
  const el = document.createElement("div");
  el.innerText = text;
  return { dragGhostContent: el };
};

describe("draghGhost", () => {
  afterEach(() => {
    removeDragGhost();
  });

  it("create drag ghost for single element", () => {
    const dragGhost = createDragGhost({
      elements: [
        { ...createDragGhostContent("elem 1") },
        { ...createDragGhostContent("elem 2") },
      ],
      numSelectedItems: 1,
    });
    const ghost = document.getElementById("drag-ghost");
    expect(ghost).toStrictEqual(dragGhost);
  });

  it("sets drag ghost content", () => {
    createDragGhost({
      elements: [
        { ...createDragGhostContent("elem 1") },
        { ...createDragGhostContent("elem 2") },
      ],
      numSelectedItems: 1,
    });
    const ghost = document.getElementById("drag-ghost");
    const elements = Array.from(ghost!.children);
    expect(elements.length).toBe(2);
    expect((elements[0] as any).innerText).toContain("elem 1");
    expect((elements[1] as any).innerText).toContain("elem 2");
  });

  it("create badge for multiple elements", () => {
    createDragGhost({
      elements: [
        { ...createDragGhostContent("elem 1") },
        { ...createDragGhostContent("elem 2") },
      ],
      numSelectedItems: 10,
    });
    const ghost = document.getElementById("drag-ghost");
    const elements = Array.from(ghost!.children);
    expect(elements.length).toBe(3);
    const badge = elements[2] as any;
    expect(badge.innerText).toBe("10");
  });

  it("remove drag ghost", () => {
    createDragGhost({
      elements: [
        { ...createDragGhostContent("elem 1") },
        { ...createDragGhostContent("elem 2") },
      ],
      numSelectedItems: 10,
    });
    expect(document.getElementById("drag-ghost")).not.toBeNull();
    removeDragGhost();
    expect(document.getElementById("drag-ghost")).toBeNull();
  });
});
