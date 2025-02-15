package betterai.algorithm;

import arc.struct.*;
import betterai.log.BLog;
import mindustry.Vars;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.world.Tile;
import mindustry.world.blocks.storage.CoreBlock;

public class MapScore
{
    private static Seq<Team> teams;
    private static byte[] scores;

    private static int worldWidth, worldHeight;

    public static void Initialize()
    {
        teams = new Seq<>();

        for (Tile tile : Vars.world.tiles)
        {
            if (tile != null && tile.block() != null && tile.block() instanceof CoreBlock)
            {
                Team team = tile.team();

                if (!teams.contains(team))
                {
                    teams.add(team);
                }
            }
        }

        worldWidth = Vars.world.width();
        worldHeight = Vars.world.height();
        scores = new byte[worldWidth * worldHeight * teams.size];
    }

    private static int indexOf(Team team, int x, int y)
    {
        return x + y * worldWidth + teams.indexOf(team) * worldWidth * worldHeight;
    }

    private static byte GetScoresIn(Team team, int x, int y)
    {
        if (!teams.contains(team))
        {
            return (byte) 0b1111_1111; // Maximum score for a team that doesn't exist
        }

        return scores[indexOf(team, x, y)];
    }

    public static int GetTargetScore(Team team, int x, int y)
    {
        return GetScoresIn(team, x, y) & 0b0000_1111;
    }

    public static int GetDangerScore(Team team, int x, int y)
    {
        return GetScoresIn(team, x, y) >> 4;
    }

    private static void SetTargetScore(Team team, int x, int y, int score)
    {
        if (!teams.contains(team))
        {
            return;
        }

        int index = indexOf(team, x, y);
        scores[index] = (byte) ((scores[index] & 0b1111_0000) | (score & 0b0000_1111));
    }

    private static void SetDangerScore(Team team, int x, int y, int score)
    {
        if (!teams.contains(team))
        {
            return;
        }

        int index = indexOf(team, x, y);
        scores[index] = (byte) ((scores[index] & 0b0000_1111) | ((score << 4) & 0b1111_0000));
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
