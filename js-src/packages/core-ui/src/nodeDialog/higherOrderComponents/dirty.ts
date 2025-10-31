import { computed, h } from "vue";

import {
  type ExtractData,
  type VueControl,
  type VueControlRenderer,
  defineControl,
  mapControls,
} from "@knime/jsonforms";
import {
  DefaultSettingComparator,
  type SettingComparator,
} from "@knime/ui-extension-service";

import { useDirtySetting } from "../composables/components/useDirtySetting";
import type { allControls } from "../renderers";

export const dirty = <D>(
  component: VueControl<D>,
  valueComparator?: NoInfer<() => SettingComparator<D | undefined>>,
): VueControl<D> =>
  defineControl((props, ctx) => {
    if (props.control.path) {
      useDirtySetting({
        dataPath: computed(() => props.control.path),
        value: computed(() => props.control.data),
        valueComparator: valueComparator?.(),
      });
    }
    return () => h(component, props, ctx.slots);
  });

type ValueComparators<T extends Record<string, VueControlRenderer>> = Partial<{
  [K in keyof T]: () => SettingComparator<ExtractData<T[K]> | undefined>;
}>;

type TwinlistData = ExtractData<typeof allControls.twinlistRenderer>;
class TwinlistValueComparator extends DefaultSettingComparator<
  TwinlistData | undefined,
  string
> {
  // eslint-disable-next-line class-methods-use-this
  toInternalState(cleanSettings: TwinlistData | undefined): string {
    return JSON.stringify(cleanSettings, (key, value) =>
      // eslint-disable-next-line no-undefined
      key === "selected" ? undefined : value,
    );
  }

  // eslint-disable-next-line class-methods-use-this
  equals(newState: string, cleanState: string): boolean {
    return newState === cleanState;
  }
}

export const valueComparators: ValueComparators<typeof allControls> = {
  twinlistRenderer: () => new TwinlistValueComparator(),
  typedStringFilterRenderer: () => new TwinlistValueComparator(),
};

export const mapDirty = mapControls((c, k) =>
  dirty(c, valueComparators[k as keyof typeof valueComparators]),
);
