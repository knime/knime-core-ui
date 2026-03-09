<script setup lang="ts">
import { inject, onMounted, provide, ref } from "vue";
import * as Vue from "vue";

import * as knimeJsonforms from "@knime/jsonforms";
import { type VueControlProps } from "@knime/jsonforms";
import {
  JsonDataService,
  ResourceService,
  type UIExtensionService,
} from "@knime/ui-extension-service";

import { useDynamicImport } from "../useDynamicImport";

type Window = {
  Vue: typeof Vue;
  knimeJsonforms: typeof knimeJsonforms;
};
// Set Vue and JSON Forms as globals for the widget to use
(window as any as Window).Vue = Vue;
(window as any as Window).knimeJsonforms = knimeJsonforms;

const getKnimeService = inject<() => UIExtensionService>("getKnimeService")!;
const shadowRoot = inject<ShadowRoot>("shadowRoot");
const { dynamicImport } = useDynamicImport();

type CustomWidget = {
  default: Vue.Component;
  injectStyles?: (shadowRoot: ShadowRoot) => void;
};
const widget = ref<null | CustomWidget>(null);

const props = defineProps<VueControlProps<unknown>>();

const rpcServiceName = props.control.uischema.options?.rpcServiceName;
if (rpcServiceName) {
  const jsonDataService = new JsonDataService(getKnimeService());
  const rpcProxy = new Proxy(
    {},
    {
      get(_target, prop) {
        if (typeof prop === "string") {
          return (...args: any[]) => {
            return jsonDataService.data({
              method: `${rpcServiceName}.${prop}`,
              options: args,
            });
          };
        }
        return null;
      },
    },
  );
  provide("rpcService", rpcProxy);
}

onMounted(async () => {
  const widgetResource = props.control.uischema.options?.widgetResource;

  const resourceService = new ResourceService(getKnimeService());

  const widgetModule = (await dynamicImport(
    await resourceService.getResourceUrl(
      `uiext/defaultdialog/customWidget/${widgetResource}`,
    ),
  )) as CustomWidget;

  widget.value = widgetModule;

  if (widgetModule.injectStyles && shadowRoot) {
    widgetModule.injectStyles(shadowRoot);
  }
});
</script>

<template>
  <component :is="widget.default" v-if="widget" v-bind="$props" />
</template>
