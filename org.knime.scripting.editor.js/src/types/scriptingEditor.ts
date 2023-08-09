export type PaneSizes = {
  [key in "left" | "right" | "bottom"]: number;
};

export enum RightPaneLayout {
  FIXED = "fixed",
  RELATIVE = "relative",
}
