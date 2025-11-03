# KNIME UI Extensions

This Maven project contains the backend code for various UI Extensions.

> **TODO**: Provide a better summary

> The subdirectory [js-src](./js-src/README.md) contains the corresponding frontend code.

The UI Extensions include:
- The [Framework](#framework)
- ...

Definitions used throughout:
- **AP**: The Analytics Platform
- **Framework**: the entire mechanism used to convert declarative definitions of a node's
  configuration (parameters) to a frontend component of the **AP**
- (Framework) **API**: the available annotations used to declaratively define a node's configuration

## Framework

The Framework allows node configurations to be declaratively defined as a java class
and be dynamically converted to web components, utilizing [knime webapp components](https://github.com/knime/webapps-common?tab=readme-ov-file).

The framework includes:
- annotations to define node parameters
- converting node parameters into frontend components
- communicating changes between backend and frontend

### Framework API

The Framework API is split into public and internal. The public API is considered stable and will
remain backwards-compatible. The internal is not stable and no guarantees are made. As the internal
API stabilizes, pieces will be relocated to public.

The API is outlined as javadocs in [NodeParameters.java](org.knime.core.ui/src/eclipse/org/knime/node/parameters/NodeParameters.java)
(public) and [package-info.java](org.knime.core.ui/src/eclipse/org/knime/core/webui/node/dialog/defaultdialog/internal/package-info.java)
(private).

Subdirectories adjacent to these files contain various annotation definitions (`@interface`s):
- widgets - things that will appear in dialog (e.g. text box entry)
- updates - effects, things that trigger a dynamical change
- persistence - storage, how things get saved and loaded from `settings.xml`
- layout - how widgets get presented in dialog e.g. sections, horizontal
- examples - example `<MyNodeThingy>NodeParameters.java` files (currently only has one)
    - > **Note:** This assumes "new node", and thus has no reference to API for customising
      persistence, which is needed for migrated nodes.

The following are internal only (for now):
- button - add a button next to some field with configurable action
- dynamic - settings field that reacts dynamically to situation
    - e.g. (perhaps?) a set of settings where each column of an inputted table gets it's own
      setting, since the table has unknown column count, dialog must respond dynamically
- file - interacting with files

### Framework flow

1. A node definition requires a class which extends [`NodeParameters`](org.knime.core.ui/src/eclipse/org/knime/node/parameters/NodeParameters.java)
2. This class is provided to the nodes `NodeFactory` class
    - [NodeFactory.java (from knime-core)](knime-core/org.knime.core/src/eclipse/org/knime/core/node/NodeFactory.java)
3. ...
4. Two tree structures are built as a result, the Widget tree and the Persistence tree
    - the Widget tree defines the dialog panel, its appearance, internal interactions, ...
    - the Persistence tree defines how the settings are stored / loaded to the nodes `settings.xml`
      file
        - > **Note:** when migrating nodes extra care must be taken to guarantee backwards
          compatibility
5. These trees (both?) are built via reflection and converted into an extended [json form](https://jsonforms.io/)
    - KNIME has extended json forms to include extra information
    - see(?) [JsonFormsSettings.java](org.knime.core.ui/src/eclipse/org/knime/core/webui/node/dialog/defaultdialog/jsonforms/JsonFormsSettings.java)
    - see(?)
      [JsonFormsSchemaUtil.java](org.knime.core.ui/src/eclipse/org/knime/core/webui/node/dialog/defaultdialog/jsonforms/schema/JsonFormsSchemaUtil.java)
      for example of building schema(?)
      > **Question**: the buildSchmema builds an xml, not a json form. What's up with that?
6. These json forms define the interface between backend and frontend
7. Various "data services" handle the backend↔frontend communication
    - e.g. [ApplyDataService.java](org.knime.core.ui/src/eclipse/org/knime/core/webui/data/ApplyDataService.java)
      handles communication triggered by clicking "apply"
8. The frontend converts the json forms into settings pane displayed in AP
9. The frontend communicates changes in settings to the backend via similar json forms
    - the communication layer is actually more complex, enabling e.g. partial communication of only
      the changes
