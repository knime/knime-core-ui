import { createApp } from "vue";
import { Consola, LogLevels } from "consola";

import { useKdsLegacyMode } from "@knime/kds-components";

import { LoadingApp } from "../lib/loading";
import { init, initMocked } from "../src/init";

import App from "./App.vue";

useKdsLegacyMode(true);

// Show loading app while initializing
const loadingApp = createApp(LoadingApp);
loadingApp.mount("#app");

const setupConsola = () => {
  const consola = new Consola({
    level: LogLevels.trace,
  });
  const globalObject = typeof global === "object" ? global : window;

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  (globalObject as any).consola = consola;
};

setupConsola();

// Initialize services here
if (import.meta.env.MODE === "development.browser") {
  initMocked((await import("./browser-mock-services")).default);
} else {
  await init();
}

loadingApp.unmount();
createApp(App).mount("#app");

// Mount debug toolbar in development mode
if (import.meta.env.DEV) {
  const { default: DebugToolbar } = await import("./DebugToolbar.vue");
  const debugToolbarDiv = document.createElement("div");
  debugToolbarDiv.id = "debug-toolbar";
  document.body.appendChild(debugToolbarDiv);
  createApp(DebugToolbar).mount("#debug-toolbar");
}
