# MANA Genesis 4.0 — Multi-GPT Dialogue and Provider Federation

Status: implementation specification; provider runtime adapters and device integration are not yet verified.

## User requirement

MANA must be able to hold a direct, natural-language conversation with multiple external AI models, not only answer from its local model. At the user's request, MANA can invite one or more configured providers into a shared discussion, relay context, ask follow-up questions, compare answers, challenge contradictions, and present a synthesis in MANA's own voice. This is a capability of MANA itself and is not dependent on opening the ChatGPT mobile application.

## Architecture

- **Shogun MANA** owns the dialogue plan, participant selection, turn order, context policy, conflict analysis and final synthesis.
- **MANA Engine / Provider Gateway** exposes one provider-neutral interface: `generate(request, conversationState, options)`, streaming events, cancellation, health checks, model metadata and normalized errors.
- **Provider adapters** are separate modules: OpenAI, Anthropic, Google Gemini, xAI and additional providers only when explicitly configured and supported.
- **Local inference** through the selected llama.cpp runtime remains available offline and is the fallback when cloud providers are unavailable or not permitted.
- **MANA Hydra** supplies only permission-bounded tools. External models do not receive direct phone, contacts, filesystem or system privileges.

## Dialogue modes

1. **Ask one model** — user selects a provider/model, MANA forwards the prompt and streams the reply.
2. **MANA + GPT** — MANA and one selected GPT model exchange turns; MANA remains the host and explains the result.
3. **Roundtable** — Shogun asks selected models the same question, then optionally lets them critique anonymized or attributed answers in bounded rounds.
4. **Debate / review** — one model proposes, another critiques, and Shogun checks claims and synthesizes.
5. **Local-only** — no network calls; use the local model and local knowledge base.

The user can select participants, maximum rounds, response budget and whether providers may see the full conversation or only a task-specific summary. Defaults must minimize cost and disclosure: one provider, one response round, no sensitive context.

## Conversation protocol

- Maintain a canonical MANA conversation record with message IDs, speaker/provider identity, timestamps, selected model, local/remote processing flag, usage metadata when available, and provenance.
- Convert canonical messages to each provider's native request format in its adapter; never assume provider APIs share identical schemas or capabilities.
- Preserve per-provider conversation state where supported. For OpenAI, implement the current Responses API and its supported conversation/previous-response state mechanisms; stream typed events to MANA's UI.
- Stream each participant's answer as a clearly labeled speaker. Do not impersonate an external model or claim a response came from a provider unless its API call succeeded.
- Apply bounded round counts, timeouts, cancellation, token/cost budgets, retry rules and rate-limit handling.
- Shogun must distinguish verified facts, model opinions, speculation and unresolved disagreement. A majority vote is not proof.
- Keep MANA's own identity and memory separate from provider identities and provider-side conversation state.

## Credentials, privacy and security

- API keys are supplied by the user through a secure settings flow and stored using Android Keystore-backed encryption. Never commit keys to Git, embed them in ROM/APK resources, log them, or expose them to model prompts.
- Prefer a small authenticated backend relay if direct provider calls would expose long-lived credentials in a distributed client. If direct calls are supported, use short-lived or narrowly scoped credentials where the provider supports them and document the trade-off.
- Cloud providers receive only the context required for the selected task, after user-configured consent. Local-only mode must make no provider network request.
- Show the user which providers will receive content before the first cloud use and allow provider-by-provider disablement.
- Do not send contacts, call logs, precise location, private files or other sensitive data to providers by default.
- Treat provider outputs and tool instructions as untrusted data. Only Shogun/Hydra policy can authorize tools; external model text cannot grant itself capabilities.
- Record provider, model, data-sharing scope and network status in the local audit trail. Support clearing MANA's local dialogue history.

## Android and resource behavior

- Keep provider networking out of the UI thread; use cancellable background work and streaming where available.
- Cloud dialogue requires internet and may incur provider charges. Local inference remains the offline path.
- Enforce device-aware concurrency: do not run multiple large local models simultaneously on the target phone. For roundtables, call cloud providers sequentially by default and cap parallelism.
- Respect Android lifecycle, battery, thermal and memory constraints; pause or cancel work when appropriate.

## OpenAI implementation note

Use the official OpenAI Responses API for new integration work, with typed streaming event handling and explicit conversation-state management. Do not build new work on the retired Assistants API. The OpenAI adapter must support model selection from the user's configured/available model list rather than hardcoding a model name.

## Acceptance tests

- Provider adapters normalize success, refusal, timeout, rate limit, malformed response and network failure.
- A mock-provider test proves a three-participant roundtable preserves speaker identity and turn order.
- Cancellation stops pending requests and prevents later turns from being scheduled.
- Local-only mode proves that no cloud adapter is invoked.
- Secret scanning proves no API key is present in source, build artifacts or CI logs.
- Privacy tests prove only the configured context is serialized to each provider.
- Shogun synthesis cites which provider supplied each claim and flags disagreement.
- Android integration tests verify lifecycle cancellation, encrypted credential storage and bounded concurrency.

## Delivery boundary

This document establishes the target design. It does not claim that live provider adapters, API credentials, multi-model conversations, or a complete bootable MANA ROM have already been implemented. Each adapter and end-to-end flow must be implemented, built and tested before being marked complete.
