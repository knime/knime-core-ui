# KNIME Core UI Components

Vue-based views and dialogs for KNIME Analytics Platform built as libraries and used in KNIME Analytics Platform and KNIME WebPortal.

## Development

### Prerequisites

- Node.js (see version in `package.json`)
- pnpm workspace (install from root: `pnpm install`)

#### Using local dependencies

If developing knime dependencies (e.g. `@webapps-common:packages/jsonforms`) then you can clone the appropriate repo and
link the target package(s) to this one.

1. Clone the repo to any location
2. If package uses `pnpm` then it must be built (see its README for specific build instructions)
   ```bash
   cd path/to/webapps-common/packages/jsonforms
   pnpm run build
   ```
3. Link the package globally
   ```bash
   cd path/to/webapps-common/packages/jsonforms
   pnpm link
   ```
   - this will symlink the package to
     `$XDG_CONFIG_HOME/nvm/versions/node/<node-version>/lib/node_modules/@knime/jsonforms`
4. Link to `jsonforms` to `js-src`
   ```bash
   cd path/to/knime-core-ui/org.knime.core.ui/js-src
   pnpm link @knime/jsonforms
   ```
   - this will symlink the package to `./node_modules/@knime/jsonforms`

> TODO: Confirm this is still true for `pnpm` (I think not)
> Note: running `npm install` or `npm link` will overwrite any pre-existing symlinks.
> If linking multiple packages then link them all at once, e.g.: `npm link @knime/jsonforms @knime/components`

Now if building in watch mode (default behavior for `npm dev:NodeDialog`) file changes in package dependencies that can
be built with `npm` (e.g. `@knime/components`) will trigger a rebuild.

Package dependencies that use `pnpm` do not trigger rebuilt from `vite` - they must be manually rebuilt after each
change. Alternatively one can build them with `pnpm build --watch`.

### View Development in KNIME Analytics Platform

Start the dev server for a specific view:

```sh
pnpm run dev:TableView
```

Add to Eclipse run configuration and start KNIME Analytics Platform:

```
-Dorg.knime.ui.dev.node.view.url=http://localhost:4000/TableView.js
-Dchromium.remote_debugging_port=8888
```

### Dialog Development

For node dialogs:

```sh
pnpm run dev:NodeDialog
```

Eclipse configuration:

```
-Dorg.knime.ui.dev.node.dialog.url=http://localhost:3333/NodeDialog.js
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

Note: noisy Vue warnings are suppressed if you set the environment flag `SUPPRESS_WARNINGS=true`,
and they are also suppressed when running with `CI=true` which is set by default in most pipeline
environments.

## Embedding

Views can be embedded in Vue/Nuxt apps as regular Vue components. Requires Vue and Consola compatible with versions in `package.json`, plus CSS variables from `@knime/styles`.

# Join the Community!

- [KNIME Forum](https://forum.knime.com/)
