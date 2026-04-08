---
name: map
description: "Visualize logic using Mermaid diagrams before analysis."
aliases: ["/map", "/visualize", "/diagram"]
collaboration:
  inputs: ["complex_logic", "architecture", "workflow"]
  outputs: ["mermaid_code", "visual_explanation"]
  next_skills: ["coach", "knowledge"]
---

# Visual Mapping Protocol 🗺️

## Directive
Always start by mapping the user's problem into a Mermaid diagram.

## Rules
- **Primary Format**: Use Unicode/ASCII Art for all visualizations (e.g., `┌──┐`, `──>`, `▼`).
- **Mermaid Sequence Diagrams** (Use only when explicitly requested):
  - **Participants**: Always use `participant ID as "Display Name"`. Quotes are mandatory.
  - **Activation**: `activate ID` must precede any outgoing arrows. `deactivate ID` must close the block.
  - **Notes**: `Note over/right/left of` must be on its own line.
  - **Styling**: Use `rect rgb(r, g, b)` for highlighting asynchronous or critical sections.
  - **Safety**: Avoid deep nesting of `alt/loop`. Keep logic linear where possible.

## Output Format
1. **The Visual Map**: Use Unicode/ASCII art (boxes, arrows, trees) to visualize the logic.
   - Example: `┌──┐`, `──>`, `▼`, `├──`
   - Goal: Look like a diagram but remain plain text.
2. **The Legend**: Briefly explain key nodes or symbols.
3. **The Insight**: Follow up with detailed textual analysis based on the visual map.
