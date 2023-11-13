export interface Item {
  isDirectory: boolean;
  /**
   * null in case of root directory
   */
  name: string;
}

export interface Folder {
  items: Item[];
  path: string | null;
}