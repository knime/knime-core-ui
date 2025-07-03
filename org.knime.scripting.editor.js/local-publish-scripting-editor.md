# Local Dry-Run Publish of @knime/scripting-editor via Verdaccio

Publishing to a throw-away local registry lets you road-test @knime/scripting-editor under the exact conditions your users will face—installing via npm install, with real post-install hooks and dependency resolution—without polluting npmjs.org or cluttering your global setup. It surfaces issues that npm link can mask (hoisting, peer-deps, bundled files), keeps the test loop fast and private, and tears down cleanly when you’re done.

## TL;DR

```bash
# 0  Clean-slate (optional)
docker rm -f knime_registry 2>/dev/null || true

# 1  Start a disposable Verdaccio registry
docker run -d --name knime_registry -p 4873:4873 verdaccio/verdaccio

# 2  Add a throw-away user
#    ⇢ Verdaccio shows a funky spinner while you type the password – just ignore it and keep typing.
npm adduser --registry http://localhost:4873

# 3  In the @knime/scripting-editor repo
cd path/to/knime/scripting-editor
npm version prerelease --preid=local.$(git rev-parse --short HEAD)
npm publish --registry http://localhost:4873

# 4  In every consumer app
echo '@knime:registry=http://localhost:4873' >> .npmrc
npm install @knime/scripting-editor@$(node -p "require('./package.json').version")

# 5  Test as usual …

# 6  Tear down
rm .npmrc                      # in every consumer repo
docker rm -f knime_registry    # stops & deletes the container
```

## Step-by-Step Explanation

| Step  | Command                                                                                                                                                          | Notes                                                                                                                                                                               |
| ----- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **1** | `docker run -d --name knime_registry -p 4873:4873 verdaccio/verdaccio`                                                                                           | Container names may not accept `-` on some Docker setups; use the underscore-separated **knime\_registry** instead.                                                                 |
| **2** | `npm adduser --registry http://localhost:4873`                                                                                                                   | Verdaccio needs a user for `npm publish`. You can invent any credentials. **The CLI shows a weird loading animation while you type the password—just ignore it and type normally.** |
| **3** | Bump to a unique prerelease (`npm version prerelease --preid=local.$(git rev-parse --short HEAD)`) then publish (`npm publish --registry http://localhost:4873`) | Keeping the version unique prevents clashes with real npmjs.org versions.                                                                                                           |
| **4** | In each consumer repo create/append `.npmrc` line: `@knime:registry=http://localhost:4873`                                                                       | This is *local to the project*, so no global npm settings are touched.                                                                                                              |
| **5** | `npm install @knime/scripting-editor@<new-local-version>`                                                                                                        | Now the package is installed exactly as if it came from npmjs.org, exposing any issues hidden by `npm link`.                                                                        |
| **6** | Clean-up (`rm .npmrc`, `docker rm -f knime_registry`)                                                                                                            | Leaves no trace on your system.                                                                                                                                                     |
