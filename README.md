# Besuchte Waypoints (Fabric 1.21.11, nur Client)

Liest `config/visitedwaypoints/overworld.json` (beim ersten Start aus dem Jar kopiert = deine Datei),
zeigt oben links die Anzahl offener Waypoints + den nächsten mit Pfeil und Entfernung,
und entfernt jeden Waypoint, sobald du dich ihm auf 5 Blöcke näherst (Höhe ±6).
Fortschritt: `config/visitedwaypoints/visited.json`.

Befehle (`/wegpunkte ...`): status, reset, reload, hud, radius <1-50>, export
`export` schreibt `overworld_remaining.json` im Originalformat nur mit den noch offenen Waypoints.

Bauen: `gradle wrapper` (einmalig) bzw. Wrapper aus einem Template von fabricmc.net/develop übernehmen, dann `./gradlew build`.
Jar: `build/libs/visitedwaypoints-1.0.0.jar`
