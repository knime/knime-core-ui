import { vi } from "vitest";
import { nextTick } from "vue";
import type { VueWrapper } from "@vue/test-utils";
import { JsonForms } from "@jsonforms/vue";
import flushPromises from "flush-promises";

/**
 * We are using dynamic imports for all renderers which need to be settled before testing.
 * Since we have a workaround in place which awaits these imports every time a control is rendered,
 * we unfortunately need to await once for each used renderer (even twice for the same renderer).
 */
export const dynamicImportsSettled = async (wrapper: VueWrapper) => {
  while (!wrapper.findComponent(JsonForms).exists()) {
    await flushPromises();
    await nextTick();
    await vi.dynamicImportSettled();
  }
};
