import { vi } from "vitest";

export const getOptions = ({
  dispatch,
}: {
  stubButtonsBySlot?: true;
  dispatch?: (path: string, value: any) => void;
} = {}) => {
  return {
    global: {
      provide: {
        getKnimeService: () => ({}),
      },
      stubs: {
        ...(dispatch
          ? {
              JsonFormsDialog: {
                props: ["data", "schema", "uischema", "renderers"],
                emits: ["change"],
                template: '<slot :name="bottom"/>',
                methods: {
                  updateData: dispatch || vi.fn(),
                },
              },
            }
          : {}),
      },
    },
  };
};
