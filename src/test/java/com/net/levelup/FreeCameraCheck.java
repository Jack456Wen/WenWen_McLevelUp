package com.net.levelup;

final class FreeCameraCheck {
    static void run() {
        double speed = 0;
        for (int tick = 0; tick < 7; tick++) speed = FreeCameraMotion.ramp(speed, 1);
        close(speed, 1, "full speed after seven ticks");
        close(FreeCameraMotion.ramp(0, 1), .15, "reference acceleration");
        close(FreeCameraMotion.ramp(1, 1), 1, "bounded acceleration");
        close(FreeCameraMotion.ramp(1, 0), .4, "reference release deceleration");
        close(FreeCameraMotion.ramp(FreeCameraMotion.ramp(1, 0), 0), .16, "continued release");
        close(FreeCameraMotion.ramp(1, -1), -.15, "reverse immediately without residual drift");
        close(FreeCameraMotion.ramp(-1, 1), .15, "reverse symmetrically");
        close(FreeCameraMotion.horizontal(0, 1, true), .125, "reference diagonal correction");
        close(FreeCameraMotion.ramp(.8, 1), -FreeCameraMotion.ramp(-.8, -1), "symmetric movement");
        System.out.println("Passed 9 free camera movement checks.");
    }

    private static void close(double actual, double expected, String message) {
        if (Math.abs(actual - expected) > 1e-12) throw new AssertionError(message + ": " + actual);
    }
}
