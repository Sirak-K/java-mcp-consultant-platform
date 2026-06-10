Generate a Candidate Presentation draft.

Follow the server-owned Candidate Presentation generation contract and resource context exactly.

Required workflow:

1. Use evidence supplied by `candidatePresentation.collectEvidence`.
2. Use the server-owned generation contract from `candidatePresentation.getGenerationContract` or `resource://candidate-presentation/generation-contract`.
3. Return one JSON object that matches the contract's `jsonSchema`.
4. Do not wrap the JSON in Markdown fences.
5. Do not include explanatory text outside the JSON object.

Generation discipline:

- Customer-facing content must be ready for a customer reader.
- Operations-only uncertainty, gaps, validation concerns, and evidence limitations belong only in `opsReviewContent`.
- Do not invent facts that are not supported by supplied MCP Java evidence.
- Do not expose MCP implementation details, runtime details, model details, or prompt mechanics in customer-facing content.
- Every Candidate Presentation artifact covers exactly one candidate matched to exactly one mission slot.
- Use `Mission Overview` instead of `Mission Context`.
- Put `Matched Role`, `Matched Primary Skills`, and `Matched Secondary Skills` under the single `Mission Slot Match Details` area.
- Do not create standalone matched-skill sections outside the mission-slot details area.
- Do not mention availability in `About the Matching Candidate`.
- Use customer-readable work mode labels such as on-premise, remote, or hybrid; never output runtime enum values such as `ON_PREMISE` in customer-facing content.
- Do not output CJK, Hiragana, Katakana, or Hangul characters in customer-facing content unless they are present in supplied evidence.
- Keep evidence trace compact and tied to meaningful generated claims.
