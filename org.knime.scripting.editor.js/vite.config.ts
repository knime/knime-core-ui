// eslint-disable-next-line spaced-comment
/// <reference types="vitest" />
import { join } from "node:path";
import { URL, fileURLToPath } from "node:url";

import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import cssInjectedByJsPlugin from "vite-plugin-css-injected-by-js";
import dts from "vite-plugin-dts";
import svgLoader from "vite-svg-loader";

// @ts-expect-error no types for svgo.config available
import { svgoConfig } from "@knime/styles/config/svgo.config";

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    svgLoader({ svgoConfig }),
    dts({
      insertTypesEntry: true,
      tsconfigPath: fileURLToPath(
        new URL("tsconfig.app.json", import.meta.url),
      ),
    }),
    cssInjectedByJsPlugin({
      jsAssetsFilterFunction: (asset) => asset.fileName === "main.js",
    }), // not supported natively in Vite yet, see https://github.com/vitejs/vite/issues/1579]
  ],
  resolve: {
    alias: {
      "@": fileURLToPath(new URL("./src", import.meta.url)),
    },
  },
  build: {
    lib: {
      entry: [
        fileURLToPath(new URL("lib/main.ts", import.meta.url)),
        fileURLToPath(new URL("lib/loading.ts", import.meta.url)),
        fileURLToPath(
          new URL("src/scripting-service-browser-mock.ts", import.meta.url),
        ),
        fileURLToPath(
          new URL("src/initial-data-service-browser-mock.ts", import.meta.url),
        ),
        fileURLToPath(
          new URL("src/settings-service-browser-mock.ts", import.meta.url),
        ),
      ],
      // fileName: "knime-scripting-editor",
      formats: ["es"],
    },
    rollupOptions: {
      external: [
        "vue",
        "monaco-editor",
        "@knime/ui-extension-service",
        "@knime/components",
      ],
      treeshake: {
        // NB: This prevents bundling dependencies of dependencies that are not really used but imported
        // we never rely on side effects of dependencies, so this is safe
        moduleSideEffects: false,
      },
    },
  },
  test: {
    include: ["src/**/__tests__/**/*.test.{js,mjs,cjs,ts,mts,cts,jsx,tsx}"],
    exclude: ["**/node_modules/**", "**/dist/**"],
    environment: "jsdom",
    reporters: ["default"],
    server: {
      deps: {
        inline: ["@knime/kds-components"], // https://github.com/vitest-dev/vitest/issues/5283
      },
    },
    setupFiles: [
      fileURLToPath(new URL("./src/test-setup/setup.ts", import.meta.url)),
    ],
    root: fileURLToPath(new URL("./", import.meta.url)),
    alias: [
      {
        find: /^monaco-editor$/,
        replacement: join(
          __dirname,
          "/node_modules/monaco-editor/esm/vs/editor/editor.api.js",
        ),
      },
    ],
    coverage: {
      all: true,
      exclude: [
        "coverage/**",
        "dist/**",
        "lib/**",
        "**/*.d.ts",
        "**/__tests__/**",
        "**/__mocks__/**",
        "test-setup/**",
        "src/scripting-service-browser-mock.ts",
        "src/initial-data-service-browser-mock.ts",
        "src/settings-service-browser-mock.ts",
        "**/{vite,vitest,postcss,lint-staged}.config.{js,cjs,mjs,ts}",
        "**/.{eslint,prettier,stylelint}rc.{js,cjs,yml}",
      ],
      reporter: ["html", "text", "lcov"],
    },
  },
});
