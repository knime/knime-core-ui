import { constants } from "node:fs";
import { access, mkdir } from "node:fs/promises";

const dir = process.argv[2];

if (!dir) {
  throw new Error("Usage: node createDir.js <path>");
}

async function ensureDir(path) {
  try {
    await access(path, constants.F_OK);
  } catch {
    await mkdir(path, { recursive: true });
  }
}

ensureDir(dir).catch((err) => {
  throw new Error("Failed to create directory:", err);
});
