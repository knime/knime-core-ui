import { createApp } from "vue";
import { Consola, LogLevels } from "consola";

import { LoadingApp } from "../lib/loading";
import { initMocked } from "../src/init";

import App from "./App.vue";

// Show loading app while initializing
const loadingApp = createApp(LoadingApp);
loadingApp.mount("#app");

const setupConsola = () => {
  const consola = new Consola({
    level: LogLevels.trace,
  });
  const globalObject = typeof global === "object" ? global : window;

  (globalObject as any).consola = consola;
};

setupConsola();

// Real apps would initialize services here:
// if (import.meta.env.MODE === "development.browser") {
//   initMocked((await import("./__mocks__/mock-services")).default)
// } else {
//   await init();
// }
initMocked((await import("./mock-services")).default);

// Simulate some loading time
const DEMO_LOADING_TIME = 0; // off by default
await new Promise((resolve) => setTimeout(resolve, DEMO_LOADING_TIME));

loadingApp.unmount();
createApp(App).mount("#app");
