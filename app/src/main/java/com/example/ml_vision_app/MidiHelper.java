package com.example.ml_vision_app;

import org.billthefarmer.mididriver.MidiDriver;

public class MidiHelper {
    private final MidiDriver midiDriver;

    public MidiHelper() {
        midiDriver = MidiDriver.getInstance();
        midiDriver.start();
    }

    public void stopMidi() {
        if (midiDriver != null) {
            midiDriver.stop();
        }
    }

    public void sendMidi(int m, int n) {
        byte[] msg = new byte[2];
        msg[0] = (byte) m;
        msg[1] = (byte) n;
        midiDriver.write(msg);
    }

    public void sendMidi(int m, int n, int v) {
        byte[] msg = new byte[3];
        msg[0] = (byte) m;
        msg[1] = (byte) n;
        msg[2] = (byte) v;
        midiDriver.write(msg);
    }
}
