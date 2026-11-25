# MANA Genesis — Minecraft Bedrock companion

This is an installable Minecraft Bedrock Behavior Pack source. It uses the official @minecraft/server Script API 2.11.0. The companion entity is a local world-side Allay with the MANA name tag; it is not a remote phone bot.

## Supported in-world commands

- !mana help — list commands.
- !mana come — spawn the MANA companion near the player.
- !mana follow — make the companion follow the player.
- !mana stay — stop following and exploring.
- !mana explore — wander within a small radius of the owner using safe teleport checks.
- !mana status — report current state.
- !mana hide — remove the companion.

The companion follows its owner or performs bounded scripted wandering within roughly five blocks using the supported entity teleport API and collision checks. This is a simple scripted exploration behavior, not model-driven world understanding. It does not mine, modify inventory, automate combat, build structures, read private device data, or bypass anti-cheat.

## Install

1. Download the MANA_Companion_BP.mcpack artifact from the GitHub Actions run named MANA Minecraft Companion Pack.
2. Open the .mcpack file on the Android phone and import it into Minecraft Bedrock.
3. Enable the Behavior Pack in a test world.
4. If the installed Bedrock release requires it, enable the Script API / Beta APIs experiment. The chatSend event used for !mana commands is pre-release in some API versions.
5. Enter the world and type !mana come.

## Owner-only Script Event interface

The pack also accepts /scriptevent mana:command {"action":"ping"} from an entity tagged mana_owner. This is a restricted owner-tag handshake for a future paired bridge. It is not a phone-to-world network transport.

## Integration boundary

The Android app does not yet connect to this Behavior Pack. A future phone bridge must be paired, authenticated, limited to a private/local network, and require explicit approval for game actions. Do not expose an unauthenticated server port to the internet. The current companion is a working world-side prototype, not a fully autonomous Minecraft agent.

## CI

- Source validation and packaging: .github/workflows/mana-minecraft-pack.yml
- The pack is syntax-checked and uploaded as a .mcpack artifact; this does not replace testing inside the exact Bedrock version installed on the owner's phone.