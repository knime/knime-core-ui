import { createApp } from "vue";
import { Consola, LogLevels } from "consola";

import { initMocked } from "../src/init";

import App from "./App.vue";

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

const app = createApp(App);
app.mount("#app");
