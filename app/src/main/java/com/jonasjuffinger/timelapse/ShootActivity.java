package com.jonasjuffinger.timelapse;

import static com.jonasjuffinger.timelapse.Util.ISO_VALUES;
import static com.jonasjuffinger.timelapse.Util.SHUTTER_SPEEDS;
import static com.jonasjuffinger.timelapse.Util.formatShutterSpeed;
import static com.jonasjuffinger.timelapse.Util.formatShutterSpeedIndex;

import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
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

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.LineGraphView;
import com.sony.scalar.hardware.CameraEx;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ShootActivity extends BaseActivity implements SurfaceHolder.Callback, CameraEx.ShutterListener, CameraEx.ShutterSpeedChangeListener
{
    private Settings settings;

    private int shotCount;

    private TextView tvCount, tvBattery, tvRemaining;

    //Holy Grail
    private TextView tvExposureLevel;
    private TextView tvAverageExposure;
    private TextView tvTargetExposureLevel;
    private TextView tvShutterspeed;
    private TextView tvMaxShutterspeed;
    private TextView tvLastPictureISO;
    private TextView tvMaxISO;
    private TextView tvShotsSinceLastChange;
    private TextView tvCooldown;
    //End Holy Grail

    private List<Float> exposureLevelList = new ArrayList<Float>();

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
    private int ISOLevelIndex = 0;
    private int shutterSpeedIndex = 0;
    private int shotsSinceLastChange = 0;
    private float targetExposureLevel = 0.0f;

    private Display display;

    GraphViewSeries exposureLevelSeries = new GraphViewSeries(new GraphView.GraphViewData[]{}); // init data
    //GraphViewSeries targetExposureLevelSeries = new GraphViewSeries(new GraphView.GraphViewData[]{}); // init data
    //GraphViewSeries shutterSpeedSeries = new GraphViewSeries(new GraphView.GraphViewData[]{}); // init data
    //GraphViewSeries ISOSeries = new GraphViewSeries(new GraphView.GraphViewData[]{}); // init data


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

        //Holy Grail
        tvExposureLevel = (TextView) findViewById(R.id.tvExposureLevel);
        tvAverageExposure = (TextView) findViewById(R.id.tvAverageExposure);
        tvTargetExposureLevel = (TextView) findViewById(R.id.tvTargetExposureLevel);
        tvShutterspeed = (TextView) findViewById(R.id.tvShutterspeed);
        tvMaxShutterspeed = (TextView) findViewById(R.id.tvMaxShutterspeed);
        tvLastPictureISO = (TextView) findViewById(R.id.tvLastPictureISO);
        tvMaxISO = (TextView) findViewById(R.id.tvMaxISO);
        tvShotsSinceLastChange = (TextView) findViewById(R.id.tvShotsSinceLastChange);
        tvCooldown = (TextView) findViewById(R.id.tvCooldown);
        //end Holy Grail

        if (settings.useCurrentExposure){
            targetExposureLevel = exposureLevel;
        } else {
            targetExposureLevel = settings.targetExposure;
        }

        if (settings.holyGrailAllowExposureUp && !settings.holyGrailAllowExposureDown){
            ISOLevelIndex = 0;
        }

        if (settings.holyGrailAllowExposureDown && !settings.holyGrailAllowExposureUp){
            ISOLevelIndex = ISO_VALUES.length - 1;
        }

        if (settings.holyGrailAllowExposureDown && settings.holyGrailAllowExposureUp){
            ISOLevelIndex = ISO_VALUES.length / 2;
        }

        reviewSurfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        reviewSurfaceView.setZOrderOnTop(false);
        cameraSurfaceHolder = reviewSurfaceView.getHolder();
        cameraSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        GraphView graphView = new LineGraphView(
                this // context
                , "ExposureLevel" // heading
        );



        exposureLevelSeries.getStyle().color = Color.RED;
        graphView.addSeries(exposureLevelSeries); // data
        //graphView.addSeries(targetExposureLevelSeries); // data
        //graphView.addSeries(shutterSpeedSeries); // data
        //graphView.addSeries(ISOSeries); // data

        LinearLayout layout = (LinearLayout) findViewById(R.id.graphLayout);
        //layout.addView(graphView);
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

        /*log("reset graph data");
        exposureLevelSeries.resetData(new GraphView.GraphViewData[]{});
        targetExposureLevelSeries.resetData(new GraphView.GraphViewData[]{});
        shutterSpeedSeries.resetData(new GraphView.GraphViewData[]{});*/

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
                if (settings.useCurrentExposure){
                    targetExposureLevel = exposureLevel;
                    settings.useCurrentExposure = false;
                    updateScreen();
                }
                tvExposureLevel.setText(String.format("%.2f", exposureLevel));
                //log(String.format("onEVRange i %d %f\n", ev, (float)ev / 3.0f));
            }

            @Override
            public void onMeteringRange(boolean b, CameraEx cameraEx)
            {
                //log(String.format("onMeteringRange b %b\n", Boolean.valueOf(b)));
            }
        });

        updateShutterSpeed();
        updateISOLevel();
        updateScreen();
    }

    private void updateShutterSpeed(){
        log("updateShutterSpeed");
        final Camera.Parameters params = cameraEx.getNormalCamera().getParameters();
        final CameraEx.ParametersModifier paramsModifier = cameraEx.createParametersModifier(params);
        Pair<Integer, Integer> sp = paramsModifier.getShutterSpeed();

        //find the shutterspeed in the list (list of int[][])
        for(int i = 0; i < SHUTTER_SPEEDS.length; i++){
            if(SHUTTER_SPEEDS[i][0] == sp.first && SHUTTER_SPEEDS[i][1] == sp.second){
                shutterSpeedIndex = i;
                break;
            }
        }
        log("shutterSpeedIndex: " + shutterSpeedIndex);
    }

    private void updateISOLevel(){
        log("updateISOLevel");
        final Camera.Parameters params = cameraEx.getNormalCamera().getParameters();
        final CameraEx.ParametersModifier paramsModifier = cameraEx.createParametersModifier(params);
        int iso = paramsModifier.getISOSensitivity();
        for(int i = 0; i < ISO_VALUES.length; i++){
            if(ISO_VALUES[i] == iso){
                ISOLevelIndex = i;
                break;
            }
        }
        log("ISOLevelIndex: " + ISOLevelIndex);
    }

    private void updateScreen(){
        tvCount.setText(Integer.toString(shotCount)+"/"+Integer.toString(settings.shotCount * getcnt()));
        tvRemaining.setText(getRemainingTime());
        tvBattery.setText(getBatteryPercentage());

        //Holy Grail
        tvAverageExposure.setText(String.format("%.2f", (getAverageExposure(settings.averageExposureAmount))));
        tvTargetExposureLevel.setText(String.format("%.2f", targetExposureLevel));
        tvShutterspeed.setText(formatShutterSpeedIndex(shutterSpeedIndex));
        tvMaxShutterspeed.setText(formatShutterSpeedIndex(settings.maxShutterSpeedIndex));
        tvLastPictureISO.setText(Integer.toString(ISO_VALUES[ISOLevelIndex]));
        tvMaxISO.setText(Integer.toString(ISO_VALUES[settings.maxISOIndex]));
        tvShotsSinceLastChange.setText(Integer.toString(shotsSinceLastChange));
        tvCooldown.setText(Integer.toString(settings.cooldown));


    }

    private float getAverageExposure(int shotCount){
        //get the average value of the last "shotCount" values in the exposureLevelList, or list.size() if list.size() < shotCount
        float sum = 0;
        int count = 0;
        for(int i = exposureLevelList.size() - 1; i >= 0 && count < shotCount; i--){
            sum += exposureLevelList.get(i);
            count++;
        }
        if (count == 0){
            return 0;
        } else {
            return sum / count;
        }
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
        //find the shutterspeed in the list (list of int[][])
        int shutterSpeedIndex = 0;
        for(int i = 0; i < SHUTTER_SPEEDS.length; i++){
            if(SHUTTER_SPEEDS[i][0] == numerator && SHUTTER_SPEEDS[i][1] == denominator){
                shutterSpeedIndex = i;
                break;
            }
        }
    }



    private void shoot() {
        if(takingPicture)
            return;

        if (settings.holyGrail){
            addPointsToGraph();
            holyGrailPrePhotoEvent();
        }

        shootTime = System.currentTimeMillis();
        cameraEx.burstableTakePicture();

        shotCount++;
        shotsSinceLastChange++;

        exposureLevelList.add(exposureLevel);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateScreen();
            }
        });
    }

    private void addPointsToGraph(){
        log("addPointsToGraph" + shotCount + ", exposureLevel: " + exposureLevel + ", target:" + targetExposureLevel);
        exposureLevelSeries.appendData(new GraphView.GraphViewData(shotCount, exposureLevel+10), false);
        //targetExposureLevelSeries.appendData(new GraphView.GraphViewData(shotCount, targetExposureLevel), false);
        //shutterSpeedSeries.appendData(new GraphView.GraphViewData(shotCount, shutterSpeedValue), false);
    }

    private void holyGrailPrePhotoEvent(){
        float usableExposureLevel = getAverageExposure(settings.averageExposureAmount);
        int cooldown = settings.cooldown;
        float deadbandSize = 0.5f;


        if (settings.holyGrail){

            if (shotsSinceLastChange >= cooldown){

                if (usableExposureLevel < targetExposureLevel - deadbandSize){

                    if (settings.holyGrailAllowExposureUp) {
                        log("trying to get higher exposure");

                        if (shutterSpeedIndex < settings.maxShutterSpeedIndex){
                            log("shutter speed is less than max shutter speed");
                            //make the shutter open longer
                            cameraEx.decrementShutterSpeed();
                            shotsSinceLastChange = 0;
                        } else if (ISOLevelIndex < settings.maxISOIndex){
                            log("ISO is less than max ISO");
                            //increase ISO
                            incrementISO();
                        } else {
                            log("can't increase shutter speed or ISO");
                        }
                    }
                }

                if (usableExposureLevel > targetExposureLevel + deadbandSize){

                    if (settings.holyGrailAllowExposureDown) {
                        if (ISOLevelIndex > 0){
                            //decrease ISO
                            decrementISO();
                        } else if (shutterSpeedIndex > 0){
                            //make the shutter open shorter
                            cameraEx.incrementShutterSpeed();
                            shotsSinceLastChange = 0;
                        } else {
                            log("can't decrease shutter speed or ISO");
                        }
                    }
                }
            }
        }
        updateShutterSpeed();
        updateISOLevel();
        updateScreen();
    }

    private void incrementISO(){
        log("incrementISO");
        ISOLevelIndex++;
        if (ISOLevelIndex < ISO_VALUES.length){
            Camera.Parameters params = cameraEx.createEmptyParameters();
            cameraEx.createParametersModifier(params).setISOSensitivity(ISO_VALUES[ISOLevelIndex]);
            cameraEx.getNormalCamera().setParameters(params);
        } else {
            log("can't increase ISO");
        }
    }

    private void decrementISO(){
        log("decrementISO");
        ISOLevelIndex--;
        if (ISOLevelIndex >= 0){
            Camera.Parameters params = cameraEx.createEmptyParameters();
            cameraEx.createParametersModifier(params).setISOSensitivity(ISO_VALUES[ISOLevelIndex]);
            cameraEx.getNormalCamera().setParameters(params);
        } else {
            log("can't decrease ISO");
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
                    //log("  Stop preview in: " + previewPictureShowTime);
                    reviewSurfaceView.setVisibility(View.VISIBLE);
                    stopPicturePreview = true;
                    shootRunnableHandler.postDelayed(shootRunnable, previewPictureShowTime);
                }
            } else {
                //stopPicturePreview = true;
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
        //add timestamp
        s = new SimpleDateFormat("HH:mm:ss.SSS").format(new Date()) + " " + s;
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
