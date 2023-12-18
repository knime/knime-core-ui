<script setup lang="ts">
import { uniqueId } from "lodash";
import {
  computed,
  ref,
  watch,
  onMounted,
  type Ref,
  onUnmounted,
  watchEffect,
  nextTick,
} from "vue";
import { fetchImage } from "@/utils/images";
import { ResourceService } from "@knime/ui-extension-service";

const props = defineProps<{
  path: string;
  resourceService: ResourceService;
  height?: number;
  width?: number;
  update?: boolean;
  includeDataInHtml: boolean;
  tableIsReady: boolean;
}>();

const waitForTableToBeReady = () =>
  new Promise<void>((resolve) => {
    watchEffect(() => {
      if (props.tableIsReady) {
        resolve();
      }
    });
  });

const emit = defineEmits(["pending", "rendered"]);
const inlinedSrc: Ref<false | string> = ref(false);

const imageUrl = computed(() =>
  props.resourceService.getResourceUrl(props.path),
);

const urlWithDimensions = computed(() => {
  /**
   * Use Number.Max_VALUE to automatically get the correct height for the given
   * width Handled in the backend
   */
  return props.width
    ? `${imageUrl.value}?w=${Math.floor(props.width)}&h=${Math.floor(
        typeof props.height === "number" ? props.height : Number.MAX_VALUE,
      )}`
    : imageUrl.value;
});

let uuid: string | null = null;
onMounted(async () => {
  if (props.includeDataInHtml) {
    uuid = uniqueId("Image");
    emit("pending", uuid);
    await waitForTableToBeReady();
    inlinedSrc.value = await fetchImage(urlWithDimensions.value);
    // wait until image was rendered in the DOM
    await nextTick();
    emit("rendered", uuid);
  }
});

onUnmounted(() => {
  if (props.includeDataInHtml) {
    emit("rendered", uuid);
  }
});

let fixedSrc: null | string = null;
watch(
  () => props.update,
  (update) => {
    fixedSrc = update ? null : urlWithDimensions.value;
  },
  { immediate: true },
);
</script>

<template>
  <img
    v-if="!includeDataInHtml || inlinedSrc"
    :style="{
      ...(typeof width === 'number' && { maxWidth: width + 'px' }),
      ...(typeof height === 'number' && { maxHeight: height + 'px' }),
    }"
    loading="lazy"
    :src="includeDataInHtml ? inlinedSrc : fixedSrc || urlWithDimensions"
    alt=""
  />
</template>
