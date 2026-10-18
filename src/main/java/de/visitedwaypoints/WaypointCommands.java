package de.visitedwaypoints;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.network.chat.Component;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public final class WaypointCommands {
    private WaypointCommands() {}

    public static void register(CommandDispatcher<FabricClientCommandSource> d, CommandBuildContext ctx) {
        WaypointStore store = VisitedWaypointsClient.STORE;

        d.register(literal("wegpunkte")
                .executes(c -> status(c.getSource()))
                .then(literal("status").executes(c -> status(c.getSource())))
                .then(literal("reset").executes(c -> {
                    store.reset();
                    c.getSource().sendFeedback(Component.literal("Alle Waypoints wieder aktiv (" + store.total() + ")."));
                    return 1;
                }))
                .then(literal("reload").executes(c -> {
                    store.load();
                    c.getSource().sendFeedback(Component.literal("Neu geladen: " + store.remaining().size() + " von " + store.total() + " offen."));
                    return 1;
                }))
                .then(literal("hud").executes(c -> {
                    store.config().hud = !store.config().hud;
                    store.saveConfig();
                    c.getSource().sendFeedback(Component.literal("HUD: " + (store.config().hud ? "an" : "aus")));
                    return 1;
                }))
                .then(literal("radius").then(argument("blöcke", IntegerArgumentType.integer(1, 50)).executes(c -> {
                    store.config().radius = IntegerArgumentType.getInteger(c, "blöcke");
                    store.saveConfig();
                    c.getSource().sendFeedback(Component.literal("Radius: " + store.config().radius + " Blöcke"));
                    return 1;
                })))
                .then(literal("export").executes(c -> {
                    try {
                        var p = store.exportRemaining();
                        c.getSource().sendFeedback(Component.literal("Gespeichert: " + p));
                    } catch (Exception e) {
                        c.getSource().sendError(Component.literal("Export fehlgeschlagen: " + e.getMessage()));
                    }
                    return 1;
                })));
    }

    private static int status(FabricClientCommandSource src) {
        WaypointStore store = VisitedWaypointsClient.STORE;
        src.sendFeedback(Component.literal("Waypoints: " + store.remaining().size() + " übrig, "
                + (store.total() - store.remaining().size()) + " besucht, " + store.total() + " gesamt. Radius "
                + store.config().radius + " Blöcke."));
        return 1;
    }
}
