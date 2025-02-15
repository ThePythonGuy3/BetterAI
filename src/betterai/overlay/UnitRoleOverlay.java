package betterai.overlay;

import arc.Core;
import arc.graphics.g2d.*;
import arc.struct.ObjectMap;
import mindustry.Vars;
import mindustry.gen.*;
import mindustry.graphics.Layer;
import mindustry.logic.GlobalVars;
import mindustry.type.UnitType;

public class UnitRoleOverlay extends BaseOverlay
{
    private final ObjectMap<UnitType, TextureRegion> unitIcons = new ObjectMap<>();

    @Override
    public void init()
    {
        for (UnitType type : Vars.content.units())
        {
            unitIcons.put(type, Core.atlas.find("betterai-" + new String[]{"defend", "destroy", "follower", "frontline", "leader"}[GlobalVars.rand.random(4)]));
        }
    }

    @Override
    public void draw()
    {
        Draw.draw(Layer.flyingUnit + 1, () -> {
            for (Unitc unit : Groups.unit)
            {
                if (unit != null)
                {
                    UnitType type = unit.type();
                    float size = Math.min(Math.min(16, type.hitSize), 64 / Vars.renderer.getDisplayScale());
                    Draw.rect(unitIcons.get(type), unit.x(), unit.y()/* + Math.min(unit.type().selectionSize, unit.type().hitSize)*/, size, size);
                }
            }

            Draw.flush();
        });
    }
}
