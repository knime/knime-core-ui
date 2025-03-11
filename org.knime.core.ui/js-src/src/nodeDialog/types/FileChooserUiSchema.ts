export interface FileChooserOptions {
  isWriter?: boolean;
  isLocal?: boolean;
  portIndex?: number;
  fileSystemType?: string;
  fileSystemSpecifier?: string;
  /**
   * true whenever there exists a portIndex but a connection could not be established
   */
  fileSystemConnectionMissing?: true;
  fileExtension?: string;
  fileExtensions?: string[];
  fileExtensionProvider?: string;
  mountId?: string;
  spacePath?: string;
  /**
   * Wether a file or a folder is to be chosen. FILE is the default.
   */
  selectionMode?: "FILE" | "FOLDER";
}

export type FileChooserUiSchema = {
  options?: FileChooserOptions;
};
