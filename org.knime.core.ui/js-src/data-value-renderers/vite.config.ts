import { defineConfig, UserConfig } from "vite";
import { fileURLToPath, URL } from "node:url";
import { InputOption } from "rollup";
import { resolve } from "path";
import glob from "glob";

export default defineConfig((): UserConfig => {
  // find all view .html files and add them as entry points
  const input: InputOption = glob.sync(
    resolve(__dirname, "src", "[!index]*.html"),
  );
  const config: UserConfig = {
    root: "src",
    base: "./",
    resolve: {
      alias: { "@": fileURLToPath(new URL("./src", import.meta.url)) },
    },
    build: {
      outDir: "../dist",
      emptyOutDir: true,
      rollupOptions: {
        input,
      },
      chunkSizeWarningLimit: 700,
      sourcemap: true,
    },
  };
  return config;
});
