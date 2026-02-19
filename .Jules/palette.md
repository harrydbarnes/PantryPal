## 2024-05-23 - Accessibility of Clickable Overlays
**Learning:** Placing a `clickable` `Box` overlay on top of a `TextField` (to make it a button) obscures the underlying field's content from screen readers like TalkBack, creating a major accessibility regression.
**Action:** Always add `Modifier.semantics { contentDescription = ... }` to the overlay to explicitly report the field's label and current value.
