import { URL, fileURLToPath } from "node:url";

import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import svgLoader from "vite-svg-loader";

// @ts-expect-error no types for svgo.config available
import { svgoConfig } from "@knime/styles/config/svgo.config";

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue(), svgLoader({ svgoConfig })],
  resolve: {
    alias: {
      "@": fileURLToPath(new URL("./src", import.meta.url)),
    },
  },
  define: {
    global: "globalThis",
  },
  server: {
    port: 3000,
  },
  root: ".",
  optimizeDeps: {
    include: [
      "monaco-editor/esm/vs/editor/editor.worker.js",
      "monaco-editor/esm/vs/language/typescript/ts.worker.js",
    ],
  },
});
