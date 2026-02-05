import { vi } from "vitest";
import * as Vue from "vue";
import consola from "consola";

window.consola = consola;
window.Vue = Vue;

// Suppress Vue warnings in CI/pipeline environments
//   Automatically enabled when CI=true (set by most CI systems)
//   Can also be manually enabled with: SUPPRESS_VUE_WARNINGS=true pnpm run test:integration
// eslint-disable-next-line no-process-env
if (process.env.CI === "true" || process.env.SUPPRESS_VUE_WARNINGS === "true") {
  // eslint-disable-next-line no-console
  const originalWarn = console.warn;
  // eslint-disable-next-line no-console
  console.warn = (...args) => {
    const msg = args[0];
    // Filter out Vue warnings
    if (typeof msg === "string" && msg.startsWith("[Vue warn]")) {
      return;
    }
    originalWarn.apply(console, args);
  };
}

vi.mock("@knime/ui-extension-service");
vi.mock("@knime/ui-extension-service/internal");
