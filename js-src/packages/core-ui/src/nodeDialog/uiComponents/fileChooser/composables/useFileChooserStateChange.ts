import { type Ref } from "vue";

import { useProvidedState } from "@knime/jsonforms";

import { type FileChooserUiSchema } from "../../../types/FileChooserUiSchema";
import { mergeDeep } from "../../../utils";
import type { FSCategory, FileChooserValue } from "../types/FileChooserProps";

export default (
  currentValue: Ref<FileChooserValue>,
  onChange: (value: FileChooserValue) => void,
  uischema: Ref<FileChooserUiSchema>,
) => {
  const connectedFSOptions = useProvidedState(uischema, "connectedFSOptions");

  const onPathUpdate = (path: string) => {
    const fsSpecifier = connectedFSOptions.value?.fileSystemSpecifier;
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
