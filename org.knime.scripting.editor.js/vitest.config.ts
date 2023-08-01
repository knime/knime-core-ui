import { fileURLToPath } from "node:url";
import { mergeConfig, defineConfig } from "vite";
import viteConfig from "./vite.config";

export default mergeConfig(
  viteConfig,
  defineConfig({
    test: {
      include: ["src/**/__tests__/**/*.test.{js,mjs,cjs,ts,mts,cts,jsx,tsx}"],
      exclude: ["**/node_modules/**", "**/dist/**", "webapps-common/**"],
      environment: "jsdom",
      root: fileURLToPath(new URL("./", import.meta.url)),
      transformMode: {
        web: [/\.[jt]sx$/],
      },
      setupFiles: [
        fileURLToPath(new URL("test-setup/vitest.setup.js", import.meta.url)),
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
          "test-setup/**",
          "**/{vite,vitest,postcss}.config.{js,cjs,mjs,ts}",
          "**/.{eslint,prettier,stylelint}rc.{js,cjs,yml}",
        ],
        reporter: ["html", "text", "lcov"],
      },
    },
  }),
);
