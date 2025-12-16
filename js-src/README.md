# KNIME Core UI Frontend Development

This is a pnpm monorepo containing frontend packages for core dialogs and views.

## Prerequisites

- Node.js (see version in `package.json`)
- pnpm (see version in `package.json`)

## Development

Install dependencies for all packages:

```sh
pnpm install
```

Build all packages:

```sh
pnpm run build:apps
```

Run tests across all packages:

```sh
pnpm run coverage
```

## Package-Specific Development

Each package has its own development commands. See individual package READMEs:

- [Core UI Development](packages/core-ui/README.md)
- [Scripting Editor Development](packages/scripting-editor/README.md)
