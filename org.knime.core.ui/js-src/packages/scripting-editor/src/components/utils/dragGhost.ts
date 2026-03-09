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
    backgroundColor: "var(--kds-color-background-selected-initial)",
    color: "var(--kds-color-text-and-icon-selected)",
    font: "var(--kds-font-base-title-xsmall)",
    lineHeight: "11px",
    position: "absolute",
    display: "flex",
    justifyContent: "center",
    alignItems: "center",
    right: "-8px",
    top: "-8px",
    border: "var(--kds-border-base-subtle)",
    borderRadius: "var(--kds-border-radius-container-pill)",
    width: "var(--kds-dimension-component-width-1x)",
    height: "var(--kds-dimension-component-height-1x)",
    paddingRight: "var(--kds-spacing-container-0-12x)",
    paddingLeft: "var(--kds-spacing-container-0-12x)",
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
    height: "var(--kds-dimension-component-height-1x)",
    paddingLeft: "var(--kds-spacing-container-0-25x)",
    paddingRight: "var(--kds-spacing-container-0-25x)",
    gap: "var(--kds-spacing-container-0-12x)",
    backgroundColor: "var(--kds-color-background-selected-initial)",
    color: "var(--kds-color-text-and-icon-selected)",
    borderRadius: "var(--kds-border-radius-container-0-31x)",
    boxShadow: "var(--kds-shadow-elevation-1)",
    font: font ?? "var(--kds-font-base-interactive-small)",
    display: "flex",
    flexDirection: "row",
    alignItems: "center",
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
