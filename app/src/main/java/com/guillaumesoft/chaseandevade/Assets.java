package com.guillaumesoft.chaseandevade;

import com.badlogic.androidgames.framework.Music;
import com.badlogic.androidgames.framework.Sound;
import com.badlogic.androidgames.framework.gl.Animation;
import com.badlogic.androidgames.framework.gl.Font;
import com.badlogic.androidgames.framework.gl.Texture;
import com.badlogic.androidgames.framework.gl.TextureRegion;
import com.badlogic.androidgames.framework.impl.GLGame;

///  THIS CLASS SETS THE ASSETS FOR THE GAME
///  JUNE 23, 2014
///  GUILLAUME SWOLFS
///  GUILLAUMESOFT
public class Assets
{
    // GAME BACKGROUND
    public static Texture background;
    public static TextureRegion backgroundRegion;

    public static Texture ItemsTexure;
    public static TextureRegion catRegion;
    public static TextureRegion mouseRegion;
    public static TextureRegion tankRegion;


    // FONT COLLORS
    public static Texture BlackFont;
    public static Font blackfont;

    public static Texture RedFont;
    public static Font redfont;

    public static Texture BlueFont;
    public static Font bluefont;

    public static Music music;
    public static Sound exitReached;
    public static Sound gemCollected;
    public static Sound monsterkilled;
    public static Sound playerfall;
    public static Sound playerjump;
    public static Sound powerup;
    public static Sound playerKilledSound;
    public static Music drum;

    public static void load(GLGame game)
    {
        background           = new Texture(game, "gamescreen.png");
        backgroundRegion     = new TextureRegion(background, 0, 0, 512, 512);

        BlackFont = new Texture(game, "blackFont.png");
        blackfont = new Font(BlackFont, 224, 0, 16, 16, 20);

        RedFont = new Texture(game, "redFont.png");
        redfont = new Font(RedFont, 224, 0, 16, 16, 20);

        BlueFont  = new Texture(game, "blueFont.png");
        bluefont  = new Font(BlueFont, 224, 0, 16, 16, 20);

        ItemsTexure = new Texture(game, "Items.png");
        catRegion   = new TextureRegion(ItemsTexure,   0, 0, 512, 512);
        mouseRegion = new TextureRegion(ItemsTexure,  64, 0, 512, 512);
        tankRegion  = new TextureRegion(ItemsTexure, 128, 0, 512, 512);

        // MUSIC SECTION
        music = game.getAudio().newMusic("Music.mp3");
        music.setLooping(true);
        music.setVolume(0.3f);

        // SOUND SECTION
        exitReached       = game.getAudio().newSound("ExitReached.ogg");
        gemCollected      = game.getAudio().newSound("GemCollected.ogg");
        monsterkilled     = game.getAudio().newSound("MonsterKilled.ogg");
        playerfall        = game.getAudio().newSound("PlayerFall.ogg");
        playerjump        = game.getAudio().newSound("PlayerJump.ogg");
        powerup           = game.getAudio().newSound("Powerup.ogg");
        playerKilledSound = game.getAudio().newSound("PlayerKilled.ogg");

        drum = game.getAudio().newMusic("drum.wav");
        drum.setLooping(true);
        drum.setVolume(0.5f);
    }

    public static void reload()
    {
        background.reload();
        BlackFont.reload();
        RedFont.reload();
    }

    public static void playMusic()
    {
        if(Settings.musicEnabled)
            music.play();
    }

    public static void playSound(Sound sound)
    {
        if(Settings.soundEnabled)
            sound.play(1.0f);
    }
}