package betterai.algorithm;

import arc.struct.*;
import betterai.log.BLog;
import mindustry.Vars;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.world.Tile;

public class MapScore
{
    private final Team team;

    public MapScore(Team team)
    {
        this.team = team;
    }

    public static Team[] PresentTeams()
    {
        Seq<Team> teams = new Seq<>();

        for (Tile tile : Vars.world.tiles)
        {
            if (tile != null && tile.build != null && !teams.contains(tile.team()))
            {
                teams.add(tile.team());
            }
        }

        return teams.toArray();
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
