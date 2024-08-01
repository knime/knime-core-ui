import { ref } from "vue";
import {
  type GenericInitialData,
  type InitialDataServiceType,
  type PortConfigs,
} from "@/initial-data-service";
import type { InputOutputModel } from "lib/main";

export const DEFAULT_INPUT_OBJECTS: InputOutputModel[] = [
  {
    name: "Input table 1",
    portType: "table",
    subItems: [
      {
        name: "Column 1",
        type: "Number",
        supported: true,
      },
      {
        name: "Column 2",
        type: "String",
        supported: true,
      },
      {
        name: "Column 3",
        type: "Something weird",
        supported: false,
      },
    ],
  },
];

export const DEFAULT_OUTPUT_OBJECTS: InputOutputModel[] = [
  {
    name: "Output table 1",
    portType: "table",
  },
];

export const DEFAULT_FLOW_VARIABLE_INPUTS: InputOutputModel = {
  name: "Flow Variables",
  portType: "flowVariable",
  subItems: [
    {
      name: "flowVar1",
      type: "Number",
      supported: true,
    },
    {
      name: "flowVar2",
      type: "String",
      supported: true,
    },
    {
      name: "flowVar3",
      type: "Bit Vector",
      supported: false,
    },
  ],
};

const DEFAULT_PORT_CONFIGS: PortConfigs = {
  inputPorts: [
    {
      nodeId: "root",
      portName: "firstPort",
      portIdx: 1,
      portViewConfigs: [
        { portViewIdx: 0, label: "firstView" },
        { portViewIdx: 1, label: "secondView" },
      ],
    },
    {
      nodeId: "notRoot",
      portName: "firstPort",
      portIdx: 1,
      portViewConfigs: [
        { portViewIdx: 0, label: "firstView" },
        { portViewIdx: 1, label: "secondView" },
      ],
    },
  ],
};

export const DEFAULT_INITIAL_DATA: GenericInitialData = {
  inputObjects: DEFAULT_INPUT_OBJECTS,
  outputObjects: DEFAULT_OUTPUT_OBJECTS,
  flowVariables: DEFAULT_FLOW_VARIABLE_INPUTS,
  inputPortConfigs: DEFAULT_PORT_CONFIGS,
  kAiConfig: {
    codeAssistantEnabled: true,
    codeAssistantInstalled: true,
    hubId: "My Mocked KNIME Hub",
    loggedIn: true,
  },
  inputsAvailable: true,
};

const log = (message: any, ...args: any[]) => {
  if (typeof consola === "undefined") {
    // eslint-disable-next-line no-console
    console.log(message, ...args);
  } else {
    consola.log(message, ...args);
  }
};

export const createInitialDataServiceMock = (
  data?: GenericInitialData,
): InitialDataServiceType => ({
  getInitialData: () => {
    log("Called initial data service mock getInitialData");
    return Promise.resolve(data ?? DEFAULT_INITIAL_DATA);
  },
  isInitialDataLoaded: () => {
    log("Called initial data service mock isInitialDataLoaded");
    return ref(true);
  },
});
