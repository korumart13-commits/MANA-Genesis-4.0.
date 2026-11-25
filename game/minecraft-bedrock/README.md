# MANA Genesis — Minecraft Bedrock bridge starter

This is a Behavior Pack source for Minecraft Bedrock using the official stable `@minecraft/server` Script API dependency `2.11.0`.

## What it does

- Emits visible in-world telemetry for player spawn/leave, block placement/breaking and dimension changes.
- Accepts a restricted `/scriptevent mana:command {"action":"ping"}` or `status` handshake only from an entity carrying the `mana_owner` tag.
- Does not transmit data over the network, modify inventory, move the player, build structures, automate combat, or bypass anti-cheat.

## Owner setup

1. Import the Behavior Pack into a test Bedrock world.
2. Enable the pack and any Script API experiment required by the installed Bedrock release.
3. In the test world, tag the owner's player: `/tag @s add mana_owner`.
4. Run `/scriptevent mana:command {"action":"ping"}` to test the owner-only command handshake.
5. Observe visible `[MANA]` telemetry in world chat.

## Integration boundary

This is an official-API world-side adapter starter, not yet a phone-to-world transport. The Android side still needs a paired, authenticated local bridge/server transport and a user-approved command queue. Do not expose an unauthenticated port to the internet. The app must not claim to control Minecraft until that transport is implemented and tested against the user's exact Bedrock version and world permissions.

API version is pinned to 2.11.0 as documented in the official Microsoft module reference. Verify compatibility with the installed Bedrock release before enabling the pack.
