package betterai.input;

import arc.*;
import arc.input.KeyCode;
import arc.struct.ObjectMap;
import mindustry.Vars;
import mindustry.game.EventType;

public class InputRegister
{
    private static final ObjectMap<KeyCode, Boolean> heldMap = new ObjectMap<>();
    private static final ObjectMap<KeyCode, Runnable> runnableMap = new ObjectMap<>();

    public static void Initialize()
    {
        if (Vars.headless) return;

        Events.run(EventType.Trigger.update, () -> {
            if (!Vars.state.isGame() || Vars.state.isPaused() || Core.scene.hasDialog() || Core.scene.hasKeyboard())
                return;

            for (KeyCode key : heldMap.keys())
            {
                if (key == null) continue;

                if (Core.input.keyDown(key))
                {
                    if (!heldMap.get(key)) runnableMap.get(key).run();

                    heldMap.put(key, true);
                }
                else
                {
                    heldMap.put(key, false);
                }
            }
        });
    }

    public static void Register(KeyCode key, Runnable runnable)
    {
        if (Vars.headless) return;

        heldMap.put(key, false);

        runnableMap.put(key, runnable);
    }

    public static void Unregister(KeyCode key)
    {
        if (Vars.headless) return;

        heldMap.remove(key);
        runnableMap.remove(key);
    }
}
