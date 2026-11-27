# MANA Brain engineering copilot

The Brain module now has a bounded GPT/cloud-assisted engineering proposal layer.

## Contract

- Uses an already-configured `InferenceProvider`; the existing OpenAI Responses adapter is reused.
- Requires explicit owner approval to share source context and explicit cloud permission for each task.
- Accepts only a bounded set of owner-selected source files.
- Redacts common API-key, token, password and private-key patterns before prompt construction.
- Requests a structured unified diff, summary, test plan and risk notes.
- Validates diff paths against approved MANA source workspaces and rejects path traversal, kernel/device trees, signing keys, build output and workflow files.
- Returns every result as `REVIEW_REQUIRED`. It never applies a patch, runs generated code, commits to Git, installs an APK or controls the phone.

## Required next integration

A trusted Android host must obtain owner-approved file context, instantiate this copilot with the selected configured provider, display the proposal and test plan, and route an explicitly approved diff into the isolated self-development build pipeline. The host must not turn a model response into shell execution.

## Limits

This module does not include API credentials. The owner must configure their own provider key in secure storage. Provider access, network connectivity, quota and data-sharing consent remain required.
