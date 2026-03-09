import knimeVitest from "@knime/eslint-config/vitest.js";
import createKnimeVueTSConfig from "@knime/eslint-config/vue3-typescript.js";

export default [
  ...createKnimeVueTSConfig(),
  ...knimeVitest,
  {
    files: ["demo/**/*"],
    rules: { "no-magic-numbers": "off" },
  },
];
