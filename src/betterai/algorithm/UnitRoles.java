package betterai.algorithm;

import arc.Core;
import arc.graphics.g2d.TextureRegion;
import arc.util.serialization.JsonValue;
import mindustry.Vars;
import mindustry.content.UnitTypes;
import mindustry.entities.bullet.BulletType;
import mindustry.type.*;
import pyguy.jsonlib.JsonLibWrapper;

public class UnitRoles
{
    private static final String[] roleIconNames = new String[]
            {
                    "defender",
                    "destroyer",
                    "squad-follower",
                    "squad-lead",
                    "frontline",
                    "frontline-heal"
            };
    private static TextureRegion[] roleIcons;
    private static UnitRole[] roles;

    private static float fortressDPS = 0f, polyDPS = 0f, atraxHealthSpeedFactor = 0f;

    public static void LoadContent()
    {
        roleIcons = new TextureRegion[roleIconNames.length];

        for (int i = 0; i < roleIconNames.length; i++)
        {
            roleIcons[i] = Core.atlas.find("betterai-" + roleIconNames[i]);
        }
    }

    public static void Initialize()
    {
        fortressDPS = GetDps(UnitTypes.fortress);
        polyDPS = GetDps(UnitTypes.poly);
        atraxHealthSpeedFactor = GetHealthSpeedFactor(UnitTypes.atrax);

        roles = new UnitRole[Vars.content.units().size];

        JsonValue vanillaRoles = JsonLibWrapper.GetRawField("betterai-vanilla-units-role", "data");

        for (UnitType unit : Vars.content.units())
        {
            UnitRole role;
            if (vanillaRoles != null && vanillaRoles.has(unit.name))
            {
                role = UnitRole.valueOf(vanillaRoles.getString(unit.name).replaceAll("-", ""));
            }
            else
            {
                String moddedRole = JsonLibWrapper.GetStringField(unit.name, "betterai-unit-role");

                if (moddedRole != null)
                {
                    role = UnitRole.valueOf(moddedRole.replaceAll("-", ""));
                }
                else
                {
                    // Fallback for modded content that does not support BetterAI
                    role = AutoAssign(unit);
                }
            }

            roles[unit.id] = role;
        }
    }

    private static float GetDps(UnitType unit)
    {
        float dps = 0;
        if (unit.weapons != null)
        {
            for (Weapon weapon : unit.weapons)
            {
                if (weapon.bullet != null)
                {
                    float bulletDamage = weapon.bullet.damage;

                    BulletType frag = weapon.bullet.fragBullet;
                    while (frag != null)
                    {
                        bulletDamage += frag.damage;
                        frag = frag.fragBullet;
                    }

                    dps += bulletDamage * weapon.shotsPerSec();
                }
            }
        }

        return dps;
    }

    private static float GetHealthSpeedFactor(UnitType unit)
    {
        return (float) (unit.health * Math.pow(unit.speed / 2f, 2));
    }

    private static UnitRole AutoAssign(UnitType unit)
    {
        UnitRole output;
        boolean heals = false;

        if (unit.weapons != null)
        {
            for (Weapon weapon : unit.weapons)
            {
                if (weapon.bullet != null)
                {
                    if (weapon.bullet.heals())
                    {
                        heals = true;
                        break;
                    }
                }
            }
        }

        float dps = GetDps(unit);
        float healthSpeedFactor = GetHealthSpeedFactor(unit);

        if (dps <= fortressDPS)
        {
            if (dps <= polyDPS)
            {
                output = UnitRole.defender;
            }
            else if (heals) output = UnitRole.frontlineheal;
            else
            {
                output = healthSpeedFactor >= atraxHealthSpeedFactor ? UnitRole.frontline : UnitRole.squadfollower;
            }
        }
        else
        {
            if (heals) output = UnitRole.frontlineheal;
            else
            {
                output = healthSpeedFactor >= atraxHealthSpeedFactor ? UnitRole.destroyer : UnitRole.squadlead;
            }
        }

        return output;
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
        squadlead,
        frontline,
        frontlineheal
    }
}
