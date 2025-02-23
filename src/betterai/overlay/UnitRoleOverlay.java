package betterai.overlay;

import arc.graphics.g2d.Draw;
import betterai.algorithm.UnitRoles;
import mindustry.Vars;
import mindustry.gen.*;
import mindustry.graphics.Layer;
import mindustry.type.UnitType;

public class UnitRoleOverlay extends BaseOverlay
{
    @Override
    public void load()
    {

    }

    @Override
    public void init()
    {

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
                    float size = Math.min(Math.min(16, type.hitSize * 0.8f), 64 / Vars.renderer.getDisplayScale());
                    Draw.rect(UnitRoles.GetIcon(type), unit.x(), unit.y()/* + Math.min(unit.type().selectionSize, unit.type().hitSize)*/, size, size);
                }
            }
        });
    }
}
