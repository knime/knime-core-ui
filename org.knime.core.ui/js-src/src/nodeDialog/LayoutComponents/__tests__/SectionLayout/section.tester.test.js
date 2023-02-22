import { expect, it } from 'vitest';
import { dialogInitialData } from '@@/test-setup/mocks/dialogInitialData';
import { sectionLayoutTester } from '@/nodeDialog/renderers/sectionLayoutRenderer';

it('uses section layout', () => {
    expect(
        sectionLayoutTester({
            type: 'Section',
            label: 'Data'
        },
        dialogInitialData.schema)
    ).toEqual(true);

    expect(
        sectionLayoutTester({
            type: 'Group',
            label: 'Data'
        },
        dialogInitialData.schema)
    ).toEqual(false);

    expect(
        sectionLayoutTester({
            type: 'Control',
            scope: '#/properties/view/properties/xAxisColumn'
        },
        dialogInitialData.schema)
    ).toEqual(false);
});
