import type { InputOutputModel } from "./components/InputOutputItem.vue";
import {
  type GenericInitialData,
  type InputConnectionInfo,
  type PortConfigs,
} from "./initial-data-service";

export const DEFAULT_INPUT_OBJECTS: InputOutputModel[] = [
  {
    name: "Input table 1",
    portType: "table",
    codeAlias: "knio.input_tables[0].to_pandas()",
    multiSelection: true,
    subItemCodeAliasTemplate: `knio.input_tables[0][
          {{~#if subItems.[1]~}}
              [{{#each subItems}}"{{{escapeDblQuotes this.name}}}"{{#unless @last}},{{/unless}}{{/each}}]
          {{~else~}}
              "{{{escapeDblQuotes subItems.[0].name}}}"
          {{~/if~}}
      ].to_pandas()`,
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

export const DEFAULT_VIEW_OBJECTS: InputOutputModel[] = [
  {
    name: "View Table",
    portType: "view",
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

export const DEFAULT_PORT_CONFIGS: PortConfigs = {
  inputPorts: [
    {
      nodeId: "root:1",
      portName: "Variable Inport",
      portIdx: 1,
      portViewConfigs: [{ portViewIdx: 0, label: "Flow variables" }],
    },
    {
      nodeId: "root:2",
      portName: "Object from a node",
      portIdx: 1,
      portViewConfigs: [{ portViewIdx: 0, label: "Object" }],
    },
    {
      // unconnected port
      nodeId: undefined,
      portIdx: 0,
      portViewConfigs: [],
      portName: undefined,
    },
    {
      nodeId: "root:3",
      portName: "Table from another node",
      portIdx: 1,
      portViewConfigs: [
        { portViewIdx: 0, label: "Table" },
        { portViewIdx: 1, label: "Statistics" },
      ],
    },
  ],
};

export const DEFAULT_INITIAL_DATA: GenericInitialData = {
  inputObjects: DEFAULT_INPUT_OBJECTS,
  outputObjects: DEFAULT_OUTPUT_OBJECTS,
  flowVariables: DEFAULT_FLOW_VARIABLE_INPUTS,
  inputPortConfigs: DEFAULT_PORT_CONFIGS,
  viewObjects: DEFAULT_VIEW_OBJECTS,
  kAiConfig: {
    hubId: "My Mocked KNIME Hub",
    isKaiEnabled: true,
  },
  inputConnectionInfo: DEFAULT_PORT_INFORMATION,
};
