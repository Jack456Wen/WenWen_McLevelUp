package com.net.levelup;

/**
 * Movement ramp adapted from Tweakeroo/Tweakerge MiscUtils and CameraEntity.
 * LGPL-3.0; see TWEAKEROO-NOTICE.txt and TWEAKEROO-LICENSE.txt.
 */
final class FreeCameraMotion {
    private FreeCameraMotion() { }

    static double ramp(double current, int input) {
        if (input == 0) return current * 0.4;
        if ((input < 0) != (current < 0)) current = 0;
        return Math.max(-1, Math.min(1, current + input * 0.15));
    }

    static double horizontal(double current, int input, boolean diagonal) {
        return ramp(current, input) / (diagonal ? 1.2 : 1);
    }
}
