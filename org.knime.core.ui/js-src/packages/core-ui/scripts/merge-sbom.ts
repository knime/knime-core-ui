/* eslint-disable no-console */
/* eslint-disable no-process-exit */
import { readFileSync, readdirSync, writeFileSync } from "node:fs";
import { join } from "node:path";

import * as CDX from "@cyclonedx/cyclonedx-library";

const fatalError = (message: string): never => {
  console.error(message);
  process.exit(1);
};

// --- Parameters ---
const [, , SBOM_DIR, OUT_FILE] = process.argv;
if (!SBOM_DIR || !OUT_FILE) {
  console.error(
    "Usage: tsx scripts/merge-sbom.ts <input-folder> <output-file>",
  );
  process.exit(1);
}

// --- Types from CycloneDX ---
type Bom = CDX.Serialize.JSON.Types.Normalized.Bom;
type Component = CDX.Serialize.JSON.Types.Normalized.Component;

// --- Gather input files ---
const files = readdirSync(SBOM_DIR).filter((f) => /^sbom-.*\.json$/i.test(f));
if (files.length === 0) {
  fatalError(`No ${SBOM_DIR}/sbom-*.json found`);
}

const parseBomFile = (file: string): Bom => {
  const filePath = join(SBOM_DIR, file);
  try {
    const content = readFileSync(filePath, "utf8");
    return JSON.parse(content) as Bom;
  } catch (err) {
    return fatalError(`Failed to read/parse BOM file at ${filePath}: ${err}`);
  }
};

// --- Collection of components and dependencies ---
const components = new Map<string, Component>();
const deps = new Map<string, Set<string>>();

const addComponent = (c: Component) => {
  const ref = c["bom-ref"];
  if (typeof ref === "undefined") {
    fatalError(
      `Component without bom-ref found: ${JSON.stringify(c, null, 2)}`,
    );
  } else if (!components.has(ref)) {
    components.set(ref, c);
  }
};
const addDepEdge = (ref: string, on: string[] = []) => {
  const set = deps.get(ref) ?? new Set<string>();
  on.forEach((d) => set.add(d));
  deps.set(ref, set);
};

// --- Collect components + dependencies from each SBOM ---
for (const file of files) {
  // Extract the name of this component from the filename
  const modeName = file.replace(/^sbom-/, "").replace(/\.json$/i, "");

  // Read and parse the SBOM file
  const bom = parseBomFile(file);

  // Add all components and dependencies from this SBOM
  (bom.components ?? []).forEach(addComponent);
  (bom.dependencies ?? []).forEach((d) => addDepEdge(d.ref, d.dependsOn ?? []));

  // Add a root component for this mode
  const modeRef = `urn:mode:${modeName}`;
  const component: Component = {
    "bom-ref": modeRef,
    name: `core-ui:${modeName}`,
    group: "@knime",
    type: CDX.Enums.ComponentType.Application,
  };
  addComponent(component);

  // Link the root component to its dependencies
  const modeDeps = (bom.components ?? [])
    .map((c) => c["bom-ref"])
    .filter((r) => typeof r !== "undefined"); // filter out undefined
  addDepEdge(modeRef, modeDeps);
}

// --- Construct merged BOM ---
const referenceBom = parseBomFile(files[0]);
const mergedBom: Bom = {
  $schema: referenceBom.$schema,
  bomFormat: referenceBom.bomFormat,
  specVersion: referenceBom.specVersion,
  version: 1,
  serialNumber: CDX.Utils.BomUtility.randomSerialNumber(),
  metadata: {
    ...referenceBom.metadata,
    timestamp: new Date().toISOString(),
  },
  components: [...components.values()],
  dependencies: [...deps.entries()].map(([ref, set]) => ({
    ref,
    dependsOn: [...set],
  })),
};

// --- Validate merged BOM ---
const validator = new CDX.Validation.JsonStrictValidator(
  CDX.Spec.Spec1dot6.version,
);
const errs = await validator.validate(JSON.stringify(mergedBom));
if (errs) {
  fatalError(
    `Merged SBOM failed validation:\n${JSON.stringify(errs, null, 2)}`,
  );
}

// --- Output merged BOM ---
writeFileSync(OUT_FILE, JSON.stringify(mergedBom, null, 2));
console.log(`Merged SBOM written to ${OUT_FILE}`);
