# KNIME Core UI Components

Vue-based views and dialogs for KNIME Analytics Platform built as libraries and used in KNIME Analytics Platform and KNIME WebPortal.

## Development

### Prerequisites

- Node.js (see version in `package.json`)
- pnpm workspace (install from root: `pnpm install`)

### View Development in KNIME Analytics Platform

Start the dev server for a specific view:

```sh
pnpm run dev:TableView
```

Add to Eclipse run configuration and start KNIME Analytics Platform:

```
-Dorg.knime.ui.dev.node.view.url=http://localhost:4000/TableView.umd.js
-Dchromium.remote_debugging_port=8888
```

### Dialog Development

For node dialogs:

```sh
pnpm run dev:NodeDialog
```

Eclipse configuration:

```
-Dorg.knime.ui.dev.node.dialog.url=http://localhost:3333/NodeDialog.umd.js
```

Standalone development with mocks:

```sh
pnpm run dev:NodeDialog:standalone
```

### Building

Build all components:

```sh
pnpm run build
```

Build specific component:

```sh
pnpm run build:TableView
```

Results are output to `../../../org.knime.core.ui/js-src/dist`.

### Testing

Run tests:

```sh
pnpm run test
```

Generate coverage:

```sh
pnpm run coverage
```

## Embedding

Views can be embedded in Vue/Nuxt apps as regular Vue components. Requires Vue and Consola compatible with versions in `package.json`, plus CSS variables from `@knime/styles`.

# Join the Community!

- [KNIME Forum](https://forum.knime.com/)
