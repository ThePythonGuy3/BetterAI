package betterai;

import arc.Events;
import betterai.algorithm.*;
import betterai.log.BLog;
import mindustry.Vars;
import mindustry.game.*;
import mindustry.mod.*;

public class BetterAI extends Mod
{
    public static boolean jsonLibEnabled = false;
    public static boolean debug = false;

    public BetterAI()
    {

    }

    @Override
    public void init()
    {
        BLog.info("BetterAI has been loaded.");

        debug = Vars.player.name.equals("PyGuy");

        Events.on(EventType.ClientLoadEvent.class, event -> {
            Mods.LoadedMod jsonLib = Vars.mods.getMod("pyguy.jsonlib");
            if (jsonLib != null && jsonLib.enabled())
            {
                jsonLibEnabled = true;
                BLog.info("CustomJsonLib is enabled.");
            }

            ContentScore.GenerateScores();
        });

        Events.on(EventType.WorldLoadEvent.class, event -> {
            /*for (Team team : BlockScore.PresentTeams())
            {*/
            new MapScore(Team.sharded).EvaluateEntireMap();
            //}
        });
    }

    @Override
    public void loadContent()
    {

    }
}
