# Text Fields Height - Quick Reference

## ✅ Standard Heights Across All Screens

All text fields in Craftoria use **48.dp minimum height** for consistency and accessibility.

---

## Component Usage Guide

### 1. Single-Line Text Fields
```kotlin
CraftoriaTextField(
    value = email,
    onValueChange = { email = it },
    label = "Email",
    placeholder = "your@email.com"
    // ✅ Automatically uses 48.dp minimum
)
```

**Used in:** Login, Registration, Checkout, Profile, Search

---

### 2. Multi-Line Text Fields
```kotlin
StandardizedOutlinedTextField(
    value = description,
    onValueChange = { description = it },
    label = "Description",
    minLines = 3,
    maxLines = 5
    // ✅ Automatically uses 48.dp minimum, expands with content
)
```

**Used in:** Store descriptions, Notes, Addresses, Comments

---

### 3. Compact/Inline Fields
```kotlin
StandardizedOutlinedTextFieldCompact(
    value = searchQuery,
    onValueChange = { searchQuery = it },
    placeholder = "Search..."
    // ✅ Automatically uses 48.dp minimum, no label
)
```

**Used in:** Search bars, Quick filters, Inline inputs

---

## ❌ What NOT to Do

### ❌ Don't use fixed heights
```kotlin
// ❌ WRONG - Creates inconsistent sizing
OutlinedTextField(
    modifier = Modifier.height(54.dp)  // Too tall!
)
```

### ❌ Don't use excessive heights
```kotlin
// ❌ WRONG - Oversized text field
OutlinedTextField(
    modifier = Modifier.height(80.dp)  // Way too tall!
)
```

### ❌ Don't mix sizing approaches
```kotlin
// ❌ WRONG - Inconsistent with other fields
CraftoriaTextField(minHeight = 60)  // Non-standard
```

---

## ✅ What TO Do

### ✅ Use component defaults
```kotlin
// ✅ CORRECT - Uses standard 48.dp
CraftoriaTextField(
    value = email,
    onValueChange = { email = it }
)
```

### ✅ Use heightIn for flexibility
```kotlin
// ✅ CORRECT - Allows expansion, maintains minimum
OutlinedTextField(
    modifier = Modifier.heightIn(min = 48.dp)
)
```

### ✅ Use minLines/maxLines for multi-line
```kotlin
// ✅ CORRECT - Expands naturally with content
StandardizedOutlinedTextField(
    minLines = 3,
    maxLines = 5
)
```

---

## Spacing Standards

```
Label to field:     4-6.dp
Field to field:     12-14.dp
Field to button:    12-16.dp
```

---

## Current Status

| Component | Height | Status |
|-----------|--------|--------|
| CraftoriaTextField | 48.dp min | ✅ Standardized |
| StandardizedOutlinedTextField | 48.dp min | ✅ Standardized |
| StandardizedOutlinedTextFieldCompact | 48.dp min | ✅ Standardized |
| LoginScreen Dropdown | 48.dp min | ✅ Fixed |
| All Other Screens | 48.dp min | ✅ Verified |

---

## Accessibility Benefits

- ✅ **Touch Target Size:** 48.dp meets WCAG guidelines
- ✅ **Consistency:** Users know what to expect
- ✅ **Readability:** Proper spacing for text
- ✅ **Professional:** Uniform appearance across app

---

## When Adding New Text Fields

1. **Use CraftoriaTextField** for single-line inputs
2. **Use StandardizedOutlinedTextField** for multi-line inputs
3. **Never set explicit `.height()`**
4. **Always use `.heightIn(min = 48.dp)`** if needed
5. **Maintain 12-14.dp spacing** between fields

---

## Quick Checklist

- [ ] Using correct component (CraftoriaTextField, StandardizedOutlinedTextField, etc.)
- [ ] No explicit `.height()` modifiers
- [ ] Using `.heightIn(min = 48.dp)` if custom sizing needed
- [ ] Proper spacing (12-14.dp) between fields
- [ ] Label and placeholder text properly sized
- [ ] Multi-line fields use `minLines`/`maxLines`

---

**Last Updated:** May 27, 2026  
**Status:** ✅ All text fields standardized to 48.dp minimum height
