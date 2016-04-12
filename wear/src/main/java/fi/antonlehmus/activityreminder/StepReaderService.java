package fi.antonlehmus.activityreminder;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Log;

import java.util.Calendar;

public class StepReaderService extends Service implements SensorEventListener {

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

        Log.d(LOG_TAG,"_STARTED_");

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

            //FOR DEBUGGING
            notifyUser();
        }

        sensorManager.unregisterListener(this);
    }

    private void startNewCycle(){
        SharedPreferences sharedPref = getSharedPreferences(PERSISTENT_STEPS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putFloat(SAVED_STEPS, steps);
        editor.putLong(CYCLE_START_TIME, System.currentTimeMillis());
        editor.commit();

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

    @Override
    public IBinder onBind(Intent intent) {
        return null;
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
