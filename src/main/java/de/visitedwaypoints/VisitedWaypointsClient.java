package de.visitedwaypoints;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public class VisitedWaypointsClient implements ClientModInitializer {
    public static final String MOD_ID = "visitedwaypoints";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final WaypointStore STORE = new WaypointStore();

    private int tickCounter;

    @Override
    public void onInitializeClient() {
        STORE.load();
        LOGGER.info("{} Waypoints geladen, {} noch offen", STORE.total(), STORE.remaining().size());

        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath(MOD_ID, "hud"),
                WaypointHud::render);
        ClientCommandRegistrationCallback.EVENT.register(WaypointCommands::register);
    }

    private void onTick(Minecraft mc) {
        if (mc.player == null || mc.level == null) return;
        if (++tickCounter % 4 != 0) return;
        if (!mc.level.dimension().equals(Level.OVERWORLD)) return;

        double px = mc.player.getX();
        double py = mc.player.getY();
        double pz = mc.player.getZ();
        double r = STORE.config().radius;
        double vt = STORE.config().verticalTolerance;

        for (WaypointStore.Waypoint w : new ArrayList<>(STORE.remaining())) {
            double dx = px - (w.x() + 0.5);
            double dz = pz - (w.z() + 0.5);
            boolean near = dx * dx + dz * dz <= r * r;
            if (near && w.yIncluded()) near = Math.abs(py - w.y()) <= vt;
            if (!near) continue;

            STORE.markVisited(w);
            int left = STORE.remaining().size();
            Component msg = left == 0
                    ? Component.literal("Waypoint " + w.name() + " abgehakt - alle Waypoints besucht!")
                    : Component.literal("Waypoint " + w.name() + " abgehakt - noch " + left + " übrig");
            mc.player.displayClientMessage(msg, true);
        }
    }
}
