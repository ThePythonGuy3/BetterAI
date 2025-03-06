package betterai;

import arc.Events;
import arc.input.KeyCode;
import betterai.algorithm.*;
import betterai.input.InputRegister;
import betterai.log.BLog;
import betterai.overlay.Overlays;
import mindustry.Vars;
import mindustry.content.Items;
import mindustry.game.EventType;
import mindustry.mod.Mod;
import mindustry.type.*;
import mindustry.world.blocks.defense.DirectionalForceProjector;
import mindustry.world.meta.BuildVisibility;

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

        if (!Vars.headless && debug)
            InputRegister.Register(KeyCode.y, () -> Vars.ui.hudfrag.shown = !Vars.ui.hudfrag.shown);

        Events.on(EventType.ClientLoadEvent.class, event -> {
            Overlays.LoadContent();
            UnitRoles.LoadContent();

            ContentScore.Initialize();
            UnitRoles.Initialize();

            MapScore.Initialize();

            Overlays.Initialize();
        });
    }

    @Override
    public void loadContent()
    {
        // Testing scores for content types not present in vanilla
        new DirectionalForceProjector("barrier-projector")
        {{
            requirements(Category.effect, ItemStack.with(Items.surgeAlloy, 100, Items.silicon, 125));
            size = 3;
            width = 50f;
            length = 36;
            shieldHealth = 2000f;
            cooldownNormal = 3f;
            cooldownBrokenBase = 0.35f;

            consumePower(4f);

            buildVisibility = BuildVisibility.hidden;
        }};
    }
}
