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

## Runtime integration added

The launcher now uses a lifecycle-scoped reconnect controller:

- Retry budget and last failure timestamp are persisted in app-private SharedPreferences.
- Service disconnection, binding death and an uninitialized Binder endpoint feed the same bounded policy.
- Duplicate scheduled retries are suppressed.
- Reconnect delays are bounded by the policy; a stable 60-second connection resets the budget.
- The controller is cancelled when the Activity is destroyed.
- A manual retry remains available through the existing degraded-state screen.

This is app-process lifecycle recovery, not an always-on daemon. Delayed retries only execute while the process/looper remains alive; force-stop, process death, Doze and Android foreground-service restrictions remain authoritative. No boot receiver or automatic background foreground-service launch has been added. Device/emulator runtime testing is still required.

## Foreground lifecycle hardening

Reconnect attempts are now initiated only while MainActivity is resumed. Pending retries are cancelled on pause, and a resumed Activity makes a fresh bind attempt if no Binder is connected. This avoids trying to start the foreground Engine service from a background Activity callback on Android versions that restrict such starts.

ServiceConnection handles disconnection, binding death and a null Binder response through the same bounded retry policy. Retry state is written with one SharedPreferences editor transaction. The current CI run must still verify Kotlin compilation, app unit tests and release packaging.
