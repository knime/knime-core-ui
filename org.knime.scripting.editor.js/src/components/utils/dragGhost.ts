type StylePatch = {
  [K in keyof CSSStyleDeclaration]?: CSSStyleDeclaration[K] | null;
};

function applyStyles(element: HTMLElement, styles: StylePatch): void {
  Object.assign(element.style, styles);
}

const createBadge = (numSelectedItems: number) => {
  const badge = document.createElement("div");
  const MAX_COUNT = 99;
  badge.innerText =
    numSelectedItems <= MAX_COUNT ? numSelectedItems.toString() : "99+";
  const badgeStyles: Partial<CSSStyleDeclaration> = {
    background: "var(--knime-masala)",
    color: "var(--knime-white)",
    fontSize: "13px",
    lineHeight: "11px",
    position: "absolute",
    display: "flex",
    justifyContent: "center",
    alignItems: "center",
    right: "-10px",
    top: "-10px",
    borderRadius: "50%",
    width: "30px",
    height: "30px",
    padding: "5px",
    pointerEvents: "none",
  };
  applyStyles(badge, badgeStyles);
  return badge;
};

const createDragGhostContent = (
  elements: {
    dragGhostContent: HTMLElement;
  }[],
) => {
  const dragGhostContent = elements.map(({ dragGhostContent }) => {
    const flex = `${Math.floor(100 / elements.length)}%`;
    applyStyles(dragGhostContent, {
      flex,
      padding: "var(--space-4) var(--space-8)",
      translate: "-var(--space-4)",
    });
    return dragGhostContent;
  });
  return dragGhostContent;
};

export const createDragGhost = ({
  elements,
  numSelectedItems,
  font,
}: {
  elements: { dragGhostContent: HTMLElement }[];
  numSelectedItems: number;
  font?: string;
}) => {
  const ghostDiv = document.createElement("div");
  ghostDiv.id = "drag-ghost";
  applyStyles(ghostDiv, {
    position: "absolute",
    top: "-1000px",
    width: "min-content",
    textWrap: "nowrap",
    backgroundColor: "var(--knime-cornflower-semi)",
    color: "var(--knime-masala)",
    borderRadius: "30px",
    display: "flex",
    fontSize: "11px",
    flexDirection: "row",
    ...(font ? { "font-family": font } : {}),
  });
  document.body.appendChild(ghostDiv);
  const dragGhostContent = createDragGhostContent(elements);
  dragGhostContent.forEach((childDiv) => {
    ghostDiv.appendChild(childDiv);
  });
  if (numSelectedItems > 1) {
    const badge = createBadge(numSelectedItems);
    ghostDiv.appendChild(badge);
  }
  return ghostDiv;
};

export const removeDragGhost = () => {
  const dragGhost = document.getElementById("drag-ghost");
  if (dragGhost?.parentNode) {
    dragGhost.parentNode.removeChild(dragGhost);
  }
};
