package com.example.ml_vision_app;

import android.content.Context;
import android.media.SoundPool;
import android.media.AudioManager;

import java.util.HashMap;

//forrás: https://github.com/alantanlc/virtual-piano/blob/master/src/com/alantan/virtualpiano/SoundPoolPlayer.java
public class SoundPlayer {

    private SoundPool sp;
    private HashMap pianoNotes = new HashMap();

    public SoundPlayer(Context pContext)
    {
        // setup Soundpool
        this.sp = new SoundPool(4, AudioManager.STREAM_MUSIC, 0);

        pianoNotes.put(R.raw.a4, this.sp.load(pContext, R.raw.a4, 1));
        pianoNotes.put(R.raw.b4, this.sp.load(pContext, R.raw.b4, 1));
        pianoNotes.put(R.raw.c4, this.sp.load(pContext, R.raw.c4, 1));
        pianoNotes.put(R.raw.c5, this.sp.load(pContext, R.raw.c5, 1));
        pianoNotes.put(R.raw.d4, this.sp.load(pContext, R.raw.d4, 1));
        pianoNotes.put(R.raw.e4, this.sp.load(pContext, R.raw.e4, 1));
        pianoNotes.put(R.raw.f4, this.sp.load(pContext, R.raw.f4, 1));
        pianoNotes.put(R.raw.g4, this.sp.load(pContext, R.raw.g4, 1));
    }

    public void playPianoSound(int pianoRes) {
        // Play the piano sound
        int pianoSoundId = (Integer) pianoNotes.get(pianoRes);
        this.sp.play(pianoSoundId, 1f, 1f, 0, 0, 1f);
    }

    public void release() {
        // Release SoundPool resources
        sp.release();
        sp = null;
    }
}