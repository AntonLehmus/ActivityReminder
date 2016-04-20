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
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;


public class StepReaderService extends Service implements SensorEventListener {


    public static final int REQUEST_CODE = 931;

    private final String LOG_TAG ="StepReaderService";
    private final String SAVED_STEPS ="saved_steps";
    private final String PERSISTENT_STEPS = "persistent_steps" ;
    private final String CYCLE_START_TIME = "cycle_start_time" ;
    private final int DEFAULT_STEP_COUNT_TRIGGER = 15;
    private final int DEFAULT_INTERVAL = 20;//15-20min vaikutti hyvältä
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


    private static long silent_start;
    private static long silent_stop ;
    private static int remind_interval;
    private static int step_trigger;
    private static long remind_interval_millis;


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        //read user configuration
        SharedPreferences sharedPrefUsr =  getSharedPreferences(wearApiListenerService.USR_CONFIG, Context.MODE_PRIVATE);
        silent_start = sharedPrefUsr.getLong(wearApiListenerService.SILENT_START_KEY,DEFAULT_SILENT_START);
        silent_stop = sharedPrefUsr.getLong(wearApiListenerService.SILENT_STOP_KEY,DEFAULT_SILENT_STOP);
        remind_interval = sharedPrefUsr.getInt(wearApiListenerService.REMIND_INTERVAL_KEY,DEFAULT_INTERVAL);
        step_trigger = sharedPrefUsr.getInt(wearApiListenerService.STEP_TRIGGER_KEY,DEFAULT_STEP_COUNT_TRIGGER);

        remind_interval_millis = TimeUnit.MINUTES.toMillis(remind_interval);

        //read steps from last time and cycle running time
        SharedPreferences sharedPref =  getSharedPreferences(PERSISTENT_STEPS, Context.MODE_PRIVATE);
        oldSteps = sharedPref.getFloat(SAVED_STEPS,0);
        last_cycle_time = sharedPref.getLong(CYCLE_START_TIME, 0);
        time_since_cycle_start = System.currentTimeMillis()-last_cycle_time;


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

        Calendar calendar = Calendar.getInstance();
        //current milliseconds from start of the day
        long currentMillis = getCurrentMillis(calendar);


        //Log.d(LOG_TAG, "total steps" + steps);
        Log.d(LOG_TAG, "steps-oldSteps=" + (steps - oldSteps));
        Log.d(LOG_TAG, "remind_interval_millis - time_since_cycle_start =" + (remind_interval_millis - time_since_cycle_start ));
        Log.d(LOG_TAG,"silent start hour:"+((silent_start)*0.000000277778));
        Log.d(LOG_TAG,"silent stop hour:"+((silent_stop)*0.000000277778));


        //silent hours
        if( currentMillis > silent_start){
            scheduler.setInexactRepeating(AlarmManager.RTC_WAKEUP,  System.currentTimeMillis(),silent_stop, scheduledIntent);
        }
        else if(steps-oldSteps > step_trigger || time_since_cycle_start > remind_interval_millis){
            setAlarm(remind_interval);
            startNewCycle();
        }
        else{
            setAlarm(DEFAULT_INTERVAL);
        }

        sensorManager.unregisterListener(this);
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

    private void startNewCycle(){
        SharedPreferences sharedPref = getSharedPreferences(PERSISTENT_STEPS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putFloat(SAVED_STEPS, steps);
        editor.putLong(CYCLE_START_TIME, System.currentTimeMillis());
        editor.apply();

        if((steps-oldSteps) < DEFAULT_STEP_COUNT_TRIGGER){
            notifyUser();
        }

        Log.d(LOG_TAG,"New cycle started!");
    }

    private void notifyUser(){
        Intent notifyIntent = new Intent();
        notifyIntent.setAction("fi.antonlehmus.activityreminde.notifyUser");
        sendBroadcast(notifyIntent);
    }
    
    private void setAlarm(int interval){
        long timeToNextAlarm = interval;
        if(timeToNextAlarm==0) {
           timeToNextAlarm=AlarmManager.INTERVAL_HOUR;
        }
        else {
           timeToNextAlarm = TimeUnit.MINUTES.toMillis(interval);
        }

        Log.d(LOG_TAG, "next start after "+(timeToNextAlarm/60000)+" minutes ");

        scheduler.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), timeToNextAlarm, scheduledIntent);
    }

    //returns milliseconds of calendar object from start of the day
    private long getCurrentMillis(Calendar calendar){
        long sum = 0;

        sum=TimeUnit.HOURS.toMillis(calendar.get(Calendar.HOUR_OF_DAY))+
                TimeUnit.MINUTES.toMillis(calendar.get(Calendar.MINUTE))+
                TimeUnit.SECONDS.toMillis(calendar.get(Calendar.SECOND))+
                calendar.get(Calendar.MILLISECOND);
        return sum;
    }

    public static PendingIntent getScheduledIntent(){
        return scheduledIntent;
    }

}
