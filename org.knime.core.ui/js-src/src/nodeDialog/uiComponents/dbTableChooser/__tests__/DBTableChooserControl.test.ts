import { beforeEach, describe, expect, it, vi } from "vitest";
import { nextTick } from "vue";
import { flushPromises } from "@vue/test-utils";

import {
  type VueControlTestProps,
  getControlBase,
  mountJsonFormsControl,
} from "@knime/jsonforms/testing";

import DBTableChooserControl from "../DBTableChooserControl.vue";
import DBTableChooserFileExplorer from "../DBTableChooserFileExplorer.vue";

const shouldUseCatalogs = vi.hoisted(() => vi.fn(() => true));

vi.mock("../useDbTableChooserBackend.ts", async () => {
  const { makeTableChooserBackendMock } = await import("./mockedBackendData");
  return {
    useDbTableChooserBackend: () =>
      makeTableChooserBackendMock(shouldUseCatalogs()),
  };
});

const BASE_PROPS: Omit<
  VueControlTestProps<typeof DBTableChooserControl>,
  "messages"
> = {
  control: {
    ...getControlBase("test"),
    data: {
      schemaName: "testSchema",
      tableName: "testTable",
      catalogName: "testCatalog",
    },
    schema: {
      type: "object",
      title: "File path",
      properties: {
        schemaName: {
          type: "string",
          title: "Schema Name",
          description: "The name of the schema to select.",
          default: "defaultSchema",
        },
        tableName: {
          type: "string",
          title: "Table Name",
          description: "The name of the table to select.",
          default: "defaultTable",
        },
        catalogName: {
          type: "string",
          title: "Catalog Name",
          description: "The name of the catalog to select.",
          default: "defaultCatalog",
        },
      },
    },
    uischema: {
      type: "Control",
      scope: "#/properties/view/properties/dbSelection",
      options: {
        format: "dbTableChooser",
        catalogsSupported: true,
        dbConnected: true,
        validateSchema: true,
        validateTable: true,
        validateCatalog: true,
      },
    },
  },

  disabled: false,
  isValid: true,
};

const doMount = async (
  {
    stubs,
    props,
  }: {
    stubs?: Record<string, boolean>;
    props?: Omit<VueControlTestProps<typeof DBTableChooserControl>, "messages">;
  } = {
    stubs: {},
  },
) => {
  const fullProps = {
    ...BASE_PROPS,
    ...props,
  };

  const mountedResult = mountJsonFormsControl(DBTableChooserControl, {
    // @ts-expect-error since we don't have the messages property
    props: fullProps,
    stubs: {
      DBTableChooserFileExplorer,
      ...stubs,
    },
  });

  await nextTick();
  await flushPromises();

  // because of debouncing, we need to wait
  await new Promise((resolve) => setTimeout(resolve, 500));

  await nextTick();
  await flushPromises();

  return mountedResult;
};

describe("DBTableChooserControl", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it("renders", async () => {
    const { wrapper } = await doMount();
    expect(wrapper.findComponent(DBTableChooserControl).exists()).toBe(true);
  });

  it("validates inputs and displays errors iff relevant", async () => {
    const props = {
      ...BASE_PROPS,
    };
    props.control.data.catalogName = "testCatalog";
    props.control.data.schemaName = "testSchema";
    props.control.data.tableName = "testTable";
    const { wrapper } = await doMount({ props });

    expect(wrapper.findAll(".error-text").length).toBe(0);

    // now mount with invalid data
    const invalidProps = {
      ...BASE_PROPS,
    };
    invalidProps.control.data.catalogName = "invalidCatalog";
    invalidProps.control.data.schemaName = "invalidSchema";
    invalidProps.control.data.tableName = "invalidTable";
    const { wrapper: invalidWrapper } = await doMount({ props: invalidProps });

    // we need to convince the control that some user interaction has happened
    // @ts-ignore
    invalidWrapper.vm.enableErrorMessages = true;

    await nextTick();
    await flushPromises();

    // because of debouncing, we need to wait a bit for things to show up
    await new Promise((resolve) => setTimeout(resolve, 1000));

    expect(invalidWrapper.findAll(".error-text").length).toBe(3);
  });

  it("displays 3 input fields for catalog, schema and table when using catalogs", async () => {
    shouldUseCatalogs.mockReturnValue(true);
    const props = {
      ...BASE_PROPS,
    };
    props.control.uischema.options!.catalogsSupported = true;

    const { wrapper } = await doMount({
      props,
    });
    expect(wrapper.findAll(".error-message").length).toBe(3);
  });

  it("displays 2 input fields for schema and table when not using catalogs", async () => {
    shouldUseCatalogs.mockReturnValue(false);
    const props = {
      ...BASE_PROPS,
    };
    props.control.uischema.options!.catalogsSupported = false;
    const { wrapper } = await doMount({
      props,
    });
    expect(wrapper.findAll(".error-message").length).toBe(2);
  });
});
