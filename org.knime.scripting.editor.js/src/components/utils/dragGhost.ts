const applyStyles = (
  element: HTMLElement,
  styles: Partial<CSSStyleDeclaration>,
): void => {
  Object.entries(styles).forEach(([property, value]) => {
    // @ts-ignore
    element.style[property] = value;
  });
};

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
    text: string;
  }[],
) => {
  const dragGhostContent = elements.map(({ text }) => {
    const el = document.createElement("div");
    el.innerText = text;
    const flex = `${Math.floor(100 / elements.length)}%`;
    applyStyles(el, {
      flex,
      padding: "var(--space-4) var(--space-8)",
      translate: "-var(--space-4)",
    });
    return el;
  });
  return dragGhostContent;
};

export const createDragGhost = ({
  width,
  elements,
  numSelectedItems,
  font,
}: {
  width: string;
  elements: { text: string }[];
  numSelectedItems: number;
  font?: string;
}) => {
  const ghostDiv = document.createElement("div");
  ghostDiv.id = "drag-ghost";
  applyStyles(ghostDiv, {
    position: "absolute",
    top: "-1000px",
    width,
    backgroundColor: "var(--knime-cornflower-semi)",
    color: "var(--knime-cornflower-dark)",
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
