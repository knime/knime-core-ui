import { type Ref, ref } from "vue";

const readOnly = ref(false);

// Make it available for other parts of the application
export const useReadonlyStore = (): Ref<boolean> => readOnly;
