# MANA Engine resilience policy — 2026-11-27

## Implemented

Added ManaServiceResiliencePolicy, a deterministic, platform-independent retry-budget component for transient app-process/service failures.

- Exponential delays start at 2 seconds and cap at 60 seconds.
- Five retries are allowed by default; the next failure reports an exhausted budget and schedules no retry.
- A failure window bounds repeated failures; failures after the window begin a fresh budget.
- The budget resets only after a full stable-health window (60 seconds), not merely after a successful bind.
- The policy does not bypass Android background execution limits, user force-stop, battery restrictions, or foreground-service start rules.

## Integration boundary

This commit adds the policy and unit tests only. Scheduling, persisted crash-loop state, Binder death monitoring and WorkManager integration remain separate tasks and are not claimed complete. The policy must be integrated with lifecycle-aware reconnect logic before being described as active runtime resilience.

## Required next steps

1. Persist the retry budget atomically in app-private preferences.
2. Handle ServiceConnection disconnection, binding death and IBinder death without duplicate reconnect jobs.
3. Use bounded WorkManager work for deferred recovery where Android permits it; user-initiated retry remains available in diagnostics.
4. Reset the budget only after the Engine remains healthy for the stable window.
5. Test process death, duplicate callbacks, force-stop behavior and Android 16 background-start restrictions on emulator and RMX5566.
