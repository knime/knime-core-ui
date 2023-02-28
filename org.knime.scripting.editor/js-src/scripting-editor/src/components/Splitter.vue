<script>
/**
 * Splitter
 * A component that acts as wrapper with  a primary (default slot) and secondary (slot: secondary) area.
 * The areas are separated by a vertical (direction: row) or a horizontal line (direction: column)
 * The height or width of the secondary area can be changed by the user using click and drag.
 * The current value of the height or width is saved to the local store and loaded on mount.
 */
export default {
    props: {
        /**
         * direction - like `flex-direction` (no reverse support)
         */
        direction: {
            type: String,
            default: 'column',
            validator: val => ['column', 'row'].includes(val),
        },
        /**
         * id is used as html-id and to load and save state
         */
        id: {
            type: String,
            required: true,
        },
        /**
         * initial size of secondary area
         */
        secondarySize: {
            type: String,
            default: '40%',
            validator: (str) => /^\d+[%\w]+$/.test(str),
        },
    },
    data() {
        return {
            isMove: false,
            currentSecondarySize: this.secondarySize,
        };
    },
    computed: {
        isColumn() {
            return this.direction === 'column';
        },
        isRow() {
            return this.direction === 'row';
        },
    },
    watch: {
        currentSecondarySize() {
            if (this.supportLocalStorage()) {
                localStorage.setItem(`ui-splitter-${this.id}`, this.currentSecondarySize);
            }
        },
    },
    beforeMount() {
        if (this.supportLocalStorage()) {
            this.currentSecondarySize = localStorage.getItem(`ui-splitter-${this.id}`) || this.secondarySize;
        }
    },
    methods: {
        supportLocalStorage() {
            return typeof localStorage !== 'undefined';
        },
        beginMove(e) {
            this.$refs.handle.setPointerCapture(e.pointerId);
            this.isMove = true;
        },
        stopMove(e) {
            this.$refs.handle.releasePointerCapture(e.pointerId);
            this.isMove = false;
        },
        move(e) {
            if (this.isMove) {
                const rect = this.$refs.secondary.getBoundingClientRect();
                if (this.isColumn) {
                    this.currentSecondarySize = `${rect.height + (rect.y - e.clientY)}px`;
                } else {
                    this.currentSecondarySize = `${rect.width + (rect.x - e.clientX)}px`;
                }
            }
        },
    },
};
</script>

<template>
  <div
    :id="id"
    :class="['splitter', direction]"
  >
    <div class="primary">
      <slot>Primary</slot>
    </div>
    <div
      ref="handle"
      :class="{'handle': true, 'active': isMove }"
      @pointerdown.left="beginMove"
      @pointerup="stopMove"
      @pointermove="move"
    />
    <div
      ref="secondary"
      class="secondary"
      :style="{ 'height': isColumn && currentSecondarySize, 'width': isRow && currentSecondarySize }"
    >
      <slot name="secondary">Secondary</slot>
    </div>
  </div>
</template>

<style lang="postcss" scoped>
.splitter {
  overflow: hidden;
  flex: 1 1 auto;
  align-items: stretch;
  display: flex;

  & .primary {
    overflow: auto;
    flex: 1 1 auto;
    min-height: 25%;
    max-height: 100%;
  }

  & .secondary {
    overflow: auto;
    flex: 0 0 auto;
    min-height: 15%;
    max-height: 75%;
  }

  & .handle {
    flex: 0 0 auto;
    background-color: var(--knime-silver-sand);
    background-clip: content-box;
    z-index: 1;
    padding: 0 3px;
    margin: 0 -3px;
    width: 7px;
    cursor: ew-resize;

    &:hover {
      background-color: var(--knime-dove-gray);
    }

    &.active {
      background-color: var(--knime-masala);
      cursor: col-resize;

    }
  }

  &.column {
    flex-direction: column;
    height: 100%;

    & .primary {
      min-height: 25%;
    }

    & .secondary {
      min-height: 35%;
    }

    & .handle {
      padding: 3px 0;
      margin: -3px 0;
      height: 7px;
      width: 100%;
      cursor: ns-resize;

      &.active {
        cursor: row-resize;
      }
    }
  }

  &.row {
    flex-direction: row;

    & .primary {
      min-width: 25%;
    }

    & .secondary {
      min-width: 15%;
    }

  }
}
</style>
