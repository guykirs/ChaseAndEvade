package com.guillaumesoft.chaseandevade;

import com.badlogic.androidgames.framework.Game;
import com.badlogic.androidgames.framework.gl.Camera2D;
import com.badlogic.androidgames.framework.gl.SpriteBatcher;
import com.badlogic.androidgames.framework.impl.GLScreen;
import javax.microedition.khronos.opengles.GL10;

//////////////////////////////////////////////////////////////////
// October /21 /2014
// Guillaume Swolfs
// guillaumesoft
// PressStartScreen class
// THIS CLASS GETS THE CONTROLLER ID THE USER PRESSED THE BUTTON ON
//////////////////////////////////////////////////////////////////
public class PressStartScreen extends GLScreen
{
    ////////////////////////////////////////////////
    // CLASS VARAIBLES
    ////////////////////////////////////////////////

    private Camera2D guiCam;
    private SpriteBatcher batcher;
    private String message = "";
    private float scale;

    ////////////////////////////////////////////////
    // CONSTRUCTOR
    ////////////////////////////////////////////////
    public PressStartScreen(Game game)
    {
        super(game);

        // SET THE CAMERA
        guiCam   = new Camera2D(glGraphics, 1920, 1080);
        batcher  = new SpriteBatcher(glGraphics, 100);

        String buttonName = "O";
        message = "Press " + buttonName + " to get started!";

    }

    @Override
    public void update(float deltaTime)
    {
        // Pulsate the size of the selected menu entry.
        double time = System.currentTimeMillis() / 60;

        float pulsate = (float) Math.sin(time * 6) + 1;

        scale = 1 + pulsate * 0.05f;
    }

    @Override
    public void present(float deltaTime)
    {
        GL10 gl = glGraphics.getGL();
        gl.glClear(GL10.GL_COLOR_BUFFER_BIT);

        guiCam.setViewportAndMatrices();

        gl.glEnable(GL10.GL_TEXTURE_2D);

        batcher.beginBatch(Assets.background);

        // SHOW THE RATING SCREEN
        batcher.drawSprite( guiCam.position.x, guiCam.position.y, 1920, 1080, Assets.backgroundRegion);

        batcher.endBatch();

        gl.glEnable(GL10.GL_BLEND);
        gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);

        batcher.beginBatch(Assets.RedFont);

        // SHOW THE MESSAGE TEXT
        Assets.redfont.drawText(batcher, message, 1920 /2 - 100, 1080 /2, 25.0f * scale, 25.0f * scale);

        batcher.endBatch();

        gl.glDisable(GL10.GL_BLEND);
    }

    @Override
    public void pause() {   }

    @Override
    public void resume() {  }

    @Override
    public void dispose()  {  }
}