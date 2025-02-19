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
        Events.run(EventType.Trigger.update, () -> {
            if (!Vars.state.isGame()) return;

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
        heldMap.put(key, false);

        runnableMap.put(key, runnable);
    }

    public static void Unregister(KeyCode key)
    {
        heldMap.remove(key);
        runnableMap.remove(key);
    }
}
