import type { UISchemaElement } from "@jsonforms/core";

import type { UiSchemaWithProvidedOptions } from "@knime/jsonforms";

import type {
  BackendType,
  MultiFileSelection,
} from "../uiComponents/fileChooser/types";

export interface ConnectedFSOptions {
  /**
   * The name of the connected file system
   */
  fileSystemType: string;
  /**
   * The fsSpecifier of the connected file system
   */
  fileSystemSpecifier?: string;
  /**
   * true whenever there exists a port connection but the file connection could not be established.
   */
  fileSystemConnectionMissing?: true;
  /**
   * The index of the port this file chooser is connected to.
   */
  portIndex: number;
}

export interface SpaceFSOptions {
  /**
   * The mount ID to use/show for space file system access
   */
  mountId?: string;
  /**
   * The path within the space to start browsing from
   */
  spacePath?: string;
}

export interface ReaderOptions {
  /**
   * File extension for FILE selection. Used to filter selectable files.
   */
  fileExtensions?: string[];
}
export interface FileChooserOptionsBase extends ReaderOptions {
  /**
   * Not actually set statically in the options but rather provided dynamically via state provider mechanism.
   * Listed here for ts typing completeness.
   * The file system ID for custom file system connections
   */
  fileSystemId?: BackendType;
  /**
   * Whether local file browsing is allowed
   */
  isLocal?: boolean;
  /**
   * Options specific to connected file systems. Required if CONNECTED file system is used.
   */
  connectedFSOptions?: ConnectedFSOptions;
  /**
   * Options specific to space file systems. Required if SPACE file system is used.
   */
  spaceFSOptions?: SpaceFSOptions;
}

export interface WriterOptions {
  /**
   * Whether this is a writer widget
   */
  isWriter?: true; // default false
  /**
   * File extension for FILE selection. Used to append
   * the extension if not present in the user input.
   */
  fileExtension?: string;
}

/**
 * File system options matching the backend FileSystemOption enum
 */
export type FileSystemOption =
  | "LOCAL"
  | "SPACE"
  | "EMBEDDED"
  | "CUSTOM_URL"
  | "CONNECTED";

// "WORKFLOW" omitted for now
export type FileSelectionMode = "FILE" | "FOLDER";

// "WORKFLOW" omitted for now
export type MultiFileFilterMode = MultiFileSelection["filterMode"];

type SingleSelectionOptionsBase = FileChooserOptionsBase &
  WriterOptions & {
    /**
     * Whether a file or a folder is to be chosen
     */
    selectionMode?: "FILE" | "FOLDER"; // default "FILE"
  };
/**
 * Options for String-based file selection using the new API
 */
export type StringFileChooserOptions = SingleSelectionOptionsBase & {
  /**
   * The file system to use. If null, fileSystemId from provided state will be used.
   */
  fileSystem: FileSystemOption | null;
};

export interface FileSystemsOptions {
  /**
   * Available file systems. If not provided, all file systems are available.
   */
  fileSystems?: FileSystemOption[]; // default: all
}

/**
 * Options for FileSelection-based file selection using the new API
 */
export type FileChooserOptions = SingleSelectionOptionsBase &
  FileSystemsOptions;

/**
 * Options for MultiFileSelection-based file selection using the new API
 */
export type MultiFileChooserOptions = FileChooserOptionsBase &
  FileSystemsOptions & {
    possibleFilterModes: MultiFileFilterMode[];
    filters: {
      classId: string;
      uiSchema: UISchemaElement;
    };
  };

export type StringFileChooserUiSchema =
  UiSchemaWithProvidedOptions<StringFileChooserOptions>;
export type FileChooserUiSchema =
  UiSchemaWithProvidedOptions<FileChooserOptions>;
export type MultiFileChooserUiSchema =
  UiSchemaWithProvidedOptions<MultiFileChooserOptions>;
