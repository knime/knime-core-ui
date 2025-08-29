import type {
  GenericInitialData,
  GenericNodeSettings,
  InputOutputModel,
} from "../lib/main";

export const DEFAULT_INPUT_OBJECTS: InputOutputModel[] = [
  {
    name: "Input table 1",
    portType: "table",
    subItems: [
      {
        name: "Column 1",
        type: {
          id: "org.knime.core.data.IntValue",
          title: "Number (Integer)",
          displayName: "Integer",
        },
        supported: true,
      },
      {
        name: "Column 2",
        type: {
          id: "org.knime.core.data.StringValue",
          title: "String",
          displayName: "String",
        },
        supported: true,
      },
      {
        name: "Column 3",
        type: {
          id: "org.knime.core.data.DoubleValue",
          title: "Number (Double)",
          displayName: "Double",
        },
        supported: true,
      },
      {
        name: "Column 4",
        type: {
          id: "org.knime.core.data.time.localdate.LocalDateValue",
          title: "Local Date",
          displayName: "Local Date",
        },
        supported: true,
      },
      {
        name: "Column 4",
        type: { displayName: "unsupported" },
        supported: false,
      },
    ],
  },
];

export const DEFAULT_FLOW_VARIABLE_INPUTS: InputOutputModel = {
  name: "Flow Variables",
  portType: "flowVariable",
  subItems: [
    {
      name: "flowVar1",
      type: { id: "STRING", title: "String", displayName: "String" },
      supported: true,
    },
    {
      name: "flowVar2",
      type: {
        id: "FSLocation",
        title: "File System Location",
        displayName: "File System Location",
      },
      supported: true,
    },
    {
      name: "flowVar3",
      type: {
        id: "BOOLEANARRAY",
        title: "Bit Vector",
        displayName: "Bit Vector",
      },
      supported: false,
    },
  ],
};

export const DEFAULT_INITIAL_DATA: GenericInitialData = {
  inputObjects: DEFAULT_INPUT_OBJECTS,
  outputObjects: [
    {
      name: "Output table 1",
      portType: "table",
    },
  ],
  flowVariables: DEFAULT_FLOW_VARIABLE_INPUTS,
  inputPortConfigs: {
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
    ],
  },
  kAiConfig: {
    hubId: "Demo KNIME Hub",
    isKaiEnabled: true,
  },
  inputConnectionInfo: [
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
  ],
};

export const DEFAULT_INITIAL_SETTINGS: GenericNodeSettings = {
  settingsAreOverriddenByFlowVariable: false,
  script: `# Demo Python Script
import knime.scripting.io as knio
import pandas as pd

# Read the input table
input_table = knio.input_tables[0].to_pandas()

# Do some processing
output_table = input_table.copy()
output_table['demo_column'] = output_table['Column 1'] * 2

# Write the output table  
knio.output_tables[0] = knio.Table.from_pandas(output_table)

print("Script executed successfully!")`,
};
