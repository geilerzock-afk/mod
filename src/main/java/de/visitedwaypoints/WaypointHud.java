package de.visitedwaypoints;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

public final class WaypointHud {
    private static final String[] ARROWS = {"↑", "↗", "→", "↘", "↓", "↙", "←", "↖"};

    private WaypointHud() {}

    public static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        WaypointStore store = VisitedWaypointsClient.STORE;
        if (!store.config().hud || mc.player == null || mc.level == null || mc.options.hideGui) return;
        if (!mc.level.dimension().equals(Level.OVERWORLD)) return;

        int left = store.remaining().size();
        int total = store.total();
        g.drawString(mc.font, "Waypoints: " + left + " übrig / " + total, 6, 6, 0xFFFFFFFF, true);

        WaypointStore.Waypoint nearest = null;
        double best = Double.MAX_VALUE;
        for (WaypointStore.Waypoint w : store.remaining()) {
            double dx = w.x() + 0.5 - mc.player.getX();
            double dz = w.z() + 0.5 - mc.player.getZ();
            double d = dx * dx + dz * dz;
            if (d < best) { best = d; nearest = w; }
        }

        if (nearest == null) {
            g.drawString(mc.font, "Alles besucht ✔", 6, 18, 0xFF55FF55, true);
            return;
        }

        double dx = nearest.x() + 0.5 - mc.player.getX();
        double dz = nearest.z() + 0.5 - mc.player.getZ();
        double targetYaw = Math.toDegrees(Math.atan2(-dx, dz));
        double rel = Mth.wrapDegrees(targetYaw - mc.player.getYRot());
        int sector = (int) Math.floorMod(Math.round(rel / 45.0), 8L);
        int dist = (int) Math.round(Math.sqrt(best));

        g.drawString(mc.font,
                "Nächster: " + nearest.name() + "  " + ARROWS[sector] + "  " + dist + " m",
                6, 18, 0xFFFFFF55, true);
    }
}
