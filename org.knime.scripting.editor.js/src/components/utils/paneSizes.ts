export const MIN_WIDTH_FOR_DISPLAYING_PANES = 576;
export const MIN_WIDTH_FOR_DISPLAYING_LEFT_PANE = 992;
export const MIN_WIDTH_FOR_SHOWING_BUTTON_TEXT = 400;

export type PaneSizes = {
  [key in "left" | "right" | "bottom"]: number;
};
