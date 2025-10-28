import { vi } from "vitest";

import NodeDialogCore from "../NodeDialogCore.vue";

export const getOptions = ({
  dispatch,
}: {
  stubButtonsBySlot?: true;
  dispatch?: ((path: string, value: any) => void) | null;
} = {}) => {
  return {
    global: {
      provide: {
        getKnimeService: () => ({}),
      },
      stubs: {
        NodeDialogCore,
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
