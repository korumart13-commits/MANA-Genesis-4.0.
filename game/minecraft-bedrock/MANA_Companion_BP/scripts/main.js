import { world, system } from "@minecraft/server";

const companions = new Map();
const FOLLOW_DISTANCE = 2.8;
const FOLLOW_TICKS = 10;

function safeMessage(player, message) {
    try { player.sendMessage("§b[MANA]§r " + message); } catch (_) {}
}

function valid(entity) {
    try { return !!entity && entity.isValid; } catch (_) { return false; }
}

function spawnCompanion(player) {
    const existing = companions.get(player.id);
    if (existing && valid(existing.entity)) return existing;
    const location = player.location;
    const entity = player.dimension.spawnEntity("minecraft:allay", {
        x: location.x + 1.2, y: location.y + 0.6, z: location.z + 1.2
    });
    entity.nameTag = "§bMANA ✦";
    const state = { entity, following: true, ownerId: player.id };
    companions.set(player.id, state);
    return state;
}

function command(player, text) {
    const args = text.trim().toLowerCase().split(/\s+/);
    if (args[0] !== "!mana") return;
    const action = args[1] || "help";
    try {
        if (action === "help") {
            safeMessage(player, "Команды: !mana come, !mana follow, !mana stay, !mana hide, !mana status");
        } else if (action === "come") {
            spawnCompanion(player);
            safeMessage(player, "Я здесь. ✦");
        } else if (action === "follow") {
            spawnCompanion(player).following = true;
            safeMessage(player, "Иду рядом.");
        } else if (action === "stay") {
            const state = companions.get(player.id);
            if (!state || !valid(state.entity)) safeMessage(player, "Я пока не появилась. Напиши !mana come.");
            else { state.following = false; safeMessage(player, "Останусь здесь."); }
        } else if (action === "hide") {
            const state = companions.get(player.id);
            if (state && valid(state.entity)) state.entity.remove();
            companions.delete(player.id);
            safeMessage(player, "До скорой встречи.");
        } else if (action === "status") {
            const state = companions.get(player.id);
            safeMessage(player, state && valid(state.entity)
                ? (state.following ? "Я рядом и следую за тобой." : "Я здесь и жду.")
                : "Я ещё не появилась. Напиши !mana come.");
        } else {
            safeMessage(player, "Не знаю такую команду. Напиши !mana help.");
        }
    } catch (error) {
        safeMessage(player, "Не удалось выполнить команду в этом мире: " + String(error).slice(0, 120));
    }
}

world.afterEvents.playerSpawn.subscribe((event) => {
    if (!event.initialSpawn) return;
    system.runTimeout(() => safeMessage(event.player, "MANA Companion загружена. Напиши !mana help."), 40);
});

// Microsoft currently marks chatSend as pre-release; this small adapter is isolated.
world.afterEvents.chatSend.subscribe((event) => {
    if (event.message.trim().toLowerCase().startsWith("!mana")) command(event.sender, event.message);
});


const OWNER_TAG = "mana_owner";
system.afterEvents.scriptEventReceive.subscribe((event) => {
    if (event.id !== "mana:command") return;
    const owner = event.sourceEntity;
    if (!owner || !owner.hasTag(OWNER_TAG)) {
        try { owner?.sendMessage?.("MANA: команда доступна только владельцу мира."); } catch (_) {}
        return;
    }
    try {
        const request = JSON.parse(event.message);
        const action = String(request.action || "help");
        if (action === "ping") {
            owner.sendMessage?.("MANA bridge online.");
            return;
        }
        command(owner, "!mana " + action);
    } catch (_) {
        try { owner.sendMessage?.("MANA: команда должна быть JSON с полем action."); } catch (_) {}
    }
});

system.runInterval(() => {
    for (const [ownerId, state] of companions.entries()) {
        if (!state.following || !valid(state.entity)) {
            if (!valid(state.entity)) companions.delete(ownerId);
            continue;
        }
        const player = world.getAllPlayers().find((candidate) => candidate.id === ownerId);
        if (!player) continue;
        try {
            const target = player.location;
            const current = state.entity.location;
            if (Math.hypot(target.x - current.x, target.y - current.y, target.z - current.z) < FOLLOW_DISTANCE) continue;
            state.entity.tryTeleport(
                { x: target.x + 1.15, y: target.y + 0.35, z: target.z + 1.15 },
                { dimension: player.dimension, keepVelocity: false, checkForBlocks: true, facingLocation: target }
            );
        } catch (_) {
            // Dimension transitions and unloaded chunks are retried on the next tick.
        }
    }
}, FOLLOW_TICKS);
