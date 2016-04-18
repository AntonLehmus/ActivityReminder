package fi.antonlehmus.activityreminder;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.util.Calendar;

import static com.google.android.gms.wearable.PutDataRequest.WEAR_URI_SCHEME;

public class StepReaderService extends WearableListenerService
        implements SensorEventListener,  GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        ResultCallback<DataApi.DeleteDataItemsResult> {

    private final int DEBUG_INTERVAL = 30;

    public static final int REQUEST_CODE = 931;

    private final String LOG_TAG ="StepReaderService";
    private final String SAVED_STEPS ="saved_steps";
    private final String PERSISTENT_STEPS = "persistent_steps" ;
    private final String CYCLE_START_TIME = "cycle_start_time" ;
    private final float DEFAULT_STEP_COUNT_TRIGGER = 15;
    private final int DEFAULT_INTERVAL = 15                                                                                                                                                                                     ; //15-20min vaikutti hyvältä
    private final long DEFAULT_CYCLE_TIME = 3600000; //60min in milliseconds
    private final int DEFAULT_SILENT_START = 20;
    private final int DEFAULT_SILENT_STOP = 8;

    private static final String REBOOT_KEY = "resume_on_reboot";
    private static final String SILENT_START_KEY = "silent_start";
    private static final String SILENT_STOP_KEY = "silent_stop";
    private static final String STEP_TRIGGER_KEY = "steps_trigger";
    private static final String REMIND_INTERVAL_KEY = "remind_interval";



    private float steps = 0;
    private float oldSteps = 0;
    private long time_since_cycle_start = 0;
    private long last_cycle_time = 0;

    private AlarmManager scheduler = null;
    private Intent intentHelper = null;
    private static PendingIntent scheduledIntent = null;


    private SensorManager sensorManager = null;
    private Sensor sensor = null;
    private Calendar calendarSilentStart = null;
    private Calendar calendarSilentStop = null;
    private GoogleApiClient mGoogleApiClient;

    private static boolean resumeOnReboot;
    private static long silent_start;
    private static long silent_stop ;
    private static int remind_interval;
    private static int step_trigger;



    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        //read steps from last time and cycle running time
        SharedPreferences sharedPref =  getSharedPreferences(PERSISTENT_STEPS, Context.MODE_PRIVATE);
        oldSteps = sharedPref.getFloat(SAVED_STEPS,0);
        last_cycle_time = sharedPref.getLong(CYCLE_START_TIME, 0);
        time_since_cycle_start = System.currentTimeMillis()-last_cycle_time;

        calendarSilentStart = Calendar.getInstance();
        calendarSilentStart.set(Calendar.HOUR_OF_DAY, DEFAULT_SILENT_START);
        calendarSilentStart.set(Calendar.MINUTE,0);

        calendarSilentStop = Calendar.getInstance();
        calendarSilentStop.set(Calendar.HOUR_OF_DAY, DEFAULT_SILENT_STOP);
        calendarSilentStop.set(Calendar.MINUTE,0);

        Log.d(LOG_TAG,"cycle has been running "+(time_since_cycle_start/60000)+" minutes");

        //set AlarmManager
        scheduler = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        intentHelper = new Intent(getApplicationContext(), StepReaderService.class);
        scheduledIntent = PendingIntent.getService(getApplicationContext(), REQUEST_CODE,
                intentHelper, PendingIntent.FLAG_CANCEL_CURRENT);

        //set SensorManager
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI);

        return START_STICKY;
    }

    @Override
    public void onDestroy (){
        Log.d(LOG_TAG, "steps updated " + steps);
        Log.d(LOG_TAG, "steps-oldSteps=" + (steps - oldSteps));

        //silent hours
        if(System.currentTimeMillis()> calendarSilentStart.getTimeInMillis()){
            scheduler.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(),calendarSilentStop.getTimeInMillis(), scheduledIntent);
        }
        else if(steps-oldSteps>DEFAULT_STEP_COUNT_TRIGGER || time_since_cycle_start>DEFAULT_CYCLE_TIME){
            setAlarm(DEBUG_INTERVAL);
            startNewCycle();
        }
        else{
            setAlarm(DEFAULT_INTERVAL);
        }

        sensorManager.unregisterListener(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {

        for (DataEvent dataEvent : dataEvents) {
            Log.d(LOG_TAG,dataEvent.toString());
            if (dataEvent.getType() == DataEvent.TYPE_CHANGED) {
                DataMap dataMap = DataMapItem.fromDataItem(dataEvent.getDataItem()).getDataMap();
                resumeOnReboot = dataMap.getBoolean(REBOOT_KEY);
                silent_start = dataMap.getLong(SILENT_START_KEY);
                silent_stop = dataMap.getLong(SILENT_STOP_KEY);
                remind_interval = Integer.valueOf(dataMap.getString(REMIND_INTERVAL_KEY));
                step_trigger = Integer.valueOf(dataMap.getString(STEP_TRIGGER_KEY));

                //note:times are in GMT
                Log.d(LOG_TAG,"resume on reboot:"+resumeOnReboot);
                Log.d(LOG_TAG,"silent start:"+(silent_start*0.000000277778));
                Log.d(LOG_TAG,"silent stop:"+(silent_stop*0.000000277778));
                Log.d(LOG_TAG,"remind interval:"+remind_interval);
                Log.d(LOG_TAG,"step trigger:"+step_trigger);
            }
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        final Uri dataItemUri =
                new Uri.Builder().scheme(WEAR_URI_SCHEME).path("/preferences").build();
        if (Log.isLoggable(LOG_TAG, Log.DEBUG)) {
            Log.d(LOG_TAG, "Deleting Uri: " + dataItemUri.toString());
        }
        Wearable.DataApi.deleteDataItems(
                mGoogleApiClient, dataItemUri).setResultCallback(this);
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
    }

    @Override
    public void onResult(DataApi.DeleteDataItemsResult deleteDataItemsResult) {
        Log.d(LOG_TAG,"disconnecting");
        mGoogleApiClient.disconnect();
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // do nothing
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // grab the values off the main thread
        new SensorEventLoggerTask().execute(event);
        stopSelf();
    }

    private class SensorEventLoggerTask extends AsyncTask<SensorEvent, Void, Float> {
        @Override
        protected Float doInBackground(SensorEvent... events) {
            SensorEvent event = events[0];
            steps = event.values[0];

            return steps;
        }
    }

    private void startNewCycle(){
        SharedPreferences sharedPref = getSharedPreferences(PERSISTENT_STEPS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putFloat(SAVED_STEPS, steps);
        editor.putLong(CYCLE_START_TIME, System.currentTimeMillis());
        editor.apply();

        if(steps-oldSteps<DEFAULT_STEP_COUNT_TRIGGER){
            notifyUser();
        }

        Log.d(LOG_TAG,"New cycle started!");
    }

    private void notifyUser(){
        Intent notifyIntent = new Intent();
        notifyIntent.setAction("fi.antonlehmus.activityreminde.notifyUser");
        sendBroadcast(notifyIntent);
    }

    private void cancelAlarm(){
        scheduler.cancel(scheduledIntent);
    }

    private void setAlarm(int interval){
        long timeToNextAlarm = interval;
        if(timeToNextAlarm==0) {
           timeToNextAlarm=AlarmManager.INTERVAL_HOUR;
        }
        else {
           timeToNextAlarm = minuteToMilliSecond(interval);
        }

        Log.d(LOG_TAG, "next start after "+(timeToNextAlarm/60000)+" minutes ");

        scheduler.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), timeToNextAlarm, scheduledIntent);
    }

    private long minuteToMilliSecond(int min){
        return 60000*min;
    }

    public static PendingIntent getScheduledIntent(){
        return scheduledIntent;
    }

}
