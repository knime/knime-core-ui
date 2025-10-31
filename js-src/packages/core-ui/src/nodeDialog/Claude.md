# NodeDialog Refactoring Task

## Context

The current `NodeDialog.vue` is a UIExtension that communicates directly with the embedder and the backend as an autonomous app.

## Goal

Split the NodeDialog into two parts:

1. **Thin communication layer** (outer wrapper) - Handles communication with embedder and backend
2. **Inner component** - The rest of the NodeDialog logic

## Use Case

Reuse the NodeDialog as a component within another UI extension (SQL Editor) that displays a NodeDialog only for parts of the settings (additional settings).

## Outer Layer Responsibilities

- Dirty settings registry
- Current data publishing
- Issuing RPC calls (in the same form as `jsonDataService.data` method)
- Providing initial data to inner component
- Getting apply data in non-serialized form (for combining with other apply data in SQL Editor)

## Current Task

Spec out the API of the inner component (props, emits, expose).
