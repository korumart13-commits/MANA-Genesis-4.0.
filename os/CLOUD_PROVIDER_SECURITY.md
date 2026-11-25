# MANA optional cloud provider security

## Current Android launcher prototype

The cloud gateway is a fallback, not the default path.

1. The local LiteRT-LM provider is the default. If it is ready, MANA Brain uses it unless the owner explicitly chooses a cloud provider for the next request.
2. If local inference is unavailable, or the owner explicitly requests cloud for one message, the user must confirm the provider choice for that request. A configured API key alone does not authorize a request.
3. Requests carrying retrieved local memory are marked sensitive and blocked from cloud routing.
4. The UI applies an additional keyword heuristic for obvious secrets and personal-data categories. This is only a safety net; it is not a reliable classifier and cannot guarantee that every sensitive prompt is detected.
5. The user can disable cloud use or remove a provider key from the cloud settings screen.

## Credential handling

- No provider API key is hard-coded in source, Gradle configuration, resources, or CI.
- User-entered keys are encrypted with AES-GCM using a key managed by Android Keystore.
- The encrypted credential file is stored in app-private internal storage and written through a temporary file followed by atomic replacement where supported.
- Requests use HTTPS and a fixed provider host. Redirects are disabled to prevent forwarding an Authorization header to another host.
- API keys are never included in error messages or normal chat logs.
- The user can remove a saved provider key; removing the selected provider key also disables cloud fallback.

An app on a user-controlled Android device cannot make a user-provided API key completely unrecoverable from a compromised/rooted device or from process memory while a request is in flight. Prefer provider-side spending limits and separate keys dedicated to MANA.

## Provider APIs

- OpenAI: Responses API; request storage is disabled with `store=false`.
- Anthropic: Messages API.
- Google Gemini: generateContent API.
- xAI: Responses API; request storage is disabled with `store=false`.
- Yandex AI Studio: Responses API; request storage is disabled with `store=false`. Adaptive-learning topic searches are a separate opt-in WorkManager task.

The user must supply a valid model identifier and API key for the selected provider. No provider account, API credits, license, or entitlement is included with MANA.

## Data boundary

A cloud request leaves the phone and is processed by the selected provider under that provider's terms and privacy policy. Do not send passwords, payment details, private keys, health information, contact records, diary entries, or other confidential information to cloud models. MANA's sensitive-content heuristic is not a substitute for user judgment.

## Release gate

Before production release, add provider-specific contract tests, TLS/redirect tests, credential removal tests, redaction tests, data-flow review, privacy disclosures, and Google Play Data safety declarations. Verify each provider's current API schema and terms before shipping.
