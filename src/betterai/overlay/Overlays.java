package betterai.overlay;

import arc.Events;
import mindustry.Vars;
import mindustry.game.EventType;

public class Overlays
{
    private static final BaseOverlay[] overlays = new BaseOverlay[]{new TargetOverlay(), new UnitRoleOverlay()};

    public static void LoadContent()
    {
        if (Vars.headless) return;

        for (BaseOverlay overlay : overlays)
        {
            overlay.load();
        }
    }

    public static void Initialize()
    {
        if (Vars.headless) return;

        for (BaseOverlay overlay : overlays)
        {
            overlay.init();
        }

        Events.run(EventType.Trigger.draw, () -> {
            if (!Vars.state.isGame()) return;

            for (BaseOverlay overlay : overlays)
            {
                overlay.draw();
            }
        });
    }
}
