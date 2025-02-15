package betterai;

import arc.*;
import arc.graphics.Texture;
import betterai.algorithm.*;
import betterai.log.BLog;
import betterai.overlay.Overlays;
import mindustry.Vars;
import mindustry.game.EventType;
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

            ContentScore.Initialize();
            UnitRoles.Initialize();
            Overlays.Initialize();

            Core.settings.put("linear", false);
            for (Texture tex : Core.atlas.getTextures())
            {
                Texture.TextureFilter filter = Texture.TextureFilter.nearest;
                tex.setFilter(filter, filter);
            }
        });
    }

    @Override
    public void loadContent()
    {

    }
}
