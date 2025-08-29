import { createApp } from "vue";
import { Consola, LogLevels } from "consola";

import "./mock-services";
import App from "./App.vue";

const setupConsola = () => {
  const consola = new Consola({
    level: LogLevels.trace,
  });
  const globalObject = typeof global === "object" ? global : window;

  (globalObject as any).consola = consola;
};

setupConsola();

const app = createApp(App);
app.mount("#app");
