export const MIN_WIDTH_FOR_DISPLAYING_PANES = 400;
export const MIN_WIDTH_FOR_DISPLAYING_LEFT_PANE = 700;

export type PaneSizes = {
  [key in "left" | "right" | "bottom"]: number;
};
