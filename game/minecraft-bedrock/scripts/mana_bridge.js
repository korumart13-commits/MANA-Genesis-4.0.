import { system, world } from "@minecraft/server";

const PREFIX = "[MANA]";
const OWNER_TAG = "mana_owner";
const COMMAND_ID = "mana:command";

function emit(kind, data) {
  const packet = {
    protocol: 1,
    kind,
    tick: system.currentTick,
    data
  };
  // Visible in-world telemetry; no hidden network transport or external data upload.
  world.sendMessage(`${PREFIX} ${JSON.stringify(packet)}`);
}

world.afterEvents.playerSpawn.subscribe((event) => {
  emit("player_spawn", {
    name: event.player.name,
    initialSpawn: event.initialSpawn,
    dimension: event.player.dimension.id
  });
});

world.afterEvents.playerLeave.subscribe((event) => {
  emit("player_leave", { playerName: event.playerName });
});

world.afterEvents.playerBreakBlock.subscribe((event) => {
  emit("block_broken", {
    player: event.player.name,
    block: event.block.typeId,
    location: event.block.location
  });
});

world.afterEvents.playerPlaceBlock.subscribe((event) => {
  emit("block_placed", {
    player: event.player.name,
    block: event.block.typeId,
    location: event.block.location
  });
});

world.afterEvents.playerDimensionChange.subscribe((event) => {
  emit("dimension_change", {
    player: event.player.name,
    dimension: event.toDimension.id
  });
});

system.afterEvents.scriptEventReceive.subscribe((event) => {
  if (event.id !== COMMAND_ID) return;

  const owner = event.sourceEntity;
  if (!owner || !owner.hasTag(OWNER_TAG)) {
    emit("command_denied", { reason: "source is not tagged as the world owner" });
    return;
  }

  let command;
  try {
    command = JSON.parse(event.message);
  } catch {
    owner.sendMessage("MANA: команда должна быть корректным JSON.");
    return;
  }

  // Safe handshake only. This starter pack intentionally does not move the
  // player, modify inventory, place blocks, or bypass server/anti-cheat rules.
  if (command.action === "ping") {
    owner.sendMessage("MANA bridge online.");
    emit("owner_ping", { player: owner.name });
  } else if (command.action === "status") {
    owner.sendMessage(`MANA: dimension=${owner.dimension.id}; tick=${system.currentTick}`);
  } else {
    owner.sendMessage("MANA: действие не поддерживается этим безопасным мостом.");
    emit("unsupported_command", { action: String(command.action ?? "unknown") });
  }
});

world.afterEvents.worldLoad.subscribe(() => {
  emit("bridge_ready", { api: "@minecraft/server", version: "2.11.0" });
});
