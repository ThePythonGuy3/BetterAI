package betterai.algorithm;

import arc.math.Mathf;
import arc.struct.*;
import arc.util.serialization.JsonValue;
import betterai.BetterAI;
import betterai.log.BLog;
import betterai.misc.Pair;
import mindustry.Vars;
import mindustry.content.*;
import mindustry.entities.bullet.BulletType;
import mindustry.type.*;
import mindustry.world.Block;
import mindustry.world.blocks.campaign.*;
import mindustry.world.blocks.defense.*;
import mindustry.world.blocks.defense.turrets.ItemTurret;
import mindustry.world.blocks.distribution.*;
import mindustry.world.blocks.environment.*;
import mindustry.world.blocks.heat.*;
import mindustry.world.blocks.liquid.*;
import mindustry.world.blocks.logic.*;
import mindustry.world.blocks.payloads.*;
import mindustry.world.blocks.power.*;
import mindustry.world.blocks.production.*;
import mindustry.world.blocks.sandbox.*;
import mindustry.world.blocks.storage.*;
import mindustry.world.blocks.units.*;
import mindustry.world.consumers.*;
import pyguy.jsonlib.JsonLibWrapper;

import java.util.ArrayList;

public class ContentScore
{
    private static final ObjectMap<Item, Seq<GenericCrafter>> itemProducers = new ObjectMap<>();
    private static final ObjectMap<Item, Seq<Item>> itemCraftingPaths = new ObjectMap<>();
    private static final float duoGraphiteBulletDamage = ((ItemTurret) Blocks.duo).ammoTypes.get(Items.graphite).damage;
    private static final float thoriumReactionProduction = ((PowerGenerator) Blocks.thoriumReactor).powerProduction;
    private static float[] itemScores;
    private static float[] liquidScores;
    private static float[] blockScores;
    private static boolean[] visitedItemRoot;
    private static Item[][] itemsByHardness;
    private static float itemLiquidScoreCapValue = 0f; // Liquids basically never reach this value, but just in case
    private static BlockScoreType[] blockScoreTypes;
    private static BlockDynamicScoreType[] blockDynamicScoreTypes;
    private static float batteryCapacity = 0f;

    public static float GetItemScore(Item item)
    {
        return itemScores[item.id];
    }

    public static float GetLiquidScore(Liquid liquid)
    {
        return liquidScores[liquid.id];
    }

    public static float GetBlockScore(Block block)
    {
        return blockScores[block.id];
    }

    public static BlockDynamicScoreType GetBlockDynamicScoreType(Block block)
    {
        return blockDynamicScoreTypes[block.id];
    }

    public static BlockScoreType GetBlockScoreType(Block block)
    {
        return blockScoreTypes[block.id];
    }

    public static void Initialize()
    {
        itemScores = new float[Vars.content.items().size];
        liquidScores = new float[Vars.content.liquids().size];
        blockScores = new float[Vars.content.blocks().size];

        visitedItemRoot = new boolean[Vars.content.items().size];
        blockScoreTypes = new BlockScoreType[Vars.content.blocks().size];
        blockDynamicScoreTypes = new BlockDynamicScoreType[Vars.content.blocks().size];

        for (Consume consume : Blocks.battery.consumers)
        {
            if (consume instanceof ConsumePower consumePower)
            {
                batteryCapacity += consumePower.capacity;
            }
        }

        GetHardnessValues();

        GenerateItemLiquidScores();
        GenerateBlockScores();

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

    private static void GetHardnessValues()
    {
        ArrayList<ArrayList<Item>> itemsByHardnessList = new ArrayList<>();

        for (Item item : Vars.content.items())
        {
            while (item.hardness > itemsByHardnessList.size())
            {
                itemsByHardnessList.add(new ArrayList<>());
            }

            itemsByHardnessList.get(Math.max(0, item.hardness - 1)).add(item);
        }

        itemsByHardness = new Item[itemsByHardnessList.size()][];

        for (int i = 0; i < itemsByHardnessList.size(); i++)
        {
            itemsByHardness[i] = itemsByHardnessList.get(i).toArray(new Item[0]);
        }

        BLog.info("Items by hardness: " + itemsByHardnessList);
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
                if (block instanceof Floor floor)
                {
                    if (floor.liquidDrop != null)
                    {
                        liquidScores[floor.liquidDrop.id] += floor.liquidDrop.coolant ? 2 : 1;
                    }
                }
                else
                {
                    // Maximum value any item can take as score, increased by 3 for each placeable block in the game.
                    itemLiquidScoreCapValue += 3f;

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
                                    liquidScores[stack.liquid.id] += Math.min(100, (1f / stack.amount) * (consumeLiquids.booster ? 0.25f : 1f) * (block instanceof NuclearReactor ? 3f : 1f) * multiplier);
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

                    // Add the block to the item producer table of all the items it outputs
                    if (block instanceof GenericCrafter genericCrafter && genericCrafter.hasConsumers && genericCrafter.outputsItems())
                    {
                        for (ItemStack stack : genericCrafter.outputItems)
                        {
                            itemProducers.get(stack.item).add(genericCrafter);
                        }
                    }
                }

                // Add scores for ore blocks, sand, water and such
                if (block.itemDrop != null)
                {
                    itemScores[block.itemDrop.id] += block.itemDrop.hardness * block.itemDrop.cost;
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
            itemScores[i] = Math.min(itemLiquidScoreCapValue, itemScores[i]);
        }

        for (int i = 0; i < liquidScores.length; i++)
        {
            liquidScores[i] = Math.min(itemLiquidScoreCapValue, liquidScores[i]);
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

        NormalizeScores(itemScores);
        NormalizeScores(liquidScores);
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
            BlockScoreType scoreType = BlockScoreType.automatic;
            if (scoringTypes != null && scoringTypes.has(block.name))
            {
                scoreType = BlockScoreType.valueOf(scoringTypes.getString(block.name));
            }
            else
            {
                String moddedScoreType = JsonLibWrapper.GetStringField(block.name, "betterai-block-score-type");

                if (moddedScoreType != null)
                {
                    scoreType = BlockScoreType.valueOf(moddedScoreType);
                }
            }

            blockScoreTypes[block.id] = scoreType;

            Pair<BlockDynamicScoreType, Float> blockInfo = GetBlockInfo(block, scoreType);

            blockScores[block.id] = blockInfo.second;
            blockDynamicScoreTypes[block.id] = blockInfo.first;
        }

        NormalizeScores(blockScores);
    }

    private static float GetPowerBlockScore(Block block)
    {
        float capacity = 0;
        for (Consume consume : block.consumers)
        {
            if (consume instanceof ConsumePower consumePower)
            {
                if (consumePower.buffered)
                {
                    capacity += consumePower.capacity;
                }
            }
        }

        return (float) Math.pow(capacity / batteryCapacity, 1 / 5f) * 20f;
    }

    private static float GetStorageBlockScore(Block block)
    {
        float baseScore = 0f;
        if (block.hasItems) baseScore += ((float) block.itemCapacity / Blocks.vault.itemCapacity) * 5f;
        if (block.hasLiquids) baseScore += (block.liquidCapacity / Blocks.liquidContainer.liquidCapacity) * 2f;

        return baseScore;
    }

    private static float FalloffFunction(float x)
    {
        return 100f * (1f - (float) Math.exp(-(2f * x) / 100f));
    }

    private static float GetBulletTypeTotalDamage(BulletType bulletType)
    {
        float baseDamage = bulletType.damage + bulletType.splashDamage;

        if (bulletType.fragBullet != null)
        {
            baseDamage += GetBulletTypeTotalDamage(bulletType.fragBullet) * bulletType.fragBullets;
        }

        if (bulletType.intervalBullet != null)
        {
            float spawnLifetime = bulletType.lifetime - (bulletType.intervalDelay == -1 ? 0 : bulletType.intervalDelay);
            float totalBullets = bulletType.intervalBullets * (spawnLifetime / bulletType.bulletInterval);
            baseDamage += GetBulletTypeTotalDamage(bulletType.intervalBullet) * totalBullets;
        }

        return baseDamage;
    }

    private static Pair<BlockDynamicScoreType, Float> GetBlockInfo(Block block, BlockScoreType scoreType)
    {
        BlockDynamicScoreType dynamicScoreType = BlockDynamicScoreType.constant;

        float score = switch (scoreType)
        {
            case wall ->
                    FalloffFunction((float) Math.pow(block.health <= 0 ? block.scaledHealth * block.size * block.size : block.health, 1 / 3f) * 1.5f);
            case powerdistributor ->
            {
                if (block.hasPower)
                {
                    dynamicScoreType = BlockDynamicScoreType.powerGrid;

                    yield FalloffFunction(GetPowerBlockScore(block));
                }
                else
                {
                    yield 0;
                }
            }
            case storage ->
            {
                dynamicScoreType = BlockDynamicScoreType.single;

                yield FalloffFunction(GetStorageBlockScore(block));
            }
            case logic -> 2f; // Who uses logic blocks anyway
            case core ->
                    10f; // Low priority, since the only units going for the core intentionally are frontline and destroyer units
            case defense -> 60f;
            case ignore, manual -> 0f;
            case automatic ->
            {
                float baseScore = 1f;

                // NOTE Campaign
                if (block instanceof Accelerator || block instanceof LaunchPad)
                {
                    baseScore = 20f;
                }
                // NOTE Defense
                else if (block instanceof Wall wall)
                {
                    baseScore = (float) Math.pow(block.health <= 0 ? block.scaledHealth * block.size * block.size : block.health, 1 / 3f) * 1.5f * Math.max(0.1f, (1f - Math.max(0f, wall.lightningChance) * 2f) * (1f - Math.max(0f, wall.chanceDeflect) / duoGraphiteBulletDamage));

                    if (block instanceof AutoDoor || block instanceof Door)
                    {
                        baseScore += 5f;
                    }
                    else if (block instanceof ShieldWall)
                    {
                        baseScore -= 5f;
                    }
                }
                else if (block instanceof BaseShield)
                {
                    baseScore = 100f; // This should give priority to beam links powering these shields
                }
                else if (block instanceof BuildTurret buildTurret)
                {
                    baseScore = buildTurret.buildSpeed * 10f + buildTurret.range / 20f;
                }
                else if (block instanceof DirectionalForceProjector directionalForceProjector)
                {
                    baseScore = directionalForceProjector.shieldHealth / 80f + directionalForceProjector.width / 3f;
                }
                else if (block instanceof ForceProjector forceProjector)
                {
                    baseScore = forceProjector.radius / 10f + (float) Math.sqrt(forceProjector.shieldHealth);
                }
                else if (block instanceof MendProjector mendProjector)
                {
                    baseScore = 20f + (int)(mendProjector.healPercent / (mendProjector.reload / 60f));
                }
                else if (block instanceof OverdriveProjector overdriveProjector)
                {
                    baseScore = (overdriveProjector.range / 10f) * overdriveProjector.speedBoost;
                }
                else if (block instanceof Radar)
                {
                    baseScore = 5f;
                }
                else if (block instanceof RegenProjector regenProjector)
                {
                    baseScore = 20f + (int)(regenProjector.healPercent * 60f);
                }
                else if (block instanceof ShockMine shockMine)
                {
                    float mineDamage = shockMine.tendrils * shockMine.damage + (shockMine.bullet == null ? 0 : GetBulletTypeTotalDamage(shockMine.bullet) * shockMine.shots) + shockMine.damage;
                    baseScore = (float) Math.sqrt(mineDamage) * 1.2f;
                }
                else if (block instanceof ShockwaveTower shockwaveTower)
                {
                    baseScore = shockwaveTower.range / 10f + (shockwaveTower.bulletDamage / duoGraphiteBulletDamage) * (60f / shockwaveTower.reload);
                }
                // NOTE Distribution
                else if (block instanceof Conveyor conveyor)
                {
                    baseScore = conveyor.displayedSpeed;
                    dynamicScoreType = BlockDynamicScoreType.itemGraph;
                }
                else if (block instanceof Duct duct)
                {
                    baseScore = 60f / duct.speed;
                    dynamicScoreType = BlockDynamicScoreType.itemGraph;
                }
                else if (block instanceof StackConveyor stackConveyor)
                {
                    baseScore = (float) Math.sqrt(stackConveyor.itemCapacity * stackConveyor.speed * 60) * 0.5f;
                    dynamicScoreType = BlockDynamicScoreType.itemGraph;
                }
                else if (block instanceof ItemBridge || block instanceof DirectionBridge)
                {
                    baseScore = 5f;

                    dynamicScoreType = (block instanceof LiquidBridge) ? BlockDynamicScoreType.liquidGraph : BlockDynamicScoreType.itemGraph;
                }
                else if (block instanceof Router router)
                {
                    baseScore = router.speed;

                    dynamicScoreType = BlockDynamicScoreType.itemGraph;
                }
                else if (block instanceof DuctRouter ductRouter)
                {
                    baseScore = ductRouter.speed;

                    dynamicScoreType = BlockDynamicScoreType.itemGraph;
                }
                else if (block instanceof Junction || block instanceof OverflowGate || block instanceof Sorter || block instanceof OverflowDuct)
                {
                    baseScore = 5f;

                    dynamicScoreType = BlockDynamicScoreType.itemGraph;
                }
                // NOTE Heat
                else if (block instanceof HeatProducer || block instanceof HeatConductor)
                {
                    baseScore = 10f;
                }
                // NOTE Liquid
                else if (block instanceof LiquidBlock liquidBlock)
                {
                    baseScore = (float) Math.pow(liquidBlock.liquidCapacity, 1 / 3f) * 0.75f;

                    if (block instanceof Pump pump)
                    {
                        baseScore += 10f * pump.pumpAmount * pump.size * pump.size;

                        if (block instanceof SolidPump solidPump)
                        {
                            baseScore = baseScore * 0.7f + liquidScores[solidPump.result.id] * 0.3f;
                        }
                    }

                    dynamicScoreType = BlockDynamicScoreType.liquidGraph;
                }
                // NOTE Logic
                else if (block instanceof LogicBlock || block instanceof CanvasBlock || block instanceof LogicDisplay || block instanceof MemoryBlock || block instanceof MessageBlock || block instanceof SwitchBlock)
                {
                    baseScore = 2f;
                }
                // NOTE Payloads
                else if (block instanceof PayloadBlock payloadBlock)
                {
                    baseScore = Math.min(6f * payloadBlock.payloadSpeed, 60f);

                    if (block instanceof BlockProducer blockProducer)
                    {
                        baseScore += 2f / blockProducer.buildSpeed;

                        dynamicScoreType = BlockDynamicScoreType.payloadCrafter;
                    }
                    else if (block instanceof PayloadLoader payloadLoader)
                    {
                        baseScore += 2f / payloadLoader.loadTime;

                        dynamicScoreType = BlockDynamicScoreType.itemGraph;
                    }
                    else if (block instanceof PayloadMassDriver payloadMassDriver)
                    {
                        baseScore += payloadMassDriver.maxPayloadSize + 10f / (payloadMassDriver.chargeTime + payloadMassDriver.reload) + payloadMassDriver.range / 20f;
                    }
                    else if (block instanceof UnitBlock)
                    {
                        if (block instanceof UnitFactory unitFactory)
                        {
                            // TODO Implement unit score lib
                        }
                        else if (block instanceof Reconstructor reconstructor)
                        {
                            // TODO Implement unit score lib
                        }

                        dynamicScoreType = BlockDynamicScoreType.payloadCrafter;
                    }
                    else if (block instanceof UnitAssembler unitAssembler)
                    {
                        // TODO Implement unit score lib

                        dynamicScoreType = BlockDynamicScoreType.payloadCrafter;
                    }
                }
                else if (block instanceof PayloadConveyor payloadConveyor)
                {
                    baseScore = 5f * ((PayloadConveyor) Blocks.payloadConveyor).moveTime / payloadConveyor.moveTime;
                }
                // NOTE Power
                else if (block instanceof PowerBlock)
                {
                    baseScore = GetPowerBlockScore(block);

                    if (block instanceof PowerDistributor && !(block instanceof Battery))
                    {
                        baseScore += 5f;
                    }

                    if (block instanceof BeamNode node)
                    {
                        baseScore += node.range;
                    }
                    else if (block instanceof PowerNode node)
                    {
                        baseScore += node.laserRange;
                    }
                    else if (block instanceof PowerGenerator powerGenerator)
                    {
                        baseScore += powerGenerator.explosionDamage / 500f;

                        baseScore += (float) (Math.sqrt(powerGenerator.powerProduction / thoriumReactionProduction) * 10f);

                        if (block instanceof NuclearReactor) baseScore += 10f; // Make em explode >:)
                    }

                    dynamicScoreType = BlockDynamicScoreType.powerGrid;
                }
                // NOTE Production
                else if (block instanceof GenericCrafter genericCrafter)
                {
                    float avgItemScore = 0f;
                    int itemCount = 0;
                    if (genericCrafter.outputItem != null)
                    {
                        avgItemScore = itemScores[genericCrafter.outputItem.item.id];
                        itemCount = 1;
                    }
                    else if (genericCrafter.outputItems != null)
                    {
                        for (ItemStack stack : genericCrafter.outputItems)
                        {
                            int count = stack.amount;
                            avgItemScore += itemScores[stack.item.id] * count;
                            itemCount += count;
                        }
                    }

                    if (itemCount > 0f) avgItemScore /= itemCount;

                    float avgLiquidScore = 0f;
                    float liquidAmount = 0f;
                    if (genericCrafter.outputLiquid != null)
                    {
                        avgLiquidScore = liquidScores[genericCrafter.outputLiquid.liquid.id];
                        liquidAmount = 1f;
                    }
                    else if (genericCrafter.outputLiquids != null)
                    {
                        for (LiquidStack stack : genericCrafter.outputLiquids)
                        {
                            avgLiquidScore += liquidScores[stack.liquid.id] * stack.amount;
                            liquidAmount += stack.amount;
                        }
                    }

                    if (liquidAmount > 0f) avgLiquidScore /= liquidAmount;

                    if (avgItemScore > 0f) baseScore = avgItemScore * 0.8f;
                    else baseScore = avgLiquidScore * 0.5f;

                    baseScore += Math.min(20f, 60f / genericCrafter.craftTime * (avgItemScore > 0f ? 1f : 0.5f));

                    dynamicScoreType = BlockDynamicScoreType.itemGraph;
                }
                else if (block instanceof BeamDrill beamDrill)
                {
                    baseScore = 5f * (60f / beamDrill.drillTime * beamDrill.size * beamDrill.size);

                    float maxItemScore = 0f;
                    for (int i = 0; i < beamDrill.tier && i < itemsByHardness.length; i++)
                    {
                        for (Item item : itemsByHardness[i])
                        {
                            if (itemScores[item.id] > maxItemScore) maxItemScore = itemScores[item.id];
                        }
                    }

                    baseScore += maxItemScore / 10f;

                    dynamicScoreType = BlockDynamicScoreType.itemGraph;
                }
                else if (block instanceof Drill drill)
                {
                    baseScore = 6f * (60f / drill.drillTime * drill.size * drill.size);

                    float maxItemScore = 0f;
                    for (int i = 0; i < drill.tier && i < itemsByHardness.length; i++)
                    {
                        for (Item item : itemsByHardness[i])
                        {
                            if (item != drill.blockedItem && itemScores[item.id] > maxItemScore)
                                maxItemScore = itemScores[item.id];
                        }
                    }

                    baseScore += maxItemScore / 10f;

                    dynamicScoreType = BlockDynamicScoreType.itemGraph;
                }
                else if (block instanceof Separator separator)
                {
                    float avgItemScore = 0f;
                    int itemCount = 0;
                    if (separator.results != null)
                    {
                        for (ItemStack stack : separator.results)
                        {
                            int count = stack.amount;
                            avgItemScore += itemScores[stack.item.id] * count;
                            itemCount += count;
                        }
                    }

                    avgItemScore /= itemCount;

                    baseScore = avgItemScore / 5f + 120f / separator.craftTime;

                    dynamicScoreType = BlockDynamicScoreType.itemGraph;
                }
                else if (block instanceof WallCrafter wallCrafter)
                {
                    baseScore = itemScores[wallCrafter.output.id] / 5f + 120f / wallCrafter.drillTime;

                    dynamicScoreType = BlockDynamicScoreType.itemGraph;
                }
                // NOTE Storage
                else if (block instanceof CoreBlock)
                {
                    baseScore = 10f + block.health / 100f;
                }
                else if (block instanceof StorageBlock)
                {
                    baseScore = GetStorageBlockScore(block);

                    dynamicScoreType = BlockDynamicScoreType.single;
                }
                else if (block instanceof Unloader unloader)
                {
                    baseScore = 10f + unloader.speed * 2f;

                    dynamicScoreType = BlockDynamicScoreType.itemGraph;
                }
                // NOTE Units
                else if (block instanceof RepairTower repairTower)
                {
                    baseScore = Math.min(25f, repairTower.range / 4f) + (float) Math.sqrt(repairTower.healAmount * 80f) * 2.5f;
                }
                else if (block instanceof RepairTurret repairTurret)
                {
                    baseScore = (float) Math.sqrt(repairTurret.repairRadius / Vars.tilesize * 3f) * 2f + repairTurret.repairSpeed * 12f;
                }
                else if (block instanceof UnitCargoLoader unitCargoLoader)
                {
                    baseScore = 10f + (1200f / unitCargoLoader.buildTime) * 5f;

                    dynamicScoreType = BlockDynamicScoreType.itemGraph;
                }
                else if (block instanceof UnitCargoUnloadPoint)
                {
                    baseScore = block.itemCapacity / 20f;

                    dynamicScoreType = BlockDynamicScoreType.itemGraph;
                }

                // Add a maximum of 10 to the score depending on the item importance
                float maxItemScore = 0f;
                for (ItemStack stack : block.requirements)
                {
                    float itemScore = itemScores[stack.item.id];
                    if (itemScore > maxItemScore) maxItemScore = itemScore;
                }

                baseScore += maxItemScore / 10f;

                baseScore = FalloffFunction(baseScore);

                // NOTE Sandbox
                if (block instanceof PayloadSource || block instanceof ItemSource || block instanceof LiquidSource || block instanceof PowerSource || block == Blocks.heatSource)
                {
                    baseScore = 100f; // Screw cheaters
                }

                yield baseScore;
            }
        };

        return new Pair<>(dynamicScoreType, Mathf.clamp(score, 0f, 100f));
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

        if (maxScore == 0) return; // How this could possibly happen is beyond me

        for (int i = 0; i < scores.length; i++)
        {
            scores[i] = (int) ((scores[i] / maxScore) * 100 * 1000) / 1000f;
        }
    }

    public enum BlockScoreType
    {
        wall, // Depends on health
        powerdistributor, // Depends on power capacity
        storage, // Depends on item and liquid capacity
        logic, // Score of 0
        core, // Medium priority
        defense, // High priority, should not be used for turrets since those are handled automatically. If your block's purpose is to attack units and is not a turret, use this
        ignore, // Priority of 0
        manual, // Score manually set via json
        automatic // INTERNAL USE ONLY
    }

    public enum BlockDynamicScoreType
    {
        powerGrid, // Priority increases on the importance of the power grid it's attached to
        itemGraph, // Priority increases on the importance of the blocks it provides items to
        liquidGraph, // Priority increases on the importance of the blocks it provides liquids to
        payloadCrafter, // Priority increases according to the importance of the payload it crafts
        single, // Priority is calculated based on the building's own stats
        constant // Priority does not change based on the building's stats or environment
    }
}
