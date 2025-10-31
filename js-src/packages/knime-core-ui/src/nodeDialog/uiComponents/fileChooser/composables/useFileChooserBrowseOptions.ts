import { type Ref, computed, onMounted, ref, watch } from "vue";

import { useProvidedState } from "@knime/jsonforms";

import { type FileChooserUiSchema } from "../../../types/FileChooserUiSchema";
import { FSCategory } from "../types/FileChooserProps";

export const useFileChooserFileSystemsOptions = (
  uischema: Ref<FileChooserUiSchema>,
) => {
  const isLocal = computed(() => uischema.value.options?.isLocal);
  const isConnected = computed(
    () => typeof uischema.value.options?.portIndex !== "undefined",
  );
  const validCategories = computed<(keyof typeof FSCategory)[]>(() =>
    isConnected.value
      ? ["CONNECTED"]
      : [
          ...(isLocal.value ? ["LOCAL" as const] : []),
          "relative-to-current-hubspace",
          "relative-to-embedded-data",
          "CUSTOM_URL",
        ],
  );

  return { isLocal, isConnected, validCategories };
};

export const useFileChooserBrowseOptions = (
  uischema: Ref<FileChooserUiSchema>,
) => {
  const options = computed(() => uischema.value.options ?? {});
  const { isLocal, isConnected } = useFileChooserFileSystemsOptions(uischema);
  const filteredExtensions = ref<string[]>([]);
  const appendedExtension = ref<string | null>(null);
  const isWriter = computed(() => options.value.isWriter);
  const mountId = computed(() => options.value.mountId ?? "Current space");
  const spacePath = computed(() => options.value.spacePath);
  const portIndex = computed(() => options.value.portIndex);
  const portFileSystemName = computed(
    () => options.value.fileSystemType ?? "Connected File System",
  );
  const isLoaded = ref(false);

  const setFileExtension = (fileExtension: string) => {
    filteredExtensions.value = [fileExtension];
    appendedExtension.value = fileExtension;
    isLoaded.value = true;
  };
  const fileExtension = useProvidedState(uischema, "fileExtension");
  watch(
    fileExtension,
    (newFileExtension) => {
      if (newFileExtension) {
        setFileExtension(newFileExtension);
      }
    },
    {
      immediate: true,
    },
  );

  onMounted(() => {
    const { fileExtension, fileExtensions } = options.value;
    if (
      !fileExtension &&
      !uischema.value.providedOptions?.includes("fileExtension") &&
      !fileExtensions
    ) {
      isLoaded.value = true;
      return;
    }
    if (fileExtensions) {
      filteredExtensions.value = fileExtensions;
      isLoaded.value = true;
    }
  });

  const allowOnlyConnectedFS = computed(
    () => options.value.allowOnlyConnectedFS,
  );

  return {
    filteredExtensions,
    appendedExtension,
    isWriter,
    isLoaded,
    isLocal,
    mountId,
    spacePath,
    isConnected,
    portIndex,
    portFileSystemName,
    allowOnlyConnectedFS,
  };
};
