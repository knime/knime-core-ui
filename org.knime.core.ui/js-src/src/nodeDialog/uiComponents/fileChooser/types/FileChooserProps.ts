export enum FSCategory {
  LOCAL,
  CUSTOM_URL,
  "relative-to-current-hubspace",
}

export interface FileChooserValue {
  path: string;
  timeout: number;
  fsCategory: keyof typeof FSCategory;
  context?: {
    fsToString: string;
  };
}

interface FileChooserProps {
  modelValue: FileChooserValue;
  disabled: boolean;
  id: string | null;
  browseOptions?: {
    isWriter?: boolean;
    fileExtension?: string;
    fileExtensions?: string[];
  };
}

export default FileChooserProps;
