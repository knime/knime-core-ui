import { composePaths } from "./paths";

/**
 * A running variable during the resolution of which deprecated flow variables
 * point to which widgets
 */
export interface DeprecatedConfigPathsCandidate {
  /**
   * The paths relative to which paths in targetConfigPaths and
   * deprecatedConfigPaths are to be understood
   */
  basePaths: string[];
  /**
   * An array of deprecated config paths (relative to basePaths)
   */
  deprecatedConfigPaths: string[];
}

export const createNewCandidate = (
  part: {
    deprecated: string[][];
  },
  configPaths: string[],
): DeprecatedConfigPathsCandidate => ({
  basePaths: configPaths,
  deprecatedConfigPaths: part.deprecated.map((keys) =>
    keys.reduce(composePaths, ""),
  ),
});

interface ProcessedDeprecatedConfigPathsCandidate {
  targetConfigPaths: string[];
  deprecatedConfigPaths: string[];
}

const process = (
  candidate: DeprecatedConfigPathsCandidate,
): ProcessedDeprecatedConfigPathsCandidate => {
  const deprecatedConfigPaths = candidate.basePaths.flatMap((basePath) =>
    candidate.deprecatedConfigPaths.map((deprecatedPath) =>
      composePaths(basePath, deprecatedPath),
    ),
  );
  return {
    targetConfigPaths: candidate.basePaths,
    deprecatedConfigPaths,
  };
};

const findDeprecatedConfigPathsForCandidate = (
  {
    targetConfigPaths,
    deprecatedConfigPaths,
  }: ProcessedDeprecatedConfigPathsCandidate,
  configPath: string,
): string[] => {
  if (
    targetConfigPaths.filter((targetConfigPath) =>
      configPath.startsWith(targetConfigPath),
    ).length > 0
  ) {
    return deprecatedConfigPaths;
  }
  return [];
};

const findDeprecatedConfigPaths = (
  candidates: ProcessedDeprecatedConfigPathsCandidate[],
  configPath: string,
): string[] => {
  return candidates.flatMap((candidate) =>
    findDeprecatedConfigPathsForCandidate(candidate, configPath),
  );
};

export const toConfigPathsWithDeprecatedConfigPaths = (
  configPaths: string[],
  candidates: DeprecatedConfigPathsCandidate[],
) => {
  const processedCandidates = candidates.map(process);
  return configPaths.map((configPath) => ({
    configPath,
    deprecatedConfigPaths: findDeprecatedConfigPaths(
      processedCandidates,
      configPath,
    ),
  }));
};
