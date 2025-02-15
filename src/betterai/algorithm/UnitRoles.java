package betterai.algorithm;

import arc.Core;
import arc.graphics.g2d.TextureRegion;
import arc.util.serialization.JsonValue;
import mindustry.Vars;
import mindustry.type.UnitType;
import pyguy.jsonlib.JsonLibWrapper;

public class UnitRoles
{
    private static final String[] roleIconNames = new String[]
            {
                    "defender",
                    "destroyer",
                    "squad-follower",
                    "squad-leader",
                    "frontline",
                    "frontline-heal"
            };
    private static UnitRole[] roles;
    private static TextureRegion[] roleIcons;

    public static void Initialize()
    {
        for (UnitRole role : UnitRole.values())
        {
            roleIcons[role.ordinal()] = Core.atlas.find("betterai-" + roleIconNames[role.ordinal()]);
        }

        roles = new UnitRole[Vars.content.units().size];

        JsonValue vanillaRoles = JsonLibWrapper.GetRawField("betterai-vanilla-units-role", "data");

        for (UnitType unit : Vars.content.units())
        {
            UnitRole role = UnitRole.defender;
            if (vanillaRoles != null && vanillaRoles.has(unit.name))
            {
                role = UnitRole.valueOf(vanillaRoles.getString(unit.name).replaceAll("-", ""));
            }

            roles[unit.id] = role;
        }
    }

    public static UnitRole GetRole(UnitType unit)
    {
        return roles[unit.id];
    }

    public static TextureRegion GetIcon(UnitType type)
    {
        return roleIcons[GetRole(type).ordinal()];
    }

    public enum UnitRole
    {
        defender,
        destroyer,
        squadfollower,
        squadleader,
        frontline,
        frontlineHealer
    }
}
