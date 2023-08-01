import { fileURLToPath } from "node:url";
import { mergeConfig, defineConfig } from "vite";
import { configDefaults } from "vitest/config";
import viteConfig from "./vite.config";

export default mergeConfig(
  viteConfig,
  defineConfig({
    test: {
      environment: "jsdom",
      exclude: [...configDefaults.exclude, "e2e/*"],
      root: fileURLToPath(new URL("./", import.meta.url)),
      transformMode: {
        web: [/\.[jt]sx$/],
      },
      setupFiles: [
        fileURLToPath(new URL("test-setup/vitest.setup.js", import.meta.url)),
      ],
    },
  }),
);
