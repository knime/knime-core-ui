import vitest from "@vitest/eslint-plugin";

import knimeVitest from "@knime/eslint-config/vitest.js";
import createKnimeVueTSConfig from "@knime/eslint-config/vue3-typescript.js";

export default [
  ...createKnimeVueTSConfig(),
  ...knimeVitest,
  {
    plugins: {
      vitest,
    },
    rules: {
      "vitest/no-import-node-test": "error",
    },
  },
  {
    files: ["demo/**/*"],
    rules: { "no-magic-numbers": "off" },
  },
];
