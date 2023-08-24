package com.jonasjuffinger.timelapse;

public class Util {
    public static String formatShutterSpeed(int n, int d)
    {
        if (n == 1 && d != 2 && d != 1)
            return String.format("%d/%d", n, d);
        else if (d == 1)
        {
            if (n == 65535)
                return "BULB";
            else
                return String.format("%d\"", n);
        }
        else
            return String.format("%.1f\"", (float) n / (float) d);
    }

    public static String formatShutterSpeedIndex(int index)
    {
        return formatShutterSpeed(SHUTTER_SPEEDS[index][0], SHUTTER_SPEEDS[index][1]);
    }


    public static final int[][] SHUTTER_SPEEDS = new int[][] {
            new int[]{1, 8000},
            new int[]{1, 6400},
            new int[]{1, 5000},
            new int[]{1, 4000},
            new int[]{1, 3200},
            new int[]{1, 2500},
            new int[]{1, 2000},
            new int[]{1, 1600},
            new int[]{1, 1250},
            new int[]{1, 1000},
            new int[]{1, 800},
            new int[]{1, 640},
            new int[]{1, 500},
            new int[]{1, 400},
            new int[]{1, 320},
            new int[]{1, 250},
            new int[]{1, 200},
            new int[]{1, 160},
            new int[]{1, 125},
            new int[]{1, 100},
            new int[]{1, 80},
            new int[]{1, 60},
            new int[]{1, 50},
            new int[]{1, 40},
            new int[]{1, 30},
            new int[]{1, 25},
            new int[]{1, 20},
            new int[]{1, 15},
            new int[]{1, 13},
            new int[]{1, 10},
            new int[]{1, 8},
            new int[]{1, 6},
            new int[]{1, 5},
            new int[]{1, 4},
            new int[]{1, 3},
            new int[]{10, 25},
            new int[]{1, 2},
            new int[]{10, 16},
            new int[]{4, 5},
            new int[]{1, 1},
            new int[]{13, 10},
            new int[]{16, 10},
            new int[]{2, 1},
            new int[]{25, 10},
            new int[]{16, 5},
            new int[]{4, 1},
            new int[]{5, 1},
            new int[]{6, 1},
            new int[]{8, 1},
            new int[]{10, 1},
            new int[]{13, 1},
            new int[]{15, 1},
            new int[]{20, 1},
            new int[]{25, 1},
            new int[]{30, 1},
    };


    public static final int[] ISO_VALUES = new int[]{
            50,
            64,
            80,
            100,
            125,
            160,
            200,
            250,
            320,
            400,
            500,
            640,
            800,
            1000,
            1250,
            1600,
            2000,
            2500,
            3200,
            4000,
            5000,
            6400,
            8000,
            10000,
            12800,
            16000,
            20000,
            25600,
            32000,
            40000,
            51200,
            64000,
            80000,
            102400
    };

    public static final float[] DEADBAND_VALUES = new float[]{
            0.0f,
            0.1f,
            0.2f,
            0.3f,
            0.4f,
            0.5f,
            0.6f,
            0.7f,
            0.8f,
            0.9f,
            1.0f,
    };
}
