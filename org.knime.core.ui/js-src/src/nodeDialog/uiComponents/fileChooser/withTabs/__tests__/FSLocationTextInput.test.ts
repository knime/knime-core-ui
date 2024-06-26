import FSLocationTextInput, {
  type Props as FSLocationTextInputProps,
  prefixes,
} from "../FSLocationTextInput.vue";
import { shallowMount } from "@vue/test-utils";
import { beforeEach, describe, expect, it, vi } from "vitest";
import flushPromises from "flush-promises";
import InputField from "@@/webapps-common/ui/components/forms/InputField.vue";

describe("FSLocationTextInput.vue", () => {
  const currentSpacePrefix = "knime://knime.space/";
  let props: FSLocationTextInputProps;

  beforeEach(() => {
    props = {
      modelValue: {
        path: "myPath",
        timeout: 1000,
        fsCategory: "relative-to-current-hubspace",
      },
      disabled: false,
      isLocal: false,
    };
  });

  const absolute = (path: string) => `absolute/path/to/${path}`;
  const mountFsLocationTextInput = async () => {
    const dataServiceSpy = vi.fn(
      ({
        options,
      }: {
        method?: string | undefined;
        options: [unknown, unknown, string];
      }) => {
        return Promise.resolve({ path: absolute(options[2]) });
      },
    );
    const wrapper = shallowMount(FSLocationTextInput, {
      props,
      global: {
        provide: {
          getData: dataServiceSpy,
        },
      },
    });

    await flushPromises();
    return wrapper;
  };

  it("renders", async () => {
    const inputField = (await mountFsLocationTextInput()).findComponent(
      InputField,
    );
    expect(inputField.exists()).toBeTruthy();
  });

  it("shows relative-to-current-hubspace path", async () => {
    const path = "foo";
    props.modelValue = {
      path,
      timeout: 1000,
      fsCategory: "relative-to-current-hubspace",
    };
    expect(
      (await mountFsLocationTextInput()).findComponent(InputField).props()
        .modelValue,
    ).toBe(currentSpacePrefix + path);
  });

  it("shows CUSTOM_URL path", async () => {
    const path = "foo://bar";
    props.modelValue = {
      path,
      timeout: 1000,
      fsCategory: "CUSTOM_URL",
    };
    expect(
      (await mountFsLocationTextInput()).findComponent(InputField).props()
        .modelValue,
    ).toBe(path);
  });

  it("shows local paths", async () => {
    props.modelValue.fsCategory = "LOCAL";
    const path = "myLocalPath";
    props.modelValue.path = path;
    expect(
      (await mountFsLocationTextInput()).findComponent(InputField).props()
        .modelValue,
    ).toBe(absolute(path));
  });

  it("shows non-supported paths", async () => {
    const fsToString = "myFsPathString";
    props.modelValue.fsCategory = "RELATIVE" as any;
    props.modelValue.context = {
      fsToString,
    };
    expect(
      (await mountFsLocationTextInput()).findComponent(InputField).props()
        .modelValue,
    ).toBe(fsToString);
  });

  it.each(prefixes)(
    "emits %s FS location on text input prefixed with %s",
    async (fsCategory, prefix) => {
      const wrapper = await mountFsLocationTextInput();
      const path = "foo";
      wrapper
        .findComponent(InputField)
        .vm.$emit("update:model-value", prefix + path);
      expect(wrapper.emitted("update:modelValue")).toStrictEqual([
        [
          {
            fsCategory,
            path,
            timeout: wrapper.props().modelValue.timeout,
          },
        ],
      ]);
    },
  );

  it("emits CUSTOM_URL FS location on text input prefixed with valid scheme", async () => {
    const wrapper = await mountFsLocationTextInput();
    const path = "foo://bar";
    wrapper.findComponent(InputField).vm.$emit("update:model-value", path);
    expect(wrapper.emitted("update:modelValue")).toStrictEqual([
      [
        {
          fsCategory: "CUSTOM_URL",
          path,
          timeout: wrapper.props().modelValue.timeout,
        },
      ],
    ]);
  });

  it("emits relative-to-current-hubspace FS location on text input prefixed without valid scheme", async () => {
    const wrapper = await mountFsLocationTextInput();
    const path = "foo";
    wrapper.findComponent(InputField).vm.$emit("update:model-value", path);
    expect(wrapper.emitted("update:modelValue")).toStrictEqual([
      [
        {
          fsCategory: "relative-to-current-hubspace",
          path,
          timeout: wrapper.props().modelValue.timeout,
        },
      ],
    ]);
  });

  it("emits local FS location on text input prefixed without valid scheme in case of isLocal", async () => {
    props.isLocal = true;
    const wrapper = await mountFsLocationTextInput();
    const path = "foo";
    wrapper.findComponent(InputField).vm.$emit("update:model-value", path);
    expect(wrapper.emitted("update:modelValue")).toStrictEqual([
      [
        {
          fsCategory: "LOCAL",
          path,
          timeout: wrapper.props().modelValue.timeout,
        },
      ],
    ]);
  });
});
