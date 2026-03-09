import { ref } from "vue";
import { useDebounceFn, useResizeObserver } from "@vueuse/core";

import useBoolean from "@/tableView/utils/useBoolean";
import type Tile from "../Tile.vue";

const TILE_HORIZONTAL_PADDING = 12;
const TILE_BORDER_WIDTH = 1;
const DEBOUNCE_DELAY = 150;

export const useTileResize = () => {
  const isResizeActive = useBoolean();
  const tileWidth = ref(0);
  const firstTileRef = ref<InstanceType<typeof Tile>>();

  const debouncedSetResizeInactive = useDebounceFn(
    () => isResizeActive.setFalse(),
    DEBOUNCE_DELAY,
  );

  useResizeObserver(
    () => firstTileRef.value?.$el,
    (entries) => {
      isResizeActive.setTrue();
      tileWidth.value =
        entries[0].target.clientWidth -
        2 * TILE_HORIZONTAL_PADDING -
        2 * TILE_BORDER_WIDTH;
      debouncedSetResizeInactive();
    },
  );

  return { tileWidth, isResizeActive, firstTileRef };
};
