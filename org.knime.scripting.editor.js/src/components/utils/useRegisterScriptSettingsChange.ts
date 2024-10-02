import { getSettingsService } from "@/settings-service";
import { watch, onMounted } from "vue";

export const useRegisterScriptSettingsChange = (
  modelOrView: "model" | "view",
  getScript: () => string,
) => {
  onMounted(async () => {
    const register = await getSettingsService().registerSettings(modelOrView);
    const onScriptChange = register(getScript());
    watch(getScript, () => {
      onScriptChange.setValue(getScript());
    });
  });
};
