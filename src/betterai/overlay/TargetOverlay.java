package betterai.overlay;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.*;
import arc.math.Mathf;
import arc.math.geom.Rect;
import mindustry.Vars;
import mindustry.game.Team;
import mindustry.gen.Buildingc;
import mindustry.graphics.Layer;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;

public class TargetOverlay extends BaseOverlay
{
    private final Rect cameraRect = new Rect();
    private final Rect drawRect = new Rect();
    private NinePatch targetRegion;

    @Override
    public void init()
    {
        targetRegion = new NinePatch(Core.atlas.find("betterai-target"), 3, 3, 3, 3);
    }

    @Override
    public void draw()
    {
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

                        Color color = new Color(Team.green.color).lerp(Team.crux.color, Mathf.clamp(build.block().size / 5f - 1 / 5f, 0f, 1f));
                        color.a = 0.75f;

                        Draw.color(color);
                        targetRegion.draw(drawRect.x, drawRect.y, drawRect.width, drawRect.height);

                        Draw.color(preColor);
                    }
                }
            }
        });
    }
}
