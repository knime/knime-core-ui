import type { InputOutputModel } from "@/components/InputOutputItem.vue";
import {
  type GenericInitialData,
  type InputConnectionInfo,
  type PortConfigs,
} from "@/initial-data-service";

export const DEFAULT_INPUT_OBJECTS: InputOutputModel[] = [
  {
    name: "Input table 1",
    portType: "table",
    subItems: [
      {
        name: "Column 1",
        type: { displayName: "Number" },
        supported: true,
      },
      {
        name: "Column 2",
        type: { displayName: "String", id: "string-datatype", title: "String" },
        supported: true,
      },
      {
        name: "Column 3",
        type: { displayName: "Something weird" },
        supported: false,
      },
    ],
  },
];

export const DEFAULT_PORT_INFORMATION: InputConnectionInfo[] = [
  {
    // flow variable port
    status: "OK",
    isOptional: true,
  },
  {
    // input port
    status: "OK",
    isOptional: false,
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
      type: { displayName: "Number" },
      supported: true,
    },
    {
      name: "flowVar2",
      type: { displayName: "String", id: "string-datatype", title: "String" },
      supported: true,
    },
    {
      name: "flowVar3",
      type: {
        displayName: "Bit Vector",
        id: "bit-byte-vector-datatype",
        title: "Bit Vector",
      },
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
    hubId: "My Mocked KNIME Hub",
    isKaiEnabled: true,
  },
  inputConnectionInfo: DEFAULT_PORT_INFORMATION,
};
