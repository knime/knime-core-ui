# KNIME Scripting Editor Framework

Shared scripting editor components for KNIME Analytics Platform and a standalone generic scripting editor app.

## Development

### Prerequisites

- Node.js (see version in `package.json`)
- pnpm workspace (install from root: `pnpm install`)

### Development Modes

#### Standalone Mode (Browser)

Develop with mocked backend services:

```sh
pnpm run dev:browser
```

Opens at [http://localhost:5173/](http://localhost:5173/)

#### KNIME Dialog Development Mode

Start KNIME Analytics Platform with:

```
-Dorg.knime.ui.dev.mode=true
-Dorg.knime.ui.dev.node.dialog.url=http://localhost:5173/
```

Run the development server:

```sh
pnpm run dev:knime
```

NOTE: The dialog served by the development server is only visible with the the dialog mode "Open in new window".

### Build

Library:

```sh
pnpm run build:lib          # Production build
pnpm run build:lib:watch    # Watch mode
```

Application:

```sh
pnpm run build:app
```

### Testing

```sh
pnpm run test:unit    # Run tests
pnpm run coverage     # Generate coverage
```

### Code Quality

```sh
pnpm run lint
pnpm run format
```
