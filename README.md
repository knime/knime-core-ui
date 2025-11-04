# KNIME® Core UI

[![Jenkins](https://jenkins.knime.com/buildStatus/icon?job=knime-core-ui%2Fmaster)](https://jenkins.knime.com/job/knime-core-ui/job/master/)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=KNIME_knime-core-ui&metric=alert_status&token=3754ed0a736e21d7b41232ced136830b1f844af6)](https://sonarcloud.io/summary/new_code?id=KNIME_knime-core-ui)

This repository is maintained by the [KNIME UI Extensions Development Team](mailto:team-ui-extensions@knime.com).

This repository contains the core UI functionality for KNIME Analytics Platform including Vue-based frontend components.

## Frontend Development

The frontend code is organized as a pnpm monorepo in `js-src/` containing:
- **core-ui**: Vue-based views and dialogs for KNIME Analytics Platform
- **scripting-editor**: Shared scripting editor components

For frontend development setup, see [js-src/README.md](js-src/README.md).

In case Eclipse complains about missing libs, have a look here:
[org.knime.core.ui/lib/WHERE_ARE_THE_JARS.txt](org.knime.core.ui/lib/WHERE_ARE_THE_JARS.txt) and
[org.knime.core.ui.tests/lib/WHERE_ARE_THE_JARS.txt](org.knime.core.ui.tests/lib/WHERE_ARE_THE_JARS.txt)

# Join the Community!
* [KNIME Forum](https://forum.knime.com/)
