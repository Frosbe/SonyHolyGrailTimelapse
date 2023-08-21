package com.jonasjuffinger.timelapse;

import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Camera;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Pair;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.github.ma1co.pmcademo.app.BaseActivity;

import com.sony.scalar.hardware.CameraEx;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ShootActivity extends BaseActivity implements SurfaceHolder.Callback, CameraEx.ShutterListener, CameraEx.ShutterSpeedChangeListener
{
    private Settings settings;

    private int shotCount;

    private TextView tvCount, tvBattery, tvRemaining, tvExposureLevel, tvLastPictureExposureLevel, tvTargetExposureLevel, tvLastPictureShutterspeed, tvShotsSinceLastChange;

    private SurfaceView reviewSurfaceView;
    private SurfaceHolder cameraSurfaceHolder;
    private CameraEx cameraEx;
    private Camera camera;
    private CameraEx.AutoPictureReviewControl autoReviewControl;
    private int pictureReviewTime;

    private boolean burstShooting;
    private boolean stopPicturePreview;
    private boolean takingPicture;

    private long shootTime;
    private long shootStartTime;

    private float exposureLevel = 0;
    private float lastPictureExposureLevel = 0;
    private String shutterSpeed = "";
    private String lastPictureShutterSpeed = "";
    private double shutterSpeedValue = 0;
    private int lastHolyGrailSettingUpdateShotCount = -5;

    private Display display;


    int getcnt(){
        if(settings.brs){
            return 3;
        }
        return 1;
    }

    private Handler shootRunnableHandler = new Handler();
    private final Runnable shootRunnable = new Runnable()
    {
        @Override
        public void run()
        {
            if(stopPicturePreview) {
                stopPicturePreview = false;
                camera.stopPreview();
                reviewSurfaceView.setVisibility(View.GONE);
                if(settings.displayOff)
                    display.off();
            }

            if(burstShooting) {
                shoot();
            }
            else if(shotCount < settings.shotCount * getcnt()) {
                long remainingTime = Math.round(shootTime + settings.interval * 1000 - System.currentTimeMillis());
                if(brck.get()>0){
                    remainingTime = -1;
                }

                log("  Remaining Time: " + Long.toString(remainingTime));

                if (remainingTime <= 150) { // 300ms is vaguely the time this postDelayed is to slow
                    brck.getAndDecrement();
                    shoot();
                    display.on();
                } else {
                    shootRunnableHandler.postDelayed(this, remainingTime-150);
                }
            }
            else {
                display.on();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(false) {
                            tvCount.setText("Thanks for using this app!");
                            tvBattery.setVisibility(View.INVISIBLE);
                            tvRemaining.setVisibility(View.INVISIBLE);
                        }
                        else {
                            onBackPressed();
                        }
                    }
                });
            }
        }
    };

    private Handler manualShutterCallbackCallRunnableHandler = new Handler();
    private final Runnable manualShutterCallbackCallRunnable = new Runnable() {
        @Override
        public void run() {
            onShutter(0, cameraEx);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        log("onCreate");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shoot);

        Intent intent = getIntent();
        settings = Settings.getFromIntent(intent);

        shotCount = 0;
        takingPicture = false;
        burstShooting = settings.interval == 0;

        tvCount = (TextView) findViewById(R.id.tvCount);
        tvBattery = (TextView) findViewById(R.id.tvBattery);
        tvRemaining = (TextView) findViewById(R.id.tvRemaining);
        tvLastPictureExposureLevel = (TextView) findViewById(R.id.tvLastPictureExposure);
        tvTargetExposureLevel = (TextView) findViewById(R.id.tvTargetExposureLevel);
        tvLastPictureShutterspeed = (TextView) findViewById(R.id.tvLastPictureShutterspeed);
        tvExposureLevel = (TextView) findViewById(R.id.tvExposureLevel);
        tvShotsSinceLastChange = (TextView) findViewById(R.id.tvShotsSinceLastChange);

        reviewSurfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        reviewSurfaceView.setZOrderOnTop(false);
        cameraSurfaceHolder = reviewSurfaceView.getHolder();
        cameraSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }


    @Override
    protected void onResume() {
        log("onResume");

        super.onResume();
        cameraEx = CameraEx.open(0, null);
        cameraEx.setShutterListener(this);
        cameraSurfaceHolder.addCallback(this);
        autoReviewControl = new CameraEx.AutoPictureReviewControl();
        //autoReviewControl.setPictureReviewInfoHist(true);
        cameraEx.setAutoPictureReviewControl(autoReviewControl);

        final Camera.Parameters params = cameraEx.getNormalCamera().getParameters();

        try {
            if(settings.mf)
                params.setFocusMode(CameraEx.ParametersModifier.FOCUS_MODE_MANUAL);
            else
                params.setFocusMode("auto");
        }
        catch(Exception ignored)
        {}


        final CameraEx.ParametersModifier modifier = cameraEx.createParametersModifier(params);

        if(burstShooting) {
            try {
                modifier.setDriveMode(CameraEx.ParametersModifier.DRIVE_MODE_BURST);
                List driveSpeeds = modifier.getSupportedBurstDriveSpeeds();
                modifier.setBurstDriveSpeed(driveSpeeds.get(driveSpeeds.size() - 1).toString());
                modifier.setBurstDriveButtonReleaseBehave(CameraEx.ParametersModifier.BURST_DRIVE_BUTTON_RELEASE_BEHAVE_CONTINUE);
            } catch (Exception ignored) {
            }
        }
        else {
            modifier.setDriveMode(CameraEx.ParametersModifier.DRIVE_MODE_SINGLE);
        }

        // setSilentShutterMode doesn't exist on all cameras
        try {
            modifier.setSilentShutterMode(settings.silentShutter);
        }
        catch(NoSuchMethodError ignored)
        {}

        try{
            //add also AEL if set
            if(settings.ael) {
                modifier.setAutoExposureLock(CameraEx.ParametersModifier.AE_LOCK_ON);
            }
        }
        catch (Exception e){
            //do nothing
        }

        if(settings.brs){
            try{
                modifier.setDriveMode(CameraEx.ParametersModifier.DRIVE_MODE_BRACKET);
                modifier.setBracketMode(CameraEx.ParametersModifier.BRACKET_MODE_EXPOSURE);
                modifier.setExposureBracketMode(CameraEx.ParametersModifier.EXPOSURE_BRACKET_MODE_SINGLE);
                modifier.setExposureBracketPeriod(30);
                modifier.setNumOfBracketPicture(3);
            }
            catch (Exception e){
                //do nothing
            }
        }

        cameraEx.getNormalCamera().setParameters(params);

        pictureReviewTime = 2; //autoReviewControl.getPictureReviewTime();
        //log(Integer.toString(pictureReviewTime));


        shotCount = 0;
        shootRunnableHandler.postDelayed(shootRunnable, (long) settings.delay * 1000 * 60);
        shootStartTime = System.currentTimeMillis() + settings.delay * 1000 * 60;

        if(burstShooting) {
            manualShutterCallbackCallRunnableHandler.postDelayed(manualShutterCallbackCallRunnable, 500);
        }

        display = new Display(getDisplayManager());

        if(settings.displayOff) {
            display.turnAutoOff(5000);
        }

        setAutoPowerOffMode(false);

        // Exposure metering
        cameraEx.setProgramLineRangeOverListener(new CameraEx.ProgramLineRangeOverListener()
        {
            @Override
            public void onAERange(boolean b, boolean b1, boolean b2, CameraEx cameraEx)
            {
                //log(String.format("onARRange b %b b1 %b b2 %b\n", Boolean.valueOf(b), Boolean.valueOf(b1), Boolean.valueOf(b2)));
            }

            @Override
            public void onEVRange(int ev, CameraEx cameraEx)
            {
                exposureLevel = (float)ev / 3.0f;
                tvExposureLevel.setText(String.format("%.1f", exposureLevel));
                //log(String.format("onEVRange i %d %f\n", ev, (float)ev / 3.0f));
            }

            @Override
            public void onMeteringRange(boolean b, CameraEx cameraEx)
            {
                //log(String.format("onMeteringRange b %b\n", Boolean.valueOf(b)));
            }
        });

        updateShutterSpeed();

        lastPictureExposureLevel = exposureLevel;
        lastPictureShutterSpeed = shutterSpeed;
        updateScreen();
    }

    private void updateShutterSpeed(){
        final Camera.Parameters params = cameraEx.getNormalCamera().getParameters();
        final CameraEx.ParametersModifier paramsModifier = cameraEx.createParametersModifier(params);
        Pair<Integer, Integer> sp = paramsModifier.getShutterSpeed();
        shutterSpeed = String.format("%d/%d", sp.first, sp.second);
        shutterSpeedValue = (float)sp.first / (float)sp.second;
    }

    private void updateScreen(){
        tvCount.setText(Integer.toString(shotCount)+"/"+Integer.toString(settings.shotCount * getcnt()));
        tvRemaining.setText(getRemainingTime());
        tvBattery.setText(getBatteryPercentage());

        //Holy Grail
        tvLastPictureExposureLevel.setText(String.format("%.1f", lastPictureExposureLevel));
        tvLastPictureShutterspeed.setText(lastPictureShutterSpeed);
        tvTargetExposureLevel.setText(String.format("%.2f", (settings.targetExposure) / 3.0f));
        tvShotsSinceLastChange.setText(Integer.toString(shotCount - lastHolyGrailSettingUpdateShotCount));

    }

    @Override
    protected boolean onMenuKeyUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onPause() {
        display.on();
        display.turnAutoOff(Display.NO_AUTO_OFF);

        super.onPause();

        log("on pause");

        shootRunnableHandler.removeCallbacks(shootRunnable);

        if(cameraSurfaceHolder == null)
            log("cameraSurfaceHolder == null");
        else {
            cameraSurfaceHolder.removeCallback(this);
        }

        autoReviewControl = null;

        if(camera == null)
            log("camera == null");
        else {
            camera.stopPreview();
            camera = null;
        }

        if(cameraEx == null)
            log("cameraEx == null");
        else {
            cameraEx.setAutoPictureReviewControl(null);
            cameraEx.release();
            cameraEx = null;
        }

        setAutoPowerOffMode(true);
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            camera = cameraEx.getNormalCamera();
            camera.setPreviewDisplay(holder);
        }
        catch (IOException e) {}
    }

    @Override
    public void onShutterSpeedChange(CameraEx.ShutterSpeedInfo shutterSpeedInfo, CameraEx cameraEx)
    {
        int numerator = shutterSpeedInfo.currentShutterSpeed_n;
        int denominator = shutterSpeedInfo.currentShutterSpeed_d;
        final String formattedShutterSpeed = String.format("%d/%d", numerator, denominator);
        shutterSpeed = formattedShutterSpeed;
        log("shutter speed changed to " + formattedShutterSpeed);
        shutterSpeedValue = (float)numerator / (float)denominator;
    }

    private void shoot() {
        if(takingPicture)
            return;

        if (settings.holyGrail){
            holyGrailPrePhotoEvent();
        }

        shootTime = System.currentTimeMillis();
        cameraEx.burstableTakePicture();

        shotCount++;

        lastPictureExposureLevel = exposureLevel;
        lastPictureShutterSpeed = shutterSpeed;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateScreen();
            }
        });
    }

    private void holyGrailPrePhotoEvent(){

        float targetExposureLevel = (settings.targetExposure) / 3.0f;
        log("targetExposureLevel: " + targetExposureLevel);
        //if holy grail is enabled
        if (settings.holyGrail){
            log("holy grail enabled");
            //if it has been more than 5 shots since the last holy grail setting change
            if (shotCount - lastHolyGrailSettingUpdateShotCount >= 5){
                log("more than 5 shots since last holy grail setting change");
                //if we are allowed to exposure up
                if (settings.holyGrailAllowExposureUp){
                    log("holy grail allow exposure up");
                    //do we need to exposure up?
                    log("exposureLevel: " + exposureLevel);
                    log("targetExposureLevel - 0.7f: " + (targetExposureLevel - 0.7f));
                    if (exposureLevel < targetExposureLevel - 0.7f){
                        log("exposureLevel < targetExposureLevel - 0.7f");
                        //exposure up
                        //can we increase shutter speed?

                        log("shutterSpeedValue: " + shutterSpeedValue);
                        log("settings.MaxShutterSpeed: " + settings.maxShutterSpeed);
                        if (shutterSpeedValue < settings.maxShutterSpeed){
                            log("shutterSpeedValue < settings.MaxShutterSpeed");
                            //make the shutter open longer
                            cameraEx.decrementShutterSpeed();
                            lastHolyGrailSettingUpdateShotCount = shotCount;
                        }
                        else{

                            //increase ISO
                            incrementISO();
                            lastHolyGrailSettingUpdateShotCount = shotCount;
                        }
                    }
                }

                //if we are allowed to exposure down
                if (settings.holyGrailAllowExposureDown){
                    log("holy grail allow exposure down");
                    //do we need to exposure down?
                    log("exposureLevel: " + exposureLevel);
                    log("targetExposureLevel + 0.7f: " + (targetExposureLevel + 0.7f));
                    if (exposureLevel > targetExposureLevel + 0.7f){
                        log("exposureLevel > targetExposureLevel + 0.7f");
                        //exposure down
                        //can we decrease shutter speed?
                        //get the minimum shutter speed value

                        if (shutterSpeedValue > 1/8000){
                            log("shutterSpeedValue > 1/8000");
                            //make the shutter open shorter
                            cameraEx.incrementShutterSpeed();
                            lastHolyGrailSettingUpdateShotCount = shotCount;
                        }
                        else{
                            //decrease ISO
                            decrementISO();
                            lastHolyGrailSettingUpdateShotCount = shotCount;
                        }
                    }
                }
            }
        }
        updateShutterSpeed();
    }

    private void incrementISO(){
        log("incrementISO");
        Camera.Parameters params = cameraEx.createEmptyParameters();

        //get the list of supported ISO values list of ints
        List<String> supportedISOs = cameraEx.createParametersModifier(params).getSupportedISOSensitivities();

        //get the current ISO value
        int currentISO = cameraEx.createParametersModifier(params).getISOSensitivity();

        //find the index of the current ISO value
        int currentISOIndex = supportedISOs.indexOf(Integer.toString(currentISO));

        //if the current ISO value is not the last in the list
        if (currentISOIndex < supportedISOs.size() - 1){
            //set the ISO value to the next one in the list, parse it to an int
            cameraEx.createParametersModifier(params).setISOSensitivity(Integer.parseInt(supportedISOs.get(currentISOIndex + 1)));
            //set the camera parameters
            cameraEx.getNormalCamera().setParameters(params);
        }
    }

    private void decrementISO(){
        log("decrementISO");
        Camera.Parameters params = cameraEx.createEmptyParameters();

        //get the list of supported ISO values list of ints
        List<String> supportedISOs = cameraEx.createParametersModifier(params).getSupportedISOSensitivities();

        //get the current ISO value
        int currentISO = cameraEx.createParametersModifier(params).getISOSensitivity();

        //find the index of the current ISO value
        int currentISOIndex = supportedISOs.indexOf(Integer.toString(currentISO));

        //if the current ISO value is not the first in the list
        if (currentISOIndex > 0){
            //set the ISO value to the previous one in the list, parse it to an int
            cameraEx.createParametersModifier(params).setISOSensitivity(Integer.parseInt(supportedISOs.get(currentISOIndex - 1)));
            //set the camera parameters
            cameraEx.getNormalCamera().setParameters(params);
        }
    }

    private AtomicInteger brck = new AtomicInteger(0);


    // When burst shooting this method is not called automatically
    // Therefore we called it every second manually
    @Override
    public void onShutter(int i, CameraEx cameraEx) {

        if(brck.get()<0){
            brck = new AtomicInteger(0);
            if(getcnt()>1) {
                brck = new AtomicInteger(2);
            }
        }

        if(burstShooting) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateScreen();
                }
            });

            // just keep shooting until we have all shots
            if (System.currentTimeMillis() >= shootStartTime + settings.shotCount * 1000) {
                this.cameraEx.cancelTakePicture();
                stopPicturePreview = true;
                display.on();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(false) {
                            tvCount.setText("Thanks for using this app!");
                            tvBattery.setVisibility(View.INVISIBLE);
                            tvRemaining.setVisibility(View.INVISIBLE);
                            tvExposureLevel.setVisibility(View.INVISIBLE);
                        }
                        else {
                            onBackPressed();
                        }
                    }
                });
            }
            else {
                manualShutterCallbackCallRunnableHandler.postDelayed(manualShutterCallbackCallRunnable, 500);
            }
        }
        else {
            this.cameraEx.cancelTakePicture();

            //camera.startPreview();

            if (shotCount < settings.shotCount * getcnt()) {

                // remaining time to the next shot
                double remainingTime = shootTime + settings.interval * 1000 - System.currentTimeMillis();
                if (brck.get() > 0) {
                    remainingTime = -1;
                }

                log("Remaining Time: " + remainingTime);

                // if the remaining time is negative immediately take the next picture
                if (remainingTime < 0) {
                    stopPicturePreview = false;
                    shootRunnableHandler.post(shootRunnable);
                }
                // show the preview picture for some time
                else {
                    long previewPictureShowTime = Math.round(Math.min(remainingTime, pictureReviewTime * 1000));
                    log("  Stop preview in: " + previewPictureShowTime);
                    reviewSurfaceView.setVisibility(View.VISIBLE);
                    stopPicturePreview = true;
                    shootRunnableHandler.postDelayed(shootRunnable, previewPictureShowTime);
                }
            } else {
                stopPicturePreview = true;
                shootRunnableHandler.postDelayed(shootRunnable, pictureReviewTime * 1000);
            }
        }
    }

    private String getBatteryPercentage()
    {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = registerReceiver(null, ifilter);

        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        int chargePlug = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL ||
                chargePlug == BatteryManager.BATTERY_PLUGGED_USB ||
                chargePlug == BatteryManager.BATTERY_PLUGGED_AC;

        String s = "";
        if(isCharging)
            s = "c ";

        return s + (int)(level / (float)scale * 100) + "%";
    }

    private String getRemainingTime() {
        if(burstShooting)
            return "" + Math.round((settings.shotCount * 1000 - System.currentTimeMillis() + shootStartTime) / 1000) + "s";
        else
            return "" + Math.round((settings.shotCount * getcnt() - shotCount) * settings.interval / 60) + "min";
    }

    private String getExposureLevel() {
        CameraEx.ExposureInfo exposureInfo = new CameraEx.ExposureInfo();
        CameraEx.ExifInfo exifInfo = new CameraEx.ExifInfo();

        return "1: " + exposureInfo.FNo + " 2: " + exposureInfo.IsoSpeedRate + " 3: " + exposureInfo.ShutterSpeedDeanom + " 4: " + exposureInfo.ShutterSpeedNumber + " 5: " + exifInfo.fNumberNumer;
    }



    @Override
    protected void onAnyKeyDown() {
        display.on();
    }


    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {}

    @Override
    protected void setColorDepth(boolean highQuality)
    {
        super.setColorDepth(false);
    }


    private void log(String s) {
        Logger.info(s);
    }

    private void dumpList(List list, String name) {
        log(name);
        log(": ");
        if (list != null)
        {
            for (Object o : list)
            {
                log(o.toString());
                log(" ");
            }
        }
        else
            log("null");
        log("\n");
    }
}
