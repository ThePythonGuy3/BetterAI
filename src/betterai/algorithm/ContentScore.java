package betterai.algorithm;

import arc.struct.*;
import arc.util.serialization.JsonValue;
import betterai.BetterAI;
import betterai.log.BLog;
import mindustry.Vars;
import mindustry.content.TechTree;
import mindustry.entities.bullet.BulletType;
import mindustry.type.*;
import mindustry.world.Block;
import mindustry.world.blocks.defense.turrets.ItemTurret;
import mindustry.world.blocks.environment.*;
import mindustry.world.blocks.power.ConsumeGenerator;
import mindustry.world.blocks.production.*;
import mindustry.world.consumers.*;
import pyguy.jsonlib.JsonLibWrapper;

public class ContentScore
{
    private static final float[] itemScores = new float[Vars.content.items().size];
    private static final float[] liquidScores = new float[Vars.content.liquids().size];
    private static final float[] blockScores = new float[Vars.content.blocks().size];

    private static final ObjectMap<Item, Seq<GenericCrafter>> itemProducers = new ObjectMap<>();
    private static final ObjectMap<Item, Seq<Item>> itemCraftingPaths = new ObjectMap<>();
    private static final boolean[] visitedItemRoot = new boolean[Vars.content.items().size];

    private static float threshold = 0f;

    public static float GetItemScore(Item item)
    {
        return itemScores[item.id];
    }

    public static float GetLiquidScore(Liquid liquid)
    {
        return liquidScores[liquid.id];
    }

    public static void Initialize()
    {
        GenerateItemLiquidScores();
        GenerateBlockScores();

        NormalizeScores(itemScores);
        NormalizeScores(liquidScores);
        NormalizeScores(blockScores);

        if (BetterAI.debug)
        {
            BLog.info("----------Item scores----------");
            for (int i = 0; i < itemScores.length; i++)
            {
                BLog.info(Vars.content.item(i).localizedName, "->", itemScores[i]);
            }

            BLog.info("----------Liquid scores----------");
            for (int i = 0; i < liquidScores.length; i++)
            {
                BLog.info(Vars.content.liquid(i).localizedName, "->", liquidScores[i]);
            }

            BLog.info("----------Block scores----------");
            for (int i = 0; i < liquidScores.length; i++)
            {
                BLog.info(Vars.content.liquid(i).localizedName, "->", liquidScores[i]);
            }
        }
    }

    private static void GenerateItemLiquidScores()
    {
        for (Item item : Vars.content.items())
        {
            itemProducers.put(item, new Seq<>());
        }

        Vars.content.blocks().each(block -> {
            if (!(block instanceof Prop))
            {
                if (!(block instanceof Floor))
                {
                    // Maximum value any item can take as score, increased by 3 for each placeable block in the game.
                    threshold += 3f;

                    // Add the block's item requirements to the item scores
                    for (ItemStack stack : block.requirements)
                    {
                        itemScores[stack.item.id] += Math.min(50, stack.amount);
                    }

                    // Add 1 to the liquid scores for each liquid filter the block has
                    if (block.hasLiquids)
                    {
                        for (int i = 0; i < liquidScores.length; i++)
                        {
                            if (block.liquidFilter[i])
                            {
                                liquidScores[i] += 1;
                            }
                        }
                    }
                }

                // Add the block's item and liquid consumers to the item and liquid scores
                if (block.hasConsumers)
                {
                    float multiplier = 1f;

                    if (block instanceof GenericCrafter genericCrafter)
                        multiplier += (genericCrafter.craftTime / 60f) * 0.5f;

                    if (block instanceof WallCrafter wallCrafter)
                        multiplier += (wallCrafter.drillTime / 60f) * 0.1f;

                    for (Consume consume : block.consumers)
                    {
                        if (consume instanceof ConsumeItems consumeItem)
                        {
                            for (ItemStack stack : consumeItem.items)
                            {
                                itemScores[stack.item.id] += Math.min(100, (1f / stack.amount) * (consumeItem.booster ? 0.25f : 1f) * multiplier);
                            }
                        }
                        else if (consume instanceof ConsumeLiquids consumeLiquids)
                        {
                            for (LiquidStack stack : consumeLiquids.liquids)
                            {
                                liquidScores[stack.liquid.id] += Math.min(100, (1f / stack.amount) * (consumeLiquids.booster ? 0.25f : 1f) * multiplier);
                            }
                        }
                        else if (consume instanceof ConsumeItemFilter consumeItemFilter)
                        {
                            if (block instanceof ConsumeGenerator consumeGenerator)
                            {
                                for (Item item : Vars.content.items())
                                {
                                    if (consumeItemFilter.filter.get(item))
                                    {
                                        itemScores[item.id] += 1f / (consumeGenerator.itemDuration / 60f) * 3f;
                                    }
                                }
                            }
                        }
                    }
                }

                // Calculate the block scores based on the block's ammo if the block is a turret
                if (block instanceof ItemTurret itemTurret)
                {
                    for (Item item : itemTurret.ammoTypes.keys())
                    {
                        BulletType type = itemTurret.ammoTypes.get(item);
                        float itemsPerShot = (1f / type.ammoMultiplier);
                        if (!type.heals()) itemScores[item.id] += itemsPerShot * type.damage / 2f;
                        while (type.fragBullet != null)
                        {
                            itemScores[item.id] += itemsPerShot * (type.fragBullets * type.fragBullet.damage) / 2f;
                            type = type.fragBullet;
                        }
                    }
                }

                // Add scores for ore blocks, sand, water and such
                if (block.itemDrop != null)
                {
                    itemScores[block.itemDrop.id] += block.itemDrop.hardness * block.itemDrop.cost;
                }

                if (block instanceof Floor floor)
                {
                    if (floor.liquidDrop != null)
                    {
                        liquidScores[floor.liquidDrop.id] += floor.liquidDrop.coolant ? 2 : 1;
                    }
                }

                // Add the block to the item producer table of all the items it outputs
                if (block instanceof GenericCrafter genericCrafter && genericCrafter.hasConsumers && genericCrafter.outputsItems())
                {
                    for (ItemStack stack : genericCrafter.outputItems)
                    {
                        itemProducers.get(stack.item).add(genericCrafter);
                    }
                }
            }
        });

        // Add research cost to scores
        TechTree.all.each(tech -> {
            for (ItemStack stack : tech.requirements)
            {
                itemScores[stack.item.id] += Math.min(50, stack.amount * 0.25f);
            }
        });

        // Add scores for items required to launch to a sector
        Vars.content.planets().each(planet -> {
            planet.sectors.each(sector -> {
                if (sector.preset != null)
                {
                    sector.preset.generator.map.rules().loadout.each(stack -> {
                        itemScores[stack.item.id] += Math.min(50, stack.amount);
                    });
                }
            });
        });

        // Multiply the scores given the amount of crafters required to create that item
        for (int i = 0; i < itemScores.length; i++)
        {
            itemScores[i] = itemScores[i] * (1f + (GetCraftingDepth(Vars.content.item(i)).size - 1f) / 2f) * 2f;
        }

        // Cap the scores at the threshold
        for (int i = 0; i < itemScores.length; i++)
        {
            itemScores[i] = Math.min(threshold, itemScores[i]);
        }

        for (int i = 0; i < liquidScores.length; i++)
        {
            liquidScores[i] = Math.min(threshold, liquidScores[i]);
        }

        // Add scores for items depending on the items required to craft them
        float[] newItemScores = new float[itemScores.length];
        for (int i = 0; i < itemScores.length; i++)
        {
            newItemScores[i] = itemScores[i];
            Seq<Item> itemCraftingPath = itemCraftingPaths.get(Vars.content.item(i));

            if (BetterAI.debug) BLog.info(Vars.content.item(i).localizedName + " crafting path: " + itemCraftingPath);
            for (int j = 1; j < itemCraftingPath.size; j++)
            {
                newItemScores[i] += itemScores[itemCraftingPath.get(j).id] * 0.5f;
            }
        }

        System.arraycopy(newItemScores, 0, itemScores, 0, itemScores.length);
    }

    // Get the crafting depth of an item (aka how many crafters are required in a chain to craft it)
    private static Seq<Item> GetCraftingDepth(Item item)
    {
        if (itemCraftingPaths.containsKey(item))
        {
            return itemCraftingPaths.get(item);
        }
        else
        {
            Seq<Item> path = new Seq<>();

            if (visitedItemRoot[item.id]) return path;
            visitedItemRoot[item.id] = true;

            Seq<Item> minPath = new Seq<>();
            int subPathMinDepth = -1;
            if (!itemProducers.get(item).isEmpty())
            {
                for (GenericCrafter genericCrafter : itemProducers.get(item))
                {
                    Seq<Item> maxPath = new Seq<>();
                    int subPathMaxDepth = 0;
                    for (Consume consume : genericCrafter.consumers)
                    {
                        if (consume instanceof ConsumeItems consumeItem)
                        {
                            for (ItemStack stack : consumeItem.items)
                            {
                                Seq<Item> subPath = GetCraftingDepth(stack.item);

                                if (subPath.size > subPathMaxDepth)
                                {
                                    subPathMaxDepth = subPath.size;
                                    maxPath = subPath;
                                }
                            }
                        }
                    }

                    if (subPathMinDepth == -1 || maxPath.size < subPathMinDepth)
                    {
                        subPathMinDepth = maxPath.size;
                        minPath = maxPath;
                    }
                }
            }

            path.add(item);
            path.addAll(minPath);

            itemCraftingPaths.put(item, path);

            return path;
        }
    }

    private static void GenerateBlockScores()
    {
        JsonValue scoringTypes = JsonLibWrapper.GetRawField("betterai-vanilla-blocks-score-type", "data");

        for (Block block : Vars.content.blocks())
        {
            BlockScoreType scoreType = BlockScoreType.base;
            if (scoringTypes != null && scoringTypes.has(block.name))
            {
                scoreType = BlockScoreType.valueOf(scoringTypes.getString(block.name));
            }
        }
    }

    private static void SetBlockScore(Block block, float score)
    {
        blockScores[block.id] = score;
    }

    // Normalize the scores to a 0-100 scale
    private static void NormalizeScores(float[] scores)
    {
        float maxScore = 0;

        for (float score : scores)
        {
            if (score > maxScore)
            {
                maxScore = score;
            }
        }

        for (int i = 0; i < scores.length; i++)
        {
            scores[i] = (int) ((scores[i] / maxScore) * 100 * 1000) / 1000f;
        }
    }

    private enum BlockScoreType
    {
        base,
        two
    }
}
