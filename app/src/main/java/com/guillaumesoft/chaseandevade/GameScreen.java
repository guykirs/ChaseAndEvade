package com.guillaumesoft.chaseandevade;

import android.graphics.Point;
import com.badlogic.androidgames.framework.Game;
import com.badlogic.androidgames.framework.gl.Camera2D;
import com.badlogic.androidgames.framework.gl.SpriteBatcher;
import com.badlogic.androidgames.framework.impl.GLScreen;
import com.badlogic.androidgames.framework.math.Clamp;
import com.badlogic.androidgames.framework.math.Vector2;
import com.badlogic.androidgames.framework.math.Lerp;
import com.badlogic.androidgames.framework.XOBJ;
import java.util.Random;
import javax.microedition.khronos.opengles.GL10;
import tv.ouya.console.api.OuyaController;

/// <summary>
/// Sample showing how to implement simple chase, evade, and wander AI behaviors.
/// The behaviors are based on the TurnToFace function, which was explained in
/// AI Sample 1: Aiming.
/// </summary>
public class GameScreen extends GLScreen
{
    // The following values control the different characteristics of the characters
    // in this sample, including their speed, turning rates. distances are specified
    // in pixels, angles are specified in radians.

    // how fast can the cat move?
    final float MaxCatSpeed = 7.5f;

    // how fast can the tank move?
    final float MaxTankSpeed = 5.0f;

    // how fast can he turn?
    final float TankTurnSpeed = 0.10f;

    // this value controls the distance at which the tank will start to chase the
    // cat.
    final float TankChaseDistance = 250.0f;

    // TankCaughtDistance controls the distance at which the tank will stop because
    // he has "caught" the cat.
    final float TankCaughtDistance = 60.0f;

    // this finalant is used to avoid hysteresis, which is common in ai programming.
    // see the doc for more details.
    final float TankHysteresis = 15.0f;

    // how fast can the mouse move?
    final float MaxMouseSpeed = 8.5f;

    // and how fast can it turn?
    final float MouseTurnSpeed = 0.20f;

    // MouseEvadeDistance controls the distance at which the mouse will flee from
    // cat. If the mouse is further than "MouseEvadeDistance" pixels away, he will
    // consider himself safe.
    final float MouseEvadeDistance = 200.0f;

    // this finalant is similar to TankHysteresis. The value is larger than the
    // tank's hysteresis value because the mouse is faster than the tank: with a
    // higher velocity, small fluctuations are much more visible.
    final float MouseHysteresis = 60.0f;

    Vector2 tankTextureCenter;
    Vector2 tankPosition;
    Vector2 tankWanderDirection;
    Vector2 catTextureCenter;
    Vector2 catPosition;
    Vector2 mouseTextureCenter;
    Vector2 mousePosition;
    Vector2 cameraPosition;
    Vector2 mouseWanderDirection;
    Vector2 scrollOffset;
    OuyaController currentGamePadState;
    OuyaController previousGamePadState;
    TankAiState tankState = TankAiState.Wander;
    MouseAiState mouseState = MouseAiState.Wander;

    // We store our input states so that we only poll once per frame,
    // then we use the same input state wherever needed
    // private OuyaController gamePadState;
    private SpriteBatcher batcher;
    private Camera2D guiCam;
    float tankOrientation;
    float mouseOrientation;

    Random random = new Random();

    /// <summary>
    /// TankAiState is used to keep track of what the tank is currently doing.
    /// </summary>
    enum TankAiState
    {
        // chasing the cat
        Chasing,
        // the tank has gotten close enough that the cat that it can stop chasing it
        Caught,
        // the tank can't "see" the cat, and is wandering around.
        Wander
    }

    /// <summary>
    /// MouseAiState is used to keep track of what the mouse is currently doing.
    /// </summary>
    enum MouseAiState
    {
        // evading the cat
        Evading,
        // the mouse can't see the "cat", and it's wandering around.
        Wander
    }

    /// <summary>
    /// standard everyday finalructor, nothing too fancy here.
    /// </summary>
    public GameScreen(Game game)
    {
        super(game);

        this.batcher = new SpriteBatcher(glGraphics, 1000);
        guiCam       = new Camera2D(glGraphics, 1920, 1080);

        // once all the content is loaded, we can calculate the centers of each
        // of the textures that we loaded. Just like in the previous sample in
        // this series, the aiming sample, we want spriteBatch to draw the
        // textures centered on their position vectors. SpriteBatch.Draw will
        // center the sprite on the vector that we pass in as the "origin"
        // parameter, so we'll just calculate that to be the middle of
        // the texture.
        tankTextureCenter  = new Vector2(Assets.tankRegion.texture.width  / 2, Assets.tankRegion.texture.height / 2);
        catTextureCenter   = new Vector2(Assets.catRegion.texture.width   / 2, Assets.catRegion.texture.height / 2);
        mouseTextureCenter = new Vector2(Assets.mouseRegion.texture.width / 2, Assets.mouseRegion.texture.height / 2);

        // once base.Initialize has finished, the GraphicsDevice will have been
        // created, and we'll know how big the Viewport is. We want the tank, cat
        // and mouse to be spread out across the screen, so we'll use the viewport
        // to figure out where they should be.
        Point size = new Point();
        ScreenManager.display.getSize(size);

        tankPosition  = new Vector2(size.x / 4, size.y / 2);
        catPosition   = new Vector2(size.x / 2, size.y / 2);
        mousePosition = new Vector2(3 * size.x / 4, size.y / 2);

    }

    /// <summary>
    /// Updates the camera position, scrolling the
    /// screen if the cat gets too close to the edge.
    /// </summary>
    void UpdateCamera()
    {
        Point size = new Point();
        ScreenManager.display.getSize(size);

        // How far away from the camera should we allow the cat
        // to move before we scroll the camera to follow it?
        Vector2 maxScroll = new Vector2(size.x /2, size.y /2);

        // Apply a safe area to prevent the cat getting too close to the edge
        // of the screen. Note that this is even more restrictive than the 80%
        // safe area used for the overlays, because we want to start scrolling
        // even before the cat gets right up to the edge of the legal area.
        final float catSafeArea = 0.7f;

        maxScroll.mul(catSafeArea);

        // Adjust for the size of the cat sprite, so we will start
        // scrolling based on the edge rather than center of the cat.
        maxScroll.sub(new Vector2(Assets.catRegion.texture.width /2, Assets.catRegion.texture.height /2));

        // Make sure the camera stays within the desired distance of the cat.
        Vector2 min = catPosition.sub(maxScroll);
        Vector2 max = catPosition.sub(maxScroll);

        //cameraPosition.x =  Clamp.clamp(cameraPosition.x, min.x, max.x);
       // cameraPosition.y =  Clamp.clamp(cameraPosition.y, min.y, max.y);
    }

    /// <summary>
    /// Allows the game to run logic.
    /// </summary>
    @Override
    public void update(float gameTime)
    {
        // Work out how far to scroll based on the current camera position.
        Vector2 screenCenter = new Vector2(1920 /2, 1080  / 2);
       // scrollOffset.add(screenCenter.sub(cameraPosition));

        // handle input will read the controller input, and update the cat
        // to move according to the user's whim.
        currentGamePadState = OuyaController.getControllerByPlayer(0);
        HandleInput(currentGamePadState);

        // UpdateTank will run the AI code that controls the tank's movement...
        // UpdateTank(scrollOffset);

        // ... and UpdateMouse does the same thing for the mouse.
        // UpdateMouse(scrollOffset);

        // Once we've finished that, we'll use the ClampToViewport helper function
        // to clamp everyone's position so that they stay on the screen.
        // tankPosition  = ClampToViewport(tankPosition);
        //catPosition   = ClampToViewport(catPosition);
        // mousePosition = ClampToViewport(mousePosition);

        UpdateCamera();

    }

    /// <summary>
/// This function takes a Vector2 as input, and returns that vector "clamped"
/// to the current graphics viewport. We use this function to make sure that
/// no one can go off of the screen.
/// </summary>
/// <param name="vector">an input vector</param>
/// <returns>the input vector, clamped between the minimum and maximum of the
/// viewport.</returns>
    private Vector2 ClampToViewport(Vector2 vector)
    {
        Point size = new Point();
        ScreenManager.display.getSize(size);

        vector.x = Clamp.clamp(vector.x, size.x, size.x);// + vp.Width);
        vector.y = Clamp.clamp(vector.y, size.y, size.y);// + vp.Height);
        return vector;
    }

    /// <summary>
/// This function contains the code that controls the mouse. It decides what the
/// mouse should do based on the position of the cat: if the cat is too close,
/// it will attempt to flee. Otherwise, it will idly wander around the screen.
///
/// </summary>
    private void UpdateMouse(Vector2 scrollOffset)
    {
        Vector2 position = new Vector2(catPosition.sub(catTextureCenter).add(scrollOffset));

        // first, calculate how far away the mouse is from the cat, and use that
        // information to decide how to behave. If they are too close, the mouse
        // will switch to "active" mode - fleeing. if they are far apart, the mouse
        // will switch to "idle" mode, where it roams around the screen.
        // we use a hysteresis finalant in the decision making process, as described
        // in the accompanying doc file.
        float distanceFromCat =  mousePosition.dist(position);

        //float distanceFromCat = Vector2.Distance(mousePosition, position);

        // the cat is a safe distance away, so the mouse should idle:
        if (distanceFromCat > MouseEvadeDistance + MouseHysteresis)
        {
            mouseState = MouseAiState.Wander;
        }
        // the cat is too close; the mouse should run:
        else if (distanceFromCat < MouseEvadeDistance - MouseHysteresis)
        {
            mouseState = MouseAiState.Evading;
        }
        // if neither of those if blocks hit, we are in the "hysteresis" range,
        // and the mouse will continue doing whatever it is doing now.

        // the mouse will move at a different speed depending on what state it
        // is in. when idle it won't move at full speed, but when actively evading
        // it will move as fast as it can. this variable is used to track which
        // speed the mouse should be moving.
        float currentMouseSpeed;

        // the second step of the Update is to change the mouse's orientation based
        // on its current state.
        if (mouseState == MouseAiState.Evading)
        {
            // If the mouse is "active," it is trying to evade the cat. The evasion
            // behavior is accomplished by using the TurnToFace function to turn
            // towards a point on a straight line facing away from the cat. In other
            // words, if the cat is point A, and the mouse is point B, the "seek
            // point" is C.
            //     C
            //   B
            // A
            // Vector2 seekPosition = 2 * mousePosition - position;

            Vector2 seekPosition = mousePosition.sub(position).mul(2);

            // Use the TurnToFace function, which we introduced in the AI Series 1:
            // Aiming sample, to turn the mouse towards the seekPosition. Now when
            // the mouse moves forward, it'll be trying to move in a straight line
            // away from the cat.
            mouseOrientation = TurnToFace(mousePosition, seekPosition,  mouseOrientation, MouseTurnSpeed);

            // set currentMouseSpeed to MaxMouseSpeed - the mouse should run as fast
            // as it can.
            currentMouseSpeed = MaxMouseSpeed;
        }
        else
        {
            // if the mouse isn't trying to evade the cat, it should just meander
            // around the screen. we'll use the Wander function, which the mouse and
            // tank share, to accomplish this. mouseWanderDirection and
            // mouseOrientation are passed by ref so that the wander function can
            // modify them. for more information on ref parameters, see
            // http://msdn2.microsoft.com/en-us/library/14akc2c7(VS.80).aspx
            Wander(mousePosition, mouseWanderDirection, mouseOrientation, MouseTurnSpeed);

            // if the mouse is wandering, it should only move at 25% of its maximum
            // speed.
            currentMouseSpeed = .25f * MaxMouseSpeed;
        }

        // The final step is to move the mouse forward based on its current
        // orientation. First, we finalruct a "heading" vector from the orientation
        // angle. To do this, we'll use Cosine and Sine to tell us the x and y
        // components of the heading vector. See the accompanying doc for more
        // information.
        Vector2 heading = new Vector2((float)Math.cos(mouseOrientation), (float)Math.sin(mouseOrientation));

        // by multiplying the heading and speed, we can get a velocity vector. the
        // velocity vector is then added to the mouse's current position, moving him
        // forward.
        mousePosition.add(heading).mul(currentMouseSpeed);
    }

    /// <summary>
    /// UpdateTank runs the AI code that will update the tank's orientation and
    /// position. It is very similar to UpdateMouse, but is slightly more
    /// complicated: where mouse only has two states, idle and active, the Tank has
    /// three.
    /// </summary>
    private void UpdateTank(Vector2 scrollOffset)
    {
        Vector2 position = catPosition.sub(catTextureCenter).add(scrollOffset);

        // However, the tank's behavior is more complicated than the mouse's, and so
        // the decision making process is a little different.

        // First we have to use the current state to decide what the thresholds are
        // for changing state, as described in the doc.

        float tankChaseThreshold = TankChaseDistance;
        float tankCaughtThreshold = TankCaughtDistance;

        // if the tank is idle, he prefers to stay idle. we do this by making the
        // chase distance smaller, so the tank will be less likely to begin chasing
        // the cat.
        if (tankState == TankAiState.Wander)
        {
            tankChaseThreshold -= TankHysteresis / 2;
        }
        // similarly, if the tank is active, he prefers to stay active. we
        // accomplish this by increasing the range of values that will cause the
        // tank to go into the active state.
        else if (tankState == TankAiState.Chasing)
        {
            tankChaseThreshold += TankHysteresis / 2;
            tankCaughtThreshold -= TankHysteresis / 2;
        }
        // the same logic is applied to the finished state.
        else if (tankState == TankAiState.Caught)
        {
            tankCaughtThreshold += TankHysteresis / 2;
        }

        // Second, now that we know what the thresholds are, we compare the tank's
        // distance from the cat against the thresholds to decide what the tank's
        // current state is.
        //float distanceFromCat = Vector2.Distance(tankPosition, catPosition);

        float distanceFromCat = tankPosition.dist(position);


        if (distanceFromCat > tankChaseThreshold)
        {
            // just like the mouse, if the tank is far away from the cat, it should
            // idle.
            tankState = TankAiState.Wander;
        }
        else if (distanceFromCat > tankCaughtThreshold)
        {
            tankState = TankAiState.Chasing;
        }
        else
        {
            tankState = TankAiState.Caught;
        }

        // Third, once we know what state we're in, act on that state.
        float currentTankSpeed = 0.0f;

        if (tankState == TankAiState.Chasing)
        {
            // the tank wants to chase the cat, so it will just use the TurnToFace
            // function to turn towards the cat's position. Then, when the tank
            // moves forward, he will chase the cat.
            //tankOrientation = TurnToFace(tankPosition, catPosition, tankOrientation, TankTurnSpeed);
            tankOrientation = TurnToFace(tankPosition, position, tankOrientation, TankTurnSpeed);

            currentTankSpeed = MaxTankSpeed;
        }
        else if (tankState == TankAiState.Wander)
        {
            // wander works just like the mouse's.
            //Wander(tankPosition, ref tankWanderDirection, ref tankOrientation, TankTurnSpeed);
            //currentTankSpeed = .25f * MaxTankSpeed;
        }
        else
        {
            // this part is different from the mouse. if the tank catches the cat,
            // it should stop. otherwise it will run right by, then spin around and
            // try to catch it all over again. The end result is that it will kind
            // of "run laps" around the cat, which looks funny, but is not what
            // we're after.
            currentTankSpeed = 0.0f;
        }

        // this calculation is also just like the mouse's: we finalruct a heading
        // vector based on the tank's orientation, and then make the tank move along
        // that heading.
        Vector2 heading = new Vector2((float)Math.cos(tankOrientation), (float)Math.sin(tankOrientation));
        tankPosition.add(heading.mul(currentTankSpeed));
    }

    /// <summary>
    /// Wander contains functionality that is shared between both the mouse and the
    /// tank, and does just what its name implies: makes them wander around the
    /// screen. The specifics of the function are described in more detail in the
    /// accompanying doc.
    /// </summary>
    /// <param name="position">the position of the character that is wandering
    /// </param>
    /// <param name="wanderDirection">the direction that the character is currently
    /// wandering. this parameter is passed by reference because it is an input and
    /// output parameter: Wander accepts it as input, and will update it as well.
    /// </param>
    /// <param name="orientation">the character's orientation. this parameter is
    /// also passed by reference and is an input/output parameter.</param>
    /// <param name="turnSpeed">the character's maximum turning speed.</param>
    private void Wander(Vector2 position, Vector2 wanderDirection, float orientation, float turnSpeed)
    {
        // The wander effect is accomplished by having the character aim in a random
        // direction. Every frame, this random direction is slightly modified.
        // Finally, to keep the characters on the center of the screen, we have them
        // turn to face the screen center. The further they are from the screen
        // center, the more they will aim back towards it.

        // the first step of the wander behavior is to use the random number
        // generator to offset the current wanderDirection by some random amount.
        // .25 is a bit of a magic number, but it controls how erratic the wander
        // behavior is. Larger numbers will make the characters "wobble" more,
        // smaller numbers will make them more stable. we want just enough
        // wobbliness to be interesting without looking odd.
        // wanderDirection.x += Lerp(-.25f, .25f, (float)random.nextDouble());
        //wanderDirection.y += Lerp(-.25f, .25f, (float)random.nextDouble());

        // we'll renormalize the wander direction, ...
        if (wanderDirection != Vector2.Zero())
        {
            //wanderDirection.Normalize();
        }
        // ... and then turn to face in the wander direction. We don't turn at the
        // maximum turning speed, but at 15% of it. Again, this is a bit of a magic
        // number: it works well for this sample, but feel free to tweak it.
        //orientation = TurnToFace(position, position + wanderDirection, orientation, .15f * turnSpeed);


        // next, we'll turn the characters back towards the center of the screen, to
        // prevent them from getting stuck on the edges of the screen.
        Vector2 screenCenter = new Vector2();
        //screenCenter.x = graphics.GraphicsDevice.Viewport.Width / 2;
        //screenCenter.y = graphics.GraphicsDevice.Viewport.Height / 2;

        // Here we are creating a curve that we can apply to the turnSpeed. This
        // curve will make it so that if we are close to the center of the screen,
        // we won't turn very much. However, the further we are from the screen
        // center, the more we turn. At most, we will turn at 30% of our maximum
        // turn speed. This too is a "magic number" which works well for the sample.
        // Feel free to play around with this one as well: smaller values will make
        // the characters explore further away from the center, but they may get
        // stuck on the walls. Larger numbers will hold the characters to center of
        // the screen. If the number is too large, the characters may end up
        // "orbiting" the center.
        float distanceFromScreenCenter = screenCenter.dist(position);
        float MaxDistanceFromScreenCenter = Math.min(screenCenter.y, screenCenter.x);

        float normalizedDistance = distanceFromScreenCenter / MaxDistanceFromScreenCenter;

        float turnToCenterSpeed = .3f * normalizedDistance * normalizedDistance * turnSpeed;

        // once we've calculated how much we want to turn towards the center, we can
        // use the TurnToFace function to actually do the work.
        orientation = TurnToFace(position, screenCenter, orientation,  turnToCenterSpeed);
    }


    /// <summary>
    /// Calculates the angle that an object should face, given its position, its
    /// target's position, its current angle, and its maximum turning speed.
    /// </summary>
    private static float TurnToFace(Vector2 position, Vector2 faceThis, float currentAngle, float turnSpeed)
    {
        // consider this diagram:
        //         B
        //        /|
        //      /  |
        //    /    | y
        //  / o    |
        // A--------
        //     x
        //
        // where A is the position of the object, B is the position of the target,
        // and "o" is the angle that the object should be facing in order to
        // point at the target. we need to know what o is. using trig, we know that
        //      tan(theta)       = opposite / adjacent
        //      tan(o)           = y / x
        // if we take the arctan of both sides of this equation...
        //      arctan( tan(o) ) = arctan( y / x )
        //      o                = arctan( y / x )
        // so, we can use x and y to find o, our "desiredAngle."
        // x and y are just the differences in position between the two objects.
        float x = faceThis.x - position.x;
        float y = faceThis.y - position.y;

        // we'll use the Atan2 function. Atan will calculates the arc tangent of
        // y / x for us, and has the added benefit that it will use the signs of x
        // and y to determine what cartesian quadrant to put the result in.
        // http://msdn2.microsoft.com/en-us/library/system.math.atan2.aspx
        float desiredAngle = (float)Math.atan2(y, x);

        // so now we know where we WANT to be facing, and where we ARE facing...
        // if we weren't finalrained by turnSpeed, this would be easy: we'd just
        // return desiredAngle.
        // instead, we have to calculate how much we WANT to turn, and then make
        // sure that's not more than turnSpeed.

        // first, figure out how much we want to turn, using WrapAngle to get our
        // result from -Pi to Pi ( -180 degrees to 180 degrees )
        float difference = WrapAngle(desiredAngle - currentAngle);

        // clamp that between -turnSpeed and turnSpeed.
        difference = Clamp.clamp(difference, -turnSpeed, turnSpeed);

        // so, the closest we can get to our target is currentAngle + difference.
        // return that, using WrapAngle again.
        return WrapAngle(currentAngle + difference);
    }

    /// <summary>
    /// Returns the angle expressed in radians between -Pi and Pi.
    /// <param name="radians">the angle to wrap, in radians.</param>
    /// <returns>the input value expressed in radians from -Pi to Pi.</returns>
    /// </summary>
    private static float WrapAngle(float radians)
    {
        while (radians < -Math.PI)
        {
            radians += Math.PI * 2;
        }
        while (radians > Math.PI)
        {
             radians -= Math.PI * 2;
        }
        return radians;
    }

    /// <summary>
    /// This is called when the game should draw itself. Nothing too fancy in here,
    /// we'll just call Begin on the SpriteBatch, and then draw the tank, cat, and
    /// mouse, and some overlay text. Once we're finished drawing, we'll call
    /// SpriteBatch.End.
    /// </summary>
    @Override
    public void present(float deltaTime)
    {

        // Work out how far to scroll based on the current camera position.
        Vector2 screenCenter = new Vector2(1920  /2, 1080 /2);
        //Vector2 scrollOffset = screenCenter.sub(cameraPosition);

         GL10 gl = glGraphics.getGL();
        gl.glClear(GL10.GL_COLOR_BUFFER_BIT);

        guiCam.setViewportAndMatrices();

        gl.glEnable(GL10.GL_TEXTURE_2D);


        this.batcher.beginBatch(Assets.ItemsTexure);

           // draw the tank, cat and mouse...
           //this.batcher.drawSprite(tankTexture, tankPosition, null, Color.White, tankOrientation, tankTextureCenter, 1.0f, SpriteEffects.None, 0.0f);

           // DrawCat(scrollOffset);
           //spriteBatch.Draw(catTexture, catPosition, null, Color.White, 0.0f, catTextureCenter, 1.0f, SpriteEffects.None, 0.0f);



           batcher.drawSprite(mousePosition.x, mousePosition.y, 100.0f, 100.0f, Assets.mouseRegion);
           //this.batcher.drawSprite(mouseTexture, mousePosition, null, Color.White, mouseOrientation, mouseTextureCenter, 1.0f, SpriteEffects.None, 0.0f);

           // and then draw some text showing the tank's and mouse's current state.
           // to make the text stand out more, we'll draw the text twice, once black
           // and once white, to create a drop shadow effect.
           /* Vector2 shadowOffset = Vector2.One;

           spriteBatch.DrawString(spriteFont, "Tank State: " + tankState.ToString(), new Vector2(50, 100) + shadowOffset, Color.Black);
           spriteBatch.DrawString(spriteFont, "Tank State: " + tankState.ToString(), new Vector2(50, 100), Color.White);

           spriteBatch.DrawString(spriteFont, "Mouse State: " + mouseState.ToString(), new Vector2(50, 175) + shadowOffset, Color.Black);
           spriteBatch.DrawString(spriteFont, "Mouse State: " + mouseState.ToString(), new Vector2(50, 175), Color.White);

           DrawOverlays();*/

        this.batcher.endBatch();

        gl.glEnable(GL10.GL_BLEND);
        gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);

        this.batcher.beginBatch(Assets.RedFont);

           Assets.redfont.drawText(batcher, "Tank State: ", 800, 1080 - 100, 20.0f, 20.0f);
           Assets.redfont.drawText(batcher, "Mouse State:", 800, 1080 - 200, 20.0f, 20.0f);

        this.batcher.endBatch();

        gl.glDisable(GL10.GL_BLEND);


    }

    /// <summary>
    /// Draws the cat sprite.
    /// </summary>
    void DrawCat(Vector2 scrollOffset)
    {
        //Vector2 position = catPosition.set(catTextureCenter).add(scrollOffset);

        // this.batcher.drawSprite(catTexture, position, null, Color.White, 0.0f, catTextureCenter, 1.0f, SpriteEffects.None, 0.0f);
        //batcher.drawSprite(position.x, position.y, 10.0f, 10.0f, Assets.catRegion);
        batcher.drawSprite(catPosition.x, catPosition.y, 100.0f, 100.0f, Assets.catRegion);
    }

    /// <summary>
    /// Handles input for quitting the game.
    /// </summary>
    void HandleInput(OuyaController gamePadState)
    {

        float axisX = gamePadState.getAxisValue(OuyaController.AXIS_LS_X);
        float axisY = gamePadState.getAxisValue(OuyaController.AXIS_LS_Y);

        if (axisX * axisX + axisY * axisY < OuyaController.STICK_DEADZONE * OuyaController.STICK_DEADZONE)
        {
            axisX = axisY = 0.0f;
        }

        // check to see if the user wants to move the cat. we'll create a vector
        // called catMovement, which will store the sum of all the user's inputs.
        Vector2 catMovement = new Vector2(axisX, axisY);

        // flip y: on the thumbsticks, down is -1, but on the screen, down is bigger
        // numbers.
        catMovement.y *= -1;

        if (gamePadState.getButton(OuyaController.BUTTON_DPAD_LEFT))
        {
            catMovement.x -= 1.0f;
        }

        if (gamePadState.getButton(OuyaController.BUTTON_DPAD_LEFT))
        {
            catMovement.x += 1.0f;
        }

        if (gamePadState.getButton(OuyaController.BUTTON_DPAD_UP))
        {
            catMovement.y -= 1.0f;
        }

        if (gamePadState.getButton(OuyaController.BUTTON_DPAD_UP))
        {
            catMovement.y += 1.0f;
        }


       /* //Move toward the touch point. We slow down the cat when it gets within a distance of MaxCatSpeed to the touch point.
        float smoothStop = 1;
        Vector2 mousePosition = new Vector2(currentMouseState.X, currentMouseState.Y);

        if (currentMouseState.LeftButton == ButtonState.Pressed && mousePosition != catPosition)
        {
            catMovement = mousePosition - catPosition;
            float delta = MaxCatSpeed - Clamp.clamp(catMovement.len(), 0, MaxCatSpeed);
            smoothStop = 1 - delta / MaxCatSpeed;
        }
*/
        // normalize the user's input, so the cat can never be going faster than
        // CatSpeed.
        if (catMovement != Vector2.Zero())
        {
            //catMovement.Normalize();
        }

        // catPosition += catMovement * MaxCatSpeed * smoothStop;

        //catPosition.add(catMovement).mul(MaxCatSpeed);

    }


    @Override
    public void pause() {   }

    @Override
    public void resume() {  }

    @Override
    public void dispose()  {  }
}





