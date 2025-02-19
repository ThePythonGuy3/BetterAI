package betterai.overlay;

import arc.*;
import arc.graphics.Color;
import arc.graphics.g2d.*;
import arc.input.KeyCode;
import arc.math.geom.Rect;
import betterai.BetterAI;
import betterai.algorithm.ContentScore;
import betterai.input.InputRegister;
import mindustry.Vars;
import mindustry.game.*;
import mindustry.gen.Buildingc;
import mindustry.graphics.Layer;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;

public class TargetOverlay extends BaseOverlay
{
    private final Rect cameraRect = new Rect();
    private final Rect drawRect = new Rect();
    private NinePatch targetRegion;
    private boolean shown = false;

    @Override
    public void init()
    {
        shown = BetterAI.debug;

        targetRegion = new NinePatch(Core.atlas.find("betterai-target"), 3, 3, 3, 3);

        InputRegister.Register(KeyCode.k, () -> shown = !shown);
    }

    @Override
    public void draw()
    {
        if (!shown) return;

        Draw.draw(Layer.groundUnit - 1, () -> {
            Core.camera.bounds(cameraRect);

            for (Tile tile : Vars.world.tiles)
            {
                Block block = tile.block();

                if (block == null || block instanceof Floor || block instanceof Prop) continue;
                Buildingc build = tile.build;

                if (build != null)
                {
                    int blockSize = build.block().size;

                    drawRect.set(build.x() - blockSize / 2f * Vars.tilesize, build.y() - blockSize / 2f * Vars.tilesize, build.block().size * Vars.tilesize, build.block().size * Vars.tilesize);

                    if (cameraRect.overlaps(drawRect))
                    {
                        Color preColor = Draw.getColor();

                        float score = ContentScore.GetBlockScore(build.block());
                        Draw.color(Team.green.color, Team.crux.color, score / 100f);
                        Draw.alpha(0.75f);

                        targetRegion.draw(drawRect.x, drawRect.y, drawRect.width, drawRect.height);

                        DrawText.Draw(String.valueOf((int) score), drawRect.x + 1, drawRect.y + 1, DrawText.flagOutline, 0.5f);

                        Draw.color(preColor);
                    }
                }
            }
        });
    }
}
