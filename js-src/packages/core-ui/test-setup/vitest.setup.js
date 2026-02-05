import { vi } from "vitest";
import * as Vue from "vue";
import consola from "consola";

window.consola = consola;
window.Vue = Vue;

window.alert = vi.fn();

// Suppress Vue warnings in CI/pipeline environments
//   Automatically enabled when CI=true (set by most CI systems)
//   Can also be manually enabled with: SUPPRESS_VUE_WARNINGS=true pnpm run test:unit
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
vi.mock(
  "@jsonforms/vue",
  async () => {
    const original = await vi.importActual("@jsonforms/vue");
    return {
      ...original,
      rendererProps: vi.fn().mockReturnValue({
        control: null,
        schema: {},
        uischema: {},
        layout: null,
        path: "",
      }),
      useJsonFormsControl: vi.fn(() => ({
        handleChange: vi.fn(),
      })),
      useJsonFormsLayout: vi.fn(),
      useJsonFormsArrayControl: vi.fn(() => ({
        addItem: vi.fn(() => vi.fn()),
        moveUp: vi.fn(() => vi.fn()),
        moveDown: vi.fn(() => vi.fn()),
        removeItems: vi.fn(() => vi.fn()),
      })),
    };
  },
  { virtual: false },
);
vi.mock(
  "@jsonforms/core",
  async () => {
    const original = await vi.importActual("@jsonforms/core");
    return {
      ...original,
      createDefaultValue: vi.fn(),
    };
  },
  { virtual: false },
);
