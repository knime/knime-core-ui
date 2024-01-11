// eslint-disable-next-line spaced-comment
/// <reference types="vitest" />
import { fileURLToPath, URL } from "node:url";

import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import dts from "vite-plugin-dts";
import cssInjectedByJsPlugin from "vite-plugin-css-injected-by-js";
import svgLoader from "vite-svg-loader";

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    svgLoader(),
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
        fileURLToPath(
          new URL("src/scripting-service-instance.ts", import.meta.url),
        ),
        fileURLToPath(
          new URL("src/scripting-service-browser-mock.ts", import.meta.url),
        ),
      ],
      // fileName: "knime-scripting-editor",
      formats: ["es"],
    },
    rollupOptions: {
      external: ["vue", "monaco-editor"],
    },
  },
  test: {
    include: ["src/**/__tests__/**/*.test.{js,mjs,cjs,ts,mts,cts,jsx,tsx}"],
    exclude: ["**/node_modules/**", "**/dist/**", "webapps-common/**"],
    environment: "jsdom",
    reporters: ["default"],
    setupFiles: [
      fileURLToPath(new URL("./src/test-setup/setup.ts", import.meta.url)),
    ],
    root: fileURLToPath(new URL("./", import.meta.url)),
    alias: [
      {
        find: /^monaco-editor$/,
        replacement: `${__dirname}/node_modules/monaco-editor/esm/vs/editor/editor.api.js`,
      },
    ],
    coverage: {
      all: true,
      exclude: [
        "coverage/**",
        "dist/**",
        "webapps-common/**",
        "lib/**",
        "**/*.d.ts",
        "**/__tests__/**",
        "**/__mocks__/**",
        "test-setup/**",
        "**/{vite,vitest,postcss,lint-staged}.config.{js,cjs,mjs,ts}",
        "**/.{eslint,prettier,stylelint}rc.{js,cjs,yml}",
      ],
      reporter: ["html", "text", "lcov"],
    },
  },
});
