package betterai.overlay;

import arc.graphics.Color;
import arc.graphics.g2d.*;
import arc.scene.ui.layout.Scl;
import arc.util.Align;
import arc.util.pooling.Pools;
import mindustry.ui.Fonts;

// Based off WorldLabelComp
public class DrawText
{
    public static final byte flagBackground = 1, flagOutline = 2;

    public byte flags = flagBackground | flagOutline;

    public static void Draw(String text, float x, float y, byte flags, float fontSize)
    {
        Font font = (flags & flagOutline) != 0 ? Fonts.outline : Fonts.def;
        GlyphLayout layout = Pools.obtain(GlyphLayout.class, GlyphLayout::new);

        boolean ints = font.usesIntegerPositions();
        font.setUseIntegerPositions(false);
        font.getData().setScale(0.25f / Scl.scl(1f) * fontSize);
        layout.setText(font, text);

        if ((flags & flagBackground) != 0)
        {
            Draw.color(0f, 0f, 0f, 0.3f);
            Fill.rect(x - 1, y + layout.height + 1, layout.width + 2, layout.height + 2);
            Draw.color();
        }

        font.setColor(Color.white);
        font.draw(text, x, y + layout.height, 0, Align.bottomLeft, false);

        Draw.reset();
        Pools.free(layout);
        font.getData().setScale(1f);
        font.setColor(Color.white);
        font.setUseIntegerPositions(ints);
    }
}
