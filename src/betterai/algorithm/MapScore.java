package betterai.algorithm;

import arc.Events;
import arc.struct.*;
import betterai.log.BLog;
import mindustry.Vars;
import mindustry.game.*;
import mindustry.gen.Building;
import mindustry.world.Tile;
import mindustry.world.blocks.environment.*;

public class MapScore
{
    private static Seq<Team> teams;

    private static ObjectMap<Building, Float> buildingScores;

    private static int worldWidth, worldHeight;

    private static ObjectMap<Tile, Building> tileBuildings = new ObjectMap<>();

    public static void Initialize()
    {
        Events.on(EventType.WorldLoadEvent.class, event -> WorldLoaded());

        Events.on(EventType.TilePreChangeEvent.class, event -> TilePreChanged(event.tile));
        Events.on(EventType.TileChangeEvent.class, event -> TileChanged(event.tile));
    }

    private static void WorldLoaded()
    {
        teams = new Seq<>();
        buildingScores = new ObjectMap<>();

        for (Tile tile : Vars.world.tiles)
        {
            if (tile != null && tile.block() != null)
            {
                Team team = tile.team();

                if (!teams.contains(team)) teams.add(team);

                if (tile.build != null) AddBuilding(tile.build);
            }
        }

        worldWidth = Vars.world.width();
        worldHeight = Vars.world.height();
    }

    private static void TilePreChanged(Tile tile)
    {
        if (tile != null)
        {
            tileBuildings.put(tile, tile.build);
        }
    }

    private static void TileChanged(Tile tile)
    {
        if (tile != null && tileBuildings.containsKey(tile))
        {
            if (tile.build == null && tileBuildings.get(tile) != null)
            {
                RemoveBuilding(tileBuildings.get(tile));
            }
            else if (tile.build != null && tileBuildings.get(tile) == null)
            {
                AddBuilding(tile.build);
            }
            else if (tile.build != null)
            {
                RemoveBuilding(tileBuildings.get(tile));
                AddBuilding(tile.build);
            }

            tileBuildings.remove(tile);
        }
    }

    private static void AddBuilding(Building building)
    {
        if (!(building.block() instanceof Floor) && !(building.block() instanceof Prop) && !buildingScores.containsKey(building))
        {
            buildingScores.put(building, ContentScore.GetBlockScore(building.block()));
        }
    }

    private static void RemoveBuilding(Building building)
    {
        if (buildingScores.containsKey(building))
        {
            buildingScores.remove(building);
        }
    }

    public static ObjectMap.Keys<Building> GetBuildings()
    {
        return buildingScores.keys();
    }

    public static float GetBuildingScore(Building building)
    {
        return buildingScores.containsKey(building) ? buildingScores.get(building) : 0f;
    }

    public void EvaluateEntireMap()
    {
        ObjectMap<Building, Integer> blockScores = new ObjectMap<>();

        for (Tile tile : Vars.world.tiles)
        {
            if (tile != null && tile.build != null && !blockScores.containsKey(tile.build))
            {
                blockScores.put(tile.build, 0);
                BLog.info("Tile: " + tile.x + ", " + tile.y + " has a block " + tile.block().name + " with team: " + tile.build.team);
            }
        }
    }
}
