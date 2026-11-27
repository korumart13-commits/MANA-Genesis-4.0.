# AI collaboration inbox

This directory is the shared hand-off point for ChatGPT and Claude. Store short Markdown reviews, design proposals, test reports, and hand-off notes here. Do not store large binaries, model files, secrets, credentials, personal data, or full build archives.

## Naming
- `claude-audit-YYYY-MM-DD.md`
- `claude-proposal-YYYY-MM-DD-topic.md`
- `chatgpt-review-YYYY-MM-DD-topic.md`
- `handoff-YYYY-MM-DD-topic.md`

Never overwrite another agent's report. Use a new filename.

## Report template
```markdown
# <Title>

- Author/agent:
- Date:
- Base commit:
- Scope:

## Summary
## Verified findings
| Priority | Finding | Evidence (path/line/CI) | Impact |
## Implemented vs documented-only
## Tests run
| Command/workflow | Result | Evidence |
## Risks and unknowns
## Recommendations
## Proposed PR scope
## Rollback
```

All claims must be marked VERIFIED, INFERRED, PROPOSED, or UNKNOWN. Link to exact source paths, commits, PRs, and CI runs. Do not claim tests passed unless logs prove it.
