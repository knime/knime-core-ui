import { rm } from "node:fs/promises";

const dir = process.argv[2];

if (!dir) {
  throw new Error("Usage: node removeDir.js <path>");
}

await rm(dir, {
  recursive: true,
  force: true,
});
