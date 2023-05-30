<script setup lang="ts">
import { computed,
    ref,
    type FunctionalComponent,
    type SVGAttributes } from 'vue';
import { useStore } from 'vuex';
import type { Editor } from '@tiptap/vue-3';

import FunctionButton from 'webapps-common/ui/components/FunctionButton.vue';
import SubMenu from 'webapps-common/ui/components/SubMenu.vue';
import type { Level } from '@tiptap/extension-heading';
import DropdownIcon from 'webapps-common/ui/assets/img/icons/arrow-dropdown.svg';
import BoldIcon from '@/assets/bold.svg';
import ItalicIcon from '@/assets/italic.svg';
import UnderlineIcon from '@/assets/underline.svg';
import BulletListIcon from '@/assets/unordered-list.svg';
import OrderedListIcon from '@/assets/ordered-list.svg';
import AlignLeftIcon from '@/assets/align-left.svg';
import AlignCenterIcon from '@/assets/align-center.svg';
import AlignRightIcon from '@/assets/align-right.svg';

interface Props {
  editor: Editor;
}

interface ToolbarItem {
  id: string;
  name: string;
  icon: FunctionalComponent<SVGAttributes>;
  hotkey: any;
  onClick: () => void;
  active?: () => boolean;
}

const store = useStore();

const props = defineProps<Props>();

// eslint-disable-next-line func-call-spacing
const emit = defineEmits<{
  (e: 'previewBorderColor', color: string): void;
  (e: 'changeBorderColor', color: string): void;
}>();

const editorTools: Array<ToolbarItem> = [
    {
        id: 'bold',
        name: 'Bold',
        hotkey: ['Ctrl', 'B'],
        icon: BoldIcon,
        active: () => props.editor.isActive('bold'),
        onClick: () => props.editor.chain().focus().toggleBold().run()
    },
    {
        id: 'italic',
        name: 'Italic',
        icon: ItalicIcon,
        hotkey: ['Ctrl', 'I'],
        active: () => props.editor.isActive('italic'),
        onClick: () => props.editor.chain().focus().toggleItalic().run()
    },
    {
        id: 'underline',
        name: 'Underline',
        icon: UnderlineIcon,
        hotkey: ['Ctrl', 'U'],
        active: () => props.editor.isActive('underline'),
        onClick: () => props.editor.chain().focus().toggleUnderline().run()
    },
    {
        id: 'bullet-list',
        name: 'Bullet list',
        icon: BulletListIcon,
        hotkey: ['Ctrl', 'Shift', '8'],
        active: () => props.editor.isActive('bulletList'),
        onClick: () => props.editor.chain().focus().toggleBulletList().run()
    },
    {
        id: 'bullet-list-numbered',
        name: 'Ordered list',
        icon: OrderedListIcon,
        hotkey: ['Ctrl', 'Shift', '7'],
        active: () => props.editor.isActive('orderedList'),
        onClick: () => props.editor.chain().focus().toggleOrderedList().run()
    },
    {
        id: 'align-left',
        icon: AlignLeftIcon,
        name: 'Align left',
        hotkey: ['Ctrl', 'Shift', 'L'],
        active: () => props.editor.isActive({ textAlign: 'left' }),
        onClick: () => props.editor.chain().focus().setTextAlign('left').run()
    },
    {
        id: 'align-center',
        icon: AlignCenterIcon,
        name: 'Align center',
        hotkey: ['Ctrl', 'Shift', 'E'],
        active: () => props.editor.isActive({ textAlign: 'center' }),
        onClick: () => props.editor.chain().focus().setTextAlign('center').run()
    },
    {
        id: 'align-right',
        icon: AlignRightIcon,
        name: 'Align right',
        hotkey: ['Ctrl', 'Shift', 'R'],
        active: () => props.editor.isActive({ textAlign: 'right' }),
        onClick: () => props.editor.chain().focus().setTextAlign('right').run()
    }
];

// +1 to include the border color tool
const totalEditorTools = computed(() => editorTools.length + 1);

const headingPresets = computed(() => {
    // eslint-disable-next-line no-magic-numbers
    const levels: Level[] = [1, 2, 3, 4, 5, 6];

    const getCurrentLevel = () => levels.find((level) => props.editor.isActive('heading', { level })) || 1;

    const base = [
        {
            text: 'Normal text',
            selected: !props.editor.isActive('heading'),
            onClick: () => props.editor
                .chain()
                .focus()
                .toggleHeading({ level: getCurrentLevel() })
                .run()
        }
    ];

    const headings = levels.map((level) => ({
        text: `Heading ${level}`,
        selected: props.editor.isActive('heading', { level }),
        onClick: () => props.editor.chain().focus().setHeading({ level }).run()
    }));

    return base.concat(headings);
});

const selectedHeadingText = computed(
    () => headingPresets.value.find((heading) => heading.selected)?.text
);

const toolbarItemPadding = 8;
const toolbarItemGap = 4;
const toolbarItemSize = 32;
const headingDropdownWidth = 115;

const toolbarWidth =
  /* account for padding on both ends */
  toolbarItemPadding * 2 +
  /* account for all items */
  totalEditorTools.value * toolbarItemSize +
  /* add space for heading dropdown */
  headingDropdownWidth +
  toolbarItemGap +
  /* include gaps (total gaps = total items - 1) */
  toolbarItemGap * (totalEditorTools.value - 1);

</script>

<template>
  <div
    class="editor-toolbar"
  >
    <SubMenu
      :items="headingPresets"
      orientation="right"
      :teleport-to-body="false"
      positioning-strategy="absolute"
      class="heading-menu"
      @item-click="(e, item) => item.onClick()"
    >
      <span class="heading-current-text">{{ selectedHeadingText }}</span>
      <DropdownIcon />
    </SubMenu>
    <FunctionButton
      v-for="tool of editorTools"
      :key="tool.icon"
      :active="tool.active ? tool.active() : false"
      class="toolbar-button"
      @click.stop="tool.onClick"
    >
      <Component :is="tool.icon" />
    </FunctionButton>
  </div>
</template>

<style lang="postcss" scoped>


.editor-toolbar {
    margin-left: -50px;
  display: flex;
  justify-content: center;
  background: var(--knime-white);
  gap: calc(v-bind(toolbarItemGap) * 1px);
  padding: calc(v-bind(toolbarItemPadding) * 1px);
  width: calc(v-bind(toolbarWidth) * 1px);
  height: 48px;
  box-shadow: 0 0 10px rgb(62 58 57 / 30%);
  border-radius: 30px;

  & .toolbar-button,
  & .border-color-tool {
    width: calc(v-bind(toolbarItemSize) * 1px);
    height: calc(v-bind(toolbarItemSize) * 1px);
    padding: 0;
    justify-content: center;
    align-items: center;
  }

  & .toolbar-button {
    & svg {
      width: calc(calc(v-bind(toolbarItemSize) - 5) * 1px);
      height: calc(calc(v-bind(toolbarItemSize) - 5) * 1px);
    }
  }

  & .heading-menu {
    width: calc(v-bind(headingDropdownWidth) * 1px);

    & .heading-current-text {
      max-width: 100%;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }

    & :deep(.submenu-toggle) {
      width: 100%;
      padding: 0 10px;
      height: calc(v-bind(toolbarItemSize) * 1px);
      justify-content: center;
      align-items: center;
    }

    & :deep(.submenu-toggle.expanded) svg {
      transform: scaleY(-1);
    }
  }

  & .border-color-tool:hover {
    & svg {
      fill: white;
    }
  }
}
</style>
