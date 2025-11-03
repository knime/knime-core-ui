import { onMounted, onUnmounted, ref } from "vue";

// Only turn on focus highlighting when we hear one of these.
const listOfFocusKeys = [
  "Tab",
  "ArrowUp",
  "ArrowDown",
  "ArrowLeft",
  "ArrowRight",
  " ",
  "Enter",
];

/*
 * Why do we want this? So that we can have focus highlighting in the
 * scripting editor, but only when navigating by keyboard. When navigating
 * by mouse, we don't need the focused elements to be highlighted because
 * the mouse pointer already does that job for us.
 */
/**
 * Composable that returns whether focus is currently painted. It turn focus
 * painting on when the document is navigated using key presses, and off again
 * when the document is clicked.
 */
const useShouldFocusBePainted = () => {
  const shouldFocusBePainted = ref<boolean>(false);

  const turnFocusPaintingOn = (evt: KeyboardEvent) => {
    if (listOfFocusKeys.includes(evt.key)) {
      shouldFocusBePainted.value = true;
    }
  };

  const turnFocusPaintingOff = () => {
    shouldFocusBePainted.value = false;
  };

  onMounted(() => {
    document.addEventListener("keydown", turnFocusPaintingOn);
    document.addEventListener("mousedown", turnFocusPaintingOff);
  });

  onUnmounted(() => {
    // Clean up - remove event listeners and stored data
    document.removeEventListener("keydown", turnFocusPaintingOn);
    document.removeEventListener("mousedown", turnFocusPaintingOff);
  });

  return shouldFocusBePainted;
};

export default useShouldFocusBePainted;
