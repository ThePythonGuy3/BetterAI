package betterai;

import arc.Events;
import betterai.algorithm.*;
import betterai.log.BLog;
import betterai.overlay.Overlays;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.mod.*;

public class BetterAI extends Mod
{
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
            ContentScore.Initialize();
            UnitRoles.Initialize();

            Overlays.Initialize();
        });
    }
}
