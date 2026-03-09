import { type Ref, computed, onMounted, ref, watch } from "vue";

import { useProvidedState } from "@knime/jsonforms";

import {
  type FileChooserOptionsBase,
  type FileChooserUiSchema,
  type FileSystemOption,
  type FileSystemsOptions,
} from "../../../types/FileChooserUiSchema";
import { FSCategory } from "../types/FileChooserProps";

const toSentenceCase = (str: string) =>
  str.charAt(0).toUpperCase() + str.slice(1).toLowerCase();

export const useFileSystems = (
  uiSchemaOptions: Ref<FileChooserOptionsBase & FileSystemsOptions>,
) => {
  const fileSystems = computed<FileSystemOption[]>(
    () =>
      uiSchemaOptions.value.fileSystems ?? [
        "CONNECTED",
        "LOCAL",
        "SPACE",
        "EMBEDDED",
        "CUSTOM_URL",
      ],
  );
  const isConnected = computed(
    () =>
      Boolean(uiSchemaOptions.value.connectedFSOptions) &&
      fileSystems.value.includes("CONNECTED"),
  );

  const isConnectedButNoFileConnectionIsAvailable = computed(
    () =>
      isConnected.value &&
      uiSchemaOptions.value.connectedFSOptions?.fileSystemConnectionMissing,
  );

  const isLocal = computed(
    () => uiSchemaOptions.value.isLocal && fileSystems.value.includes("LOCAL"),
  );
  const validCategories = computed<(keyof typeof FSCategory)[]>(() =>
    isConnected.value
      ? ["CONNECTED"]
      : [
          ...(isLocal.value ? ["LOCAL" as const] : []),
          ...(fileSystems.value.includes("SPACE")
            ? [
                "relative-to-current-hubspace" as const,
                "relative-to-workflow" as const,
              ]
            : []),
          ...(fileSystems.value.includes("EMBEDDED")
            ? ["relative-to-embedded-data" as const]
            : []),
          ...(fileSystems.value.includes("CUSTOM_URL")
            ? ["CUSTOM_URL" as const]
            : []),
        ],
  );

  return {
    isLocal,
    validCategories,
    fileSystems,
    isConnected,
    isConnectedButNoFileConnectionIsAvailable,
  };
};

export const useFileChooserBrowseOptions = (
  uischema: Ref<FileChooserUiSchema>,
) => {
  const connectedFSOptions = useProvidedState(uischema, "connectedFSOptions");
  // Merge connectedFSOptions from state provider into options
  const tabsCanBeRendered = ref(false);
  watch(
    connectedFSOptions,
    (newVal) => {
      if (newVal) {
        tabsCanBeRendered.value = true;
      }
    },
    { immediate: true },
  );
  const options = computed(() => ({
    ...(uischema.value.options ?? {}),
    connectedFSOptions:
      connectedFSOptions.value ?? uischema.value.options?.connectedFSOptions,
  }));
  const { isLocal, isConnected, fileSystems } = useFileSystems(options);
  const filteredExtensions = ref<string[]>([]);
  const appendedExtension = ref<string | null>(null);
  const isWriter = computed(() => options.value.isWriter);
  const mountId = computed(
    () => options.value.spaceFSOptions?.mountId ?? "Current space",
  );
  const spacePath = computed(() => options.value.spaceFSOptions?.spacePath);
  const relativeWorkflowPath = computed(
    () => options.value.spaceFSOptions?.relativeWorkflowPath,
  );
  const portIndex = computed(() => connectedFSOptions.value?.portIndex);
  const portFileSystemName = computed(() =>
    toSentenceCase(
      connectedFSOptions.value?.fileSystemType ?? "Connected file system",
    ),
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
    if (!uischema.value.providedOptions?.includes("connectedFSOptions")) {
      tabsCanBeRendered.value = true;
    }
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

  return {
    filteredExtensions,
    appendedExtension,
    isWriter,
    isLoaded,
    tabsCanBeRendered,
    isLocal,
    mountId,
    spacePath,
    relativeWorkflowPath,
    isConnected,
    portIndex,
    portFileSystemName,
    fileSystems,
  };
};
