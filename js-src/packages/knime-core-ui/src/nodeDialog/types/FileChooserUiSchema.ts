import type { UiSchemaWithProvidedOptions } from "@knime/jsonforms";

import type { BackendType } from "../uiComponents/fileChooser/types";

export type FileChooserOptions = {
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
  /**
   * The file system ID for custom file system connections
   */
  fileSystemId?: BackendType;
  /**
   * When true, only shows the connected file system tab in the file chooser browser
   */
  allowOnlyConnectedFS?: boolean;
};

export type FileChooserUiSchema =
  UiSchemaWithProvidedOptions<FileChooserOptions>;
