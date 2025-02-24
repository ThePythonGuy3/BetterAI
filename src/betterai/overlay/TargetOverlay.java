package betterai.overlay;

import arc.*;
import arc.graphics.Color;
import arc.graphics.g2d.*;
import arc.input.KeyCode;
import arc.math.geom.Rect;
import betterai.BetterAI;
import betterai.algorithm.*;
import betterai.input.InputRegister;
import mindustry.Vars;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.Layer;
import mindustry.world.*;

public class TargetOverlay extends BaseOverlay
{
    private static final String[] dynamicScoreTypeIconNames = new String[]
            {
                    "power-grid",
                    "item-graph",
                    "liquid-graph",
                    "payload-crafter",
                    "single",
                    "constant"
            };

    private static final Color[] targetColors = new Color[]
            {
                    Team.green.color,
                    Team.sharded.color,
                    Team.crux.color
            };

    private static TextureRegion[] dynamicScoreTypeIcons;
    private static NinePatch targetRegion;

    private final Rect cameraRect = new Rect();
    private final Rect drawRect = new Rect();

    private boolean shown = false;

    @Override
    public void load()
    {
        targetRegion = new NinePatch(Core.atlas.find("betterai-target"), 3, 3, 3, 3);

        dynamicScoreTypeIcons = new TextureRegion[dynamicScoreTypeIconNames.length];
        for (int i = 0; i < dynamicScoreTypeIconNames.length; i++)
        {
            dynamicScoreTypeIcons[i] = Core.atlas.find("betterai-" + dynamicScoreTypeIconNames[i]);
        }
    }

    @Override
    public void init()
    {
        if (!Vars.headless && BetterAI.debug) InputRegister.Register(KeyCode.k, () -> shown = !shown);
    }

    @Override
    public void draw()
    {
        if (!shown) return;

        Draw.draw(Layer.groundUnit - 1, () -> {
            Core.camera.bounds(cameraRect);

            for (Building build : MapScore.GetBuildings())
            {
                Block block = build.block();
                
                int blockSize = block.size;

                drawRect.set(build.x() - blockSize / 2f * Vars.tilesize, build.y() - blockSize / 2f * Vars.tilesize, block.size * Vars.tilesize, block.size * Vars.tilesize);

                if (cameraRect.overlaps(drawRect))
                {
                    Color preColor = Draw.getColor();

                    Draw.color(Color.white);

                    float score = MapScore.GetBuildingScore(build);

                    Draw.color(Team.green.color, Team.sharded.color, Team.crux.color, score / 100f);
                    Draw.alpha(0.75f);

                    targetRegion.draw(drawRect.x, drawRect.y, drawRect.width, drawRect.height);

                    DrawText.Draw(String.valueOf((int) score), drawRect.x + 1, drawRect.y + 1, DrawText.flagOutline, 0.5f);

                    Draw.color();

                    Draw.rect(dynamicScoreTypeIcons[ContentScore.GetBlockDynamicScoreType(block).ordinal()], drawRect.x + drawRect.width - 2f, drawRect.y + drawRect.height - 2f, 3f, 3f);

                    Draw.color(preColor);
                }
            }
        });
    }
}
