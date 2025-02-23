package betterai;

import arc.Events;
import arc.input.KeyCode;
import betterai.algorithm.*;
import betterai.input.InputRegister;
import betterai.log.BLog;
import betterai.overlay.Overlays;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.mod.Mod;

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

        InputRegister.Initialize();

        if (!Vars.headless && debug) InputRegister.Register(KeyCode.y, () -> Vars.ui.hudfrag.shown = !Vars.ui.hudfrag.shown);

        Events.on(EventType.ClientLoadEvent.class, event -> {
            Overlays.LoadContent();
            UnitRoles.LoadContent();

            ContentScore.Initialize();
            UnitRoles.Initialize();

            Overlays.Initialize();
        });
    }
}
