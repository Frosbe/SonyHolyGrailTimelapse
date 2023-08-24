package com.jonasjuffinger.timelapse;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import java.util.concurrent.atomic.AtomicInteger;

import static android.preference.PreferenceManager.getDefaultSharedPreferences;

/**
 * Created by jonas on 2/18/17.
 */

class Settings {
    private static final String EXTRA_INTERVAL = "com.jonasjuffinger.timelapse.INTERVAL";
    private static final String EXTRA_SHOTCOUNT = "com.jonasjuffinger.timelapse.SHOTCOUNT";
    private static final String EXTRA_DELAY = "com.jonasjuffinger.timelapse.DELAY";
    private static final String EXTRA_DISPLAYOFF = "com.jonasjuffinger.timelapse.DISPLAYOFF";
    private static final String EXTRA_SILENTSHUTTER = "com.jonasjuffinger.timelapse.SILENTSHUTTER";
    private static final String EXTRA_AEL = "com.jonasjuffinger.timelapse.AEL";
    private static final String EXTRA_BRS = "com.jonasjuffinger.timelapse.BRS";
    private static final String EXTRA_MF = "com.jonasjuffinger.timelapse.MF";
    private static final String EXTRA_HOLYGRAIL = "com.jonasjuffinger.timelapse.HOLYGRAIL";
    private static final String EXTRA_USECURRENTEXPOSURE = "com.jonasjuffinger.timelapse.USECURRENTEXPOSURE";
    private static final String EXTRA_TARGETEXPOSURE = "com.jonasjuffinger.timelapse.TARGETEXPOSURE";
    private static final String EXTRA_MAXSHUTTERSPEEDINDEX = "com.jonasjuffinger.timelapse.MAXSHUTTERSPEEDINDEX";
    private static final String EXTRA_MAXISOINDEX = "com.jonasjuffinger.timelapse.MAXISOINDEX";
    private static final String EXTRA_COOLDOWN = "com.jonasjuffinger.timelapse.COOLDOWN";
    private static final String EXTRA_AVERAGEEXPOSUREAMOUNT = "com.jonasjuffinger.timelapse.AVERAGEEXPOSUREAMOUNT";
    private static final String EXTRA_DEADBANDINDEX = "com.jonasjuffinger.timelapse.DEADBANDINDEX";
    private static final String EXTRA_HOLYGRAILALLOWEXPOSUREUP = "com.jonasjuffinger.timelapse.HOLYGRAILALLOWEXPOSUREUP";
    private static final String EXTRA_HOLYGRAILALLOWEXPOSUREDOWN = "com.jonasjuffinger.timelapse.HOLYGRAILALLOWEXPOSUREDOWN";


    double interval;
    int delay;
    int rawInterval, rawDelay;
    int shotCount, rawShotCount;
    boolean displayOff;
    boolean silentShutter;
    boolean ael;
    int fps;    // index
    boolean brs;
    boolean mf;
    boolean holyGrail;
    boolean useCurrentExposure;
    int targetExposure;
    int maxShutterSpeedIndex;
    int maxISOIndex;
    int cooldown;
    int averageExposureAmount;
    int deadbandIndex;
    boolean holyGrailAllowExposureUp;
    boolean holyGrailAllowExposureDown;


    Settings() {
        interval = 1;
        rawInterval = 1;
        delay = 0;
        rawDelay = 0;
        shotCount = 1;
        rawShotCount = 1;
        displayOff = false;
        silentShutter = true;
        ael = true;
        fps = 0;
        brs = true;
        mf = true;
        holyGrail = true;
        useCurrentExposure = true;
        targetExposure = 0; //0
        maxShutterSpeedIndex = 0;
        maxISOIndex = 0;
        cooldown = 5;
        averageExposureAmount = 1;
        deadbandIndex = 0;
        holyGrailAllowExposureUp = true;
        holyGrailAllowExposureDown = false;
    }

    public Settings(double interval, int shotCount, int delay, boolean displayOff, boolean silentShutter, boolean ael, boolean brs, boolean mf, boolean holyGrail, boolean useCurrentExposure, int targetExposure, int maxShutterSpeed, int maxISO, int cooldown, int averageExposureAmount, int deadbandIndex, boolean holyGrailAllowExposureUp, boolean holyGrailAllowExposureDown) {
        this.interval = interval;
        this.delay = delay;
        this.shotCount = shotCount;
        this.displayOff = displayOff;
        this.silentShutter = silentShutter;
        this.ael = ael;
        this.brs = brs;
        this.mf = mf;
        this.holyGrail = holyGrail;
        this.useCurrentExposure = useCurrentExposure;
        this.targetExposure = targetExposure;
        this.maxShutterSpeedIndex = maxShutterSpeed;
        this.maxISOIndex = maxISO;
        this.cooldown = cooldown;
        this.averageExposureAmount = averageExposureAmount;
        this.deadbandIndex = deadbandIndex;
        this.holyGrailAllowExposureUp = holyGrailAllowExposureUp;
        this.holyGrailAllowExposureDown = holyGrailAllowExposureDown;
    }

    void putInIntent(Intent intent) {
        intent.putExtra(EXTRA_INTERVAL, interval);
        intent.putExtra(EXTRA_SHOTCOUNT, shotCount);
        intent.putExtra(EXTRA_DELAY, delay);
        intent.putExtra(EXTRA_DISPLAYOFF, displayOff);
        intent.putExtra(EXTRA_SILENTSHUTTER, silentShutter);
        intent.putExtra(EXTRA_AEL, ael);
        intent.putExtra(EXTRA_BRS, brs);
        intent.putExtra(EXTRA_MF, mf);
        intent.putExtra(EXTRA_HOLYGRAIL, holyGrail);
        intent.putExtra(EXTRA_USECURRENTEXPOSURE, useCurrentExposure);
        intent.putExtra(EXTRA_TARGETEXPOSURE, targetExposure);
        intent.putExtra(EXTRA_MAXSHUTTERSPEEDINDEX, maxShutterSpeedIndex);
        intent.putExtra(EXTRA_MAXISOINDEX, maxISOIndex);
        intent.putExtra(EXTRA_COOLDOWN, cooldown);
        intent.putExtra(EXTRA_AVERAGEEXPOSUREAMOUNT, averageExposureAmount);
        intent.putExtra(EXTRA_DEADBANDINDEX, deadbandIndex);
        intent.putExtra(EXTRA_HOLYGRAILALLOWEXPOSUREUP, holyGrailAllowExposureUp);
        intent.putExtra(EXTRA_HOLYGRAILALLOWEXPOSUREDOWN, holyGrailAllowExposureDown);

    }

    static Settings getFromIntent(Intent intent) {
        return new Settings(
                intent.getDoubleExtra(EXTRA_INTERVAL, 1),
                intent.getIntExtra(EXTRA_SHOTCOUNT, 1),
                intent.getIntExtra(EXTRA_DELAY, 1),
                intent.getBooleanExtra(EXTRA_DISPLAYOFF, false),
                intent.getBooleanExtra(EXTRA_SILENTSHUTTER, true),
                intent.getBooleanExtra(EXTRA_AEL, false),
                intent.getBooleanExtra(EXTRA_BRS, false),
                intent.getBooleanExtra(EXTRA_MF, true),
                intent.getBooleanExtra(EXTRA_HOLYGRAIL, true),
                intent.getBooleanExtra(EXTRA_USECURRENTEXPOSURE, true),
                intent.getIntExtra(EXTRA_TARGETEXPOSURE, 0),
                intent.getIntExtra(EXTRA_MAXSHUTTERSPEEDINDEX, 0),
                intent.getIntExtra(EXTRA_MAXISOINDEX, 0),
                intent.getIntExtra(EXTRA_COOLDOWN, 5),
                intent.getIntExtra(EXTRA_AVERAGEEXPOSUREAMOUNT, 1),
                intent.getIntExtra(EXTRA_DEADBANDINDEX, 0),
                intent.getBooleanExtra(EXTRA_HOLYGRAILALLOWEXPOSUREUP, true),
                intent.getBooleanExtra(EXTRA_HOLYGRAILALLOWEXPOSUREDOWN, false)
        );
    }

    void save(Context context)
    {
        SharedPreferences sharedPref = getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt("interval", rawInterval);
        editor.putInt("shotCount", rawShotCount);
        editor.putInt("delay", rawDelay);
        editor.putBoolean("silentShutter", silentShutter);
        editor.putBoolean("ael", ael);
        editor.putInt("fps", fps);
        editor.putBoolean("brs", brs);
        editor.putBoolean("mf", mf);
        editor.putBoolean("displayOff", displayOff);
        editor.putBoolean("holyGrail", holyGrail);
        editor.putBoolean("useCurrentExposure", useCurrentExposure);
        editor.putInt("targetExposure", targetExposure);
        editor.putInt("maxShutterSpeedIndex", maxShutterSpeedIndex);
        editor.putInt("maxIsoIndex", maxISOIndex);
        editor.putInt("cooldown", cooldown);
        editor.putInt("averageExposureAmount", averageExposureAmount);
        editor.putInt("deadbandIndex", deadbandIndex);
        editor.putBoolean("holyGrailAllowExposureUp", holyGrailAllowExposureUp);
        editor.putBoolean("holyGrailAllowExposureDown", holyGrailAllowExposureDown);
        editor.apply();
    }

    void load(Context context)
    {
        SharedPreferences sharedPref = getDefaultSharedPreferences(context);
        rawInterval = sharedPref.getInt("interval", rawInterval);
        rawShotCount = sharedPref.getInt("shotCount", rawShotCount);
        rawDelay = sharedPref.getInt("delay", rawDelay);
        silentShutter = sharedPref.getBoolean("silentShutter", silentShutter);
        ael = sharedPref.getBoolean("ael", ael);
        fps = sharedPref.getInt("fps", fps);
        brs = sharedPref.getBoolean("brs", brs);
        mf = sharedPref.getBoolean("mf", mf);
        displayOff = sharedPref.getBoolean("displayOff", displayOff);
        holyGrail = sharedPref.getBoolean("holyGrail", holyGrail);
        useCurrentExposure = sharedPref.getBoolean("useCurrentExposure", useCurrentExposure);
        targetExposure = sharedPref.getInt("targetExposure", targetExposure);
        maxShutterSpeedIndex = sharedPref.getInt("maxShutterSpeedIndex", maxShutterSpeedIndex);
        maxISOIndex = sharedPref.getInt("maxIsoIndex", maxISOIndex);
        cooldown = sharedPref.getInt("cooldown", cooldown);
        averageExposureAmount = sharedPref.getInt("averageExposureAmount", averageExposureAmount);
        deadbandIndex = sharedPref.getInt("deadbandIndex", deadbandIndex);
        holyGrailAllowExposureUp = sharedPref.getBoolean("holyGrailAllowExposureUp", holyGrailAllowExposureUp);
        holyGrailAllowExposureDown = sharedPref.getBoolean("holyGrailAllowExposureDown", holyGrailAllowExposureDown);
    }

}
