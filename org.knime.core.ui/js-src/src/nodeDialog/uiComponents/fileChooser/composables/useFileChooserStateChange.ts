import { type Ref } from "vue";

import { type FileChooserUiSchema } from "../../../types/FileChooserUiSchema";
import { mergeDeep } from "../../../utils";
import type { FSCategory, FileChooserValue } from "../types/FileChooserProps";

export default (
  currentValue: Ref<FileChooserValue>,
  onChange: (value: FileChooserValue) => void,
  uischema: Ref<FileChooserUiSchema>,
) => {
  const onPathUpdate = (path: string) => {
    const fsSpecifier =
      uischema.value.options?.connectedFSOptions?.fileSystemSpecifier;
    onChange(
      mergeDeep(currentValue.value, {
        path,
        ...(fsSpecifier ? { context: { fsSpecifier } } : {}),
      }),
    );
  };

  const onTimeoutUpdate = (timeout: number) => {
    onChange(mergeDeep(currentValue.value, { timeout }));
  };

  const onFsCategoryUpdate = (fsCategory: keyof typeof FSCategory) => {
    onChange(mergeDeep(currentValue.value, { fsCategory }));
  };

  return { onPathUpdate, onTimeoutUpdate, onFsCategoryUpdate };
};
