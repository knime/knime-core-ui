import { type ComputedRef, type Ref, computed, ref } from "vue";

import type { BackendType } from "../types";
import type { FileChooserValue } from "../types/FileChooserProps";

export const useTrackingOfPathsPerTab = ({
  backendType,
  modelValue,
}: {
  backendType: ComputedRef<BackendType>;
  modelValue: Ref<FileChooserValue>;
}) => {
  const initialBackendType: BackendType | "CUSTOM_URL" =
    modelValue.value.fsCategory === "CUSTOM_URL"
      ? "CUSTOM_URL"
      : backendType.value;
  const perTabPaths = ref<Record<string, string>>({
    [initialBackendType]: modelValue.value.path,
  });
  const initialFilePathForCurrentTab = computed(
    () => perTabPaths.value[backendType.value] ?? "",
  );
  const handleNavigate = (currentPath: string | null) => {
    perTabPaths.value[backendType.value] = currentPath ?? "";
  };

  return {
    initialFilePathForCurrentTab,
    handleNavigate,
  };
};
