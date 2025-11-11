# KNIME Frontend Development Guide

## Architecture Overview

This project (knime-core-ui/js-src) depends on packages from the separate **webapps-common** monorepo repository. The key packages are:

- `@knime/jsonforms` - JsonForms integration and custom renderers
- `@knime/components` - Reusable Vue components
- `@knime/styles` - Shared styles and design system

These packages can be **symlinked** for local development to enable live-reloading in some cases.

## Development Environment Setup

### Prerequisites

- Node.js 22.x (or 24.x with engine warnings)
- pnpm 10.x installed globally
- Both repositories cloned, e.g.:
  ```
  repos/
    ├── knime-core-ui/org.knime.core.ui/js-src/
    └── webapps-common/
  ```

### Initial Setup (One-Time)

#### 1. Build desired webapps-common dependencies

E.g. for `jsonforms`:

```bash
cd /path/to/webapps-common/packages/jsonforms
pnpm install
pnpm build
```

**Why pnpm?** The webapps-common repository uses:

- `catalog:` protocol for shared dependency versions
- `workspace:*` protocol for internal package references
- These are pnpm-specific features that npm/yarn don't support

Some packages (e.g. `components`) do not need to be built and can simply be symlinked.

#### 2. Install js-src dependencies with npm

```bash
cd /path/to/knime-core-ui/org.knime.core.ui/js-src
npm install
```

We use npm here (not pnpm) because js-src is a standalone project, not part of the pnpm workspace.

#### 3. Create symlinks to webapps-common packages

**Option A: Manual symlinks (recommended for reliability)**

```bash
cd /path/to/knime-core-ui/org.knime.core.ui/js-src

# Remove installed versions
rm -r node_modules/@knime/jsonforms
rm -r node_modules/@knime/components

# Create symlinks (adjust path if your repos are in different locations)
ln -s ../../../../../webapps-common/packages/jsonforms node_modules/@knime/jsonforms
ln -s ../../../../../webapps-common/packages/components node_modules/@knime/components
```

**Option B: npm link (can be fragile)**

```bash
# In webapps-common packages
cd /path/to/webapps-common/packages/jsonforms
npm link

cd /path/to/webapps-common/packages/components
npm link

# In js-src
cd /path/to/knime-core-ui/org.knime.core.ui/js-src
npm link @knime/jsonforms @knime/components
```

⚠️ **Important:** Link both packages simultaneously to avoid one overwriting the other.

#### 4. Verify symlinks

```bash
cd /path/to/knime-core-ui/org.knime.core.ui/js-src
ls -la node_modules/@knime/jsonforms  # Should show -> pointing to webapps-common
ls -la node_modules/@knime/components # Should show -> pointing to webapps-common
```

### Running Development Server

```bash
cd /path/to/knime-core-ui/org.knime.core.ui/js-src
npm run dev:NodeDialog
```

Changes to webapps-common source files should now hot-reload automatically! No rebuilding needed.

## Common Issues and Solutions

### Issue: Changes to webapps-common packages don't appear

**Symptoms:** Editing files in `webapps-common/packages/jsonforms/src/` doesn't trigger hot reload

**Possible causes:**

1. **Symlinks not created** - Check with `ls -la node_modules/@knime/`
2. **Vite cache** - Clear with `rm -r node_modules/.vite && rm -r dist/`
3. **Browser cache** - Hard refresh (Ctrl+Shift+R)
4. **Wrong package being used** - Global npm packages can override symlinks

**Debug steps:**

```bash
# 1. Verify symlink points to correct location
ls -la node_modules/@knime/jsonforms

# 2. Add a console.warn() to the source file
# Edit webapps-common/packages/jsonforms/src/uiComponents/RadioControlBase.vue
# Add: console.warn("🚨 RadioControlBase loaded from webapps-common");

# 3. Check browser console or IDE logs for the message

# 4. If still not working, check for global npm links
ls -la ~/.config/nvm/versions/node/*/lib/node_modules/@knime/
# Remove any found with: rm -r <path>
```

### Issue: Build fails with "catalog:" errors

**Error:** `Unsupported URL Type "catalog:"`

**Cause:** Trying to run pnpm/npm install inside a webapps-common package when it's not part of the pnpm workspace

**Solution:** Only run `pnpm install` from the webapps-common root, never inside individual packages

### Issue: Build fails resolving @knime/jsonforms entry

**Error:** `Failed to resolve entry for package "@knime/jsonforms"`

**Cause:** The package.json exports point to dist/ instead of src/

**Solution:** Verify jsonforms/package.json has:

```json
{
  "main": "./src/index.ts",
  "exports": {
    ".": {
      "import": "./src/index.ts"
    }
  }
}
```

### Issue: npm install breaks symlinks

**Cause:** Running `npm install` can replace symlinked packages with versions from npm registry

**Solution:** Re-create symlinks after running npm install (see step 3 above)

## Key Package Differences

### @knime/components

- **Exports:** Source files (`./src/index.ts`)
- **Symlink works:** ✅ Out of the box
- **Build needed:** ❌ No

### @knime/jsonforms

- **Exports:** Source files (`./src/index.ts`)
- **Symlink works:** ✅
- **Build needed:** ✅ Yes
- **Dependencies:** Uses catalog: and workspace: protocols, needs pnpm in webapps-common

## Component Discovery Strategy

When trying to find which component renders in the UI:

1. **Add unique markers** to templates:

   ```vue
   <template>
     <div>MARKER_NAME - ComponentName</div>
     <!-- existing template -->
   </template>
   ```

2. **Use phonetic alphabet** for systematic testing (ALPHA, BRAVO, CHARLIE, etc.)

3. **Check component hierarchy:**

   - NodeDialog.vue → NodeDialogCore.vue → JsonFormsDialog.vue → Individual controls

4. **Remember async components** are lazy-loaded:

   ```typescript
   const RadioControl = defineAsyncComponent(
     () => import("../uiComponents/RadioControl.vue"),
   );
   ```

5. **Component chains** for radio buttons:
   - RadioControl.vue → RadioControlBase.vue → RadioButtons.vue (from @knime/components) → BaseRadioButtons.vue

## Development Commands

```bash
# Development mode (with hot reload)
npm run dev:NodeDialog          # Node configuration dialog
npm run dev:TableView           # Table view
npm run dev:TextView            # Text view

# Build for production
npm run build                   # Build all components
npm run build:NodeDialog        # Build specific component

# Testing
npm run test:unit              # Unit tests
npm run test:integration       # Integration tests
npm run coverage               # Coverage report

# Code quality
npm run lint                   # Fix linting issues
npm run format                 # Format code with Prettier
npm run type-check             # TypeScript type checking
```

## When to Rebuild vs Live Reload

### Live reload works for:

- ✅ Vue component templates
- ✅ Vue component scripts
- ✅ TypeScript files
- ✅ CSS/PostCSS files
- ✅ Changes in symlinked packages that didn't need building
- ❌ Changes in symlinked packages that did need building (simply rebuild these)

### Requires rebuild:

- ❌ Changes to vite.config.ts
- ❌ Changes to package.json
- ❌ Adding new dependencies
- ❌ Switching build modes

## Troubleshooting Checklist

When things aren't working:

- [ ] Symlinks exist and point to correct location
- [ ] webapps-common has `node_modules` installed via pnpm
- [ ] js-src has `node_modules` installed via npm
- [ ] No global npm links interfering (`ls ~/.config/nvm/versions/node/*/lib/node_modules/@knime/`)
- [ ] Vite cache cleared (`rm -r node_modules/.vite dist/`)
- [ ] Dev server restarted
- [ ] Browser hard refresh (Ctrl+Shift+R)
- [ ] Package.json exports point to src/ not dist/

## Architecture Patterns

### JsonForms Integration

- Schema-driven form rendering
- Custom renderers in `src/nodeDialog/renderers/`
- Higher-order components for common functionality (flow variables, dirty state, advanced settings)

### UI Extension Service

- Communication with backend via `@knime/ui-extension-service`
- JsonDataService for data state
- SharedDataService for shared state
- DialogService for dialog operations
- AlertingService for user notifications

### Testing Strategy

- Unit tests: Fast, isolated component tests
- Integration tests: Test with JsonForms integration
- Use `--mode integration` flag to run integration tests

---

**Last Updated:** November 2025
**Maintainer:** KNIME Frontend Team
