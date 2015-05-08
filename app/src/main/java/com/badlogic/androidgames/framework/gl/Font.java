package com.badlogic.androidgames.framework.gl;

public class Font
{
    public final Texture texture;
    public final int glyphWidth;
    public final int glyphHeight;
    public float CharWidth=18;
    public float CharHeight=18;
    public final TextureRegion[] glyphs = new TextureRegion[96];

    public Font(Texture texture, int offsetX, int offsetY,  int glyphsPerRow, int glyphWidth, int glyphHeight)
    {
        this.texture = texture;
        this.glyphWidth = glyphWidth;
        this.glyphHeight = glyphHeight;
        int x = offsetX;
        int y = offsetY;

        for(int i = 0; i < 96; i++)
        {
            glyphs[i] = new TextureRegion(texture, x, y, glyphWidth, glyphHeight);
            x += glyphWidth;

            if(x == offsetX + glyphsPerRow * glyphWidth)
            {
                x = offsetX;
                y += glyphHeight;
            }
        }
    }
    public void FontSize(float xsize, float ysize)
    {
        CharWidth = xsize;
        CharHeight = ysize;
    }

    public void drawText( SpriteBatcher batcher, String text, float x, float y, float xsize, float ysize)
    {
        int len = text.length();

        CharWidth = xsize;
        CharHeight = ysize;

        for(int i = 0; i < len; i++)
        {
            int c = text.charAt(i) - ' ';

            if(c < 0 || c > glyphs.length - 1)
                continue;

            TextureRegion glyph = glyphs[c];
            batcher.drawSprite(x, y, CharWidth, CharHeight, glyph);
            x += CharWidth;
        }
    }
}


