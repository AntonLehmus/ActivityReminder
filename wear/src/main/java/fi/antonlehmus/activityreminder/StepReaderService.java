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
import android.os.SystemClock;
import android.util.Log;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;


public class StepReaderService extends Service implements SensorEventListener {


    public static final int REQUEST_CODE = 931;

    public static final String LOG_TAG ="StepReaderService";
    public static final String SAVED_STEPS ="saved_steps";
    public static final String PERSISTENT_STEPS = "persistent_steps" ;
    public static final String CYCLE_START_TIME = "cycle_start_time" ;
    public static final String FIRST_RUN = "first_run" ;
    private static final int DEFAULT_STEP_COUNT_TRIGGER = 15;
    private static final int DEFAULT_INTERVAL = 20;//15-20min vaikutti hyvältä
    private static final int DEFAULT_SILENT_START = 20;
    private static final int DEFAULT_SILENT_STOP = 8;


    private static float steps = 0;
    private static float oldSteps = 0;
    private static long time_since_cycle_start = 0;
    private static long last_cycle_time = 0;

    private static AlarmManager scheduler = null;
    private static Intent intentHelper = null;
    private static PendingIntent scheduledIntent = null;


    private  static SensorManager sensorManager = null;
    private static Sensor sensor = null;


    private static long silent_start;
    private static long silent_stop ;
    private static int remind_interval;
    private static int step_trigger;
    private static long remind_interval_millis;
    private static long time_left_in_cycle;
    private static boolean first_run;
    private static Intent callingIntent;


    private int check_interval;


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        callingIntent = intent;

        //read user configuration
        SharedPreferences sharedPrefUsr =  getSharedPreferences(wearApiListenerService.USR_CONFIG, Context.MODE_PRIVATE);
        silent_start = sharedPrefUsr.getLong(wearApiListenerService.SILENT_START_KEY,DEFAULT_SILENT_START);
        silent_stop = sharedPrefUsr.getLong(wearApiListenerService.SILENT_STOP_KEY,DEFAULT_SILENT_STOP);
        remind_interval = sharedPrefUsr.getInt(wearApiListenerService.REMIND_INTERVAL_KEY,DEFAULT_INTERVAL);
        step_trigger = sharedPrefUsr.getInt(wearApiListenerService.STEP_TRIGGER_KEY,DEFAULT_STEP_COUNT_TRIGGER);

        //midnight as 24:00 instead of 00:00
        if(silent_stop == 0){
            silent_stop = 86400000;
        }
        if(silent_start == 0){
            silent_start = 86400000;
        }

        remind_interval_millis = TimeUnit.MINUTES.toMillis(remind_interval);

        //read steps from last time and cycle running time
        SharedPreferences sharedPref =  getSharedPreferences(PERSISTENT_STEPS, Context.MODE_PRIVATE);
        oldSteps = sharedPref.getFloat(SAVED_STEPS,0);
        last_cycle_time = sharedPref.getLong(CYCLE_START_TIME, 0);
        first_run = sharedPref.getBoolean(FIRST_RUN,false);
        time_since_cycle_start = System.currentTimeMillis()-last_cycle_time;


        //set AlarmManager
        scheduler = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        intentHelper = new Intent(getApplicationContext(), StepReaderStarter.class);
        scheduledIntent = PendingIntent.getBroadcast(getApplicationContext(), REQUEST_CODE,
                intentHelper, PendingIntent.FLAG_CANCEL_CURRENT);

        //set SensorManager
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI);

        //adapt check_interval based on remind_interval
        check_interval = remind_interval/4;
        if(check_interval<1){
            check_interval=1;
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy (){
        time_left_in_cycle = remind_interval_millis - time_since_cycle_start;


        //Log.d(LOG_TAG, "total steps " + steps);
        //Log.d(LOG_TAG, "old steps " + oldSteps);
        //Log.d(LOG_TAG, "steps-oldSteps=" + (steps - oldSteps));
        //Log.d(LOG_TAG, "step trigger = " +step_trigger);
        //Log.d(LOG_TAG, "time left in this cycle = "+TimeUnit.MILLISECONDS.toMinutes(time_left_in_cycle)+" minutes ("+time_left_in_cycle+")ms");




        //silent hours
        if(isSilentHours()){
            //set next run at end of silent hours
            Log.d(LOG_TAG,"Silent hours started!");
            Intent initializerHelper = new Intent(getApplicationContext(), StepReaderInitializer.class);
            PendingIntent scheduledInitializerIntent = PendingIntent.getService(getApplicationContext(), REQUEST_CODE,
                    initializerHelper, PendingIntent.FLAG_CANCEL_CURRENT);
            scheduler.setExact(AlarmManager.RTC_WAKEUP,  System.currentTimeMillis()+getTimeToSilentStop(), scheduledInitializerIntent);
        }
        else if(first_run){
            //(LOG_TAG, "First run_____");
            startNewCycle();
            SharedPreferences sharedPref = getSharedPreferences(StepReaderService.PERSISTENT_STEPS, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putBoolean(StepReaderService.FIRST_RUN,false);
            editor.apply();

            //one minute inexact alarm
            setAlarm(TimeUnit.MINUTES.toMillis(1),false);
        }
        else if((steps-oldSteps) > step_trigger){
            //exact alarm
            setAlarm(TimeUnit.MINUTES.toMillis(remind_interval), true);
            startNewCycle();
        }
        else if(time_left_in_cycle <= 300) {
            startNewCycle();
            //inexact alarm
            setAlarm(TimeUnit.MINUTES.toMillis(check_interval), false);
        }
        else if(TimeUnit.MILLISECONDS.toMinutes(time_left_in_cycle) < check_interval && !first_run){
            //exact alarm at the end of the cycle to prevent time shifts
            setAlarm(time_left_in_cycle,true);
        }
        else{
            //inexact alarm
            setAlarm(TimeUnit.MINUTES.toMillis(check_interval), false);
        }
        sensorManager.unregisterListener(this);
        //release wake lock
        StepReaderStarter.completeWakefulIntent(callingIntent);
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
        //Log.d(LOG_TAG,"got sensor event");
        new SensorEventLoggerTask().execute(event);
    }

    private class SensorEventLoggerTask extends AsyncTask<SensorEvent, Void, Float> {
        @Override
        protected Float doInBackground(SensorEvent... events) {
            SensorEvent event = events[0];
            steps = event.values[0];
            return steps;
        }
        @Override
        protected void onPostExecute (Float result){
            //Log.d(LOG_TAG,"sensor event saved");
            stopSelf();
        }
    }

    private void startNewCycle(){
        //save variables for next check
        SharedPreferences sharedPref = getSharedPreferences(PERSISTENT_STEPS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putFloat(SAVED_STEPS, steps);
        editor.putLong(CYCLE_START_TIME, System.currentTimeMillis());
        editor.apply();

        if((steps-oldSteps) < step_trigger && !first_run){
            //user did not reach his/hers goal
            //Log.d(LOG_TAG,"notifying user");
            notifyUser();
        }

        //Log.d(LOG_TAG,"New cycle started!");
    }

    private void notifyUser(){
        Intent notifyIntent = new Intent();
        notifyIntent.setAction("fi.antonlehmus.activityreminde.notifyUser");
        sendBroadcast(notifyIntent);
    }
    
    private void setAlarm(long interval, boolean exact){
        long timeToNextAlarm = interval;
        String alarm_type = "Inexact";

        if(timeToNextAlarm==0) {
           timeToNextAlarm=AlarmManager.INTERVAL_HOUR;
        }
        if(exact){
            scheduler.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime()+timeToNextAlarm, scheduledIntent);
            alarm_type="Exact";
        }
        else {
            scheduler.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime(), timeToNextAlarm, scheduledIntent);
        }

        //Log.d(LOG_TAG, "next start after "+(TimeUnit.MILLISECONDS.toMinutes(timeToNextAlarm))+" minutes as "+alarm_type);
    }

    //returns milliseconds of calendar object from start of the day
    private long getCurrentMillis(Calendar calendar){
        long sum;

        sum=TimeUnit.HOURS.toMillis(calendar.get(Calendar.HOUR_OF_DAY))+
                TimeUnit.MINUTES.toMillis(calendar.get(Calendar.MINUTE))+
                TimeUnit.SECONDS.toMillis(calendar.get(Calendar.SECOND))+
                calendar.get(Calendar.MILLISECOND);
        return sum;
    }

    private long getTimeToSilentStop(){
        long time;
        final long dayInMillis = 86400000; //24 hours in milliseconds
        Calendar calendar = Calendar.getInstance();
        long now = getCurrentMillis(calendar);

        //silent hours stop on the next day
        if(silent_stop<silent_start){
            time=(dayInMillis-now)+silent_stop;
        }
        else{//silent hours stop on the same day
            time=silent_stop-silent_start;
        }

        return time;
    }

    public static PendingIntent getScheduledIntent(){
        return scheduledIntent;
    }

    private boolean isSilentHours(){
        Calendar calendar = Calendar.getInstance();
        //current milliseconds from start of the day
        long currentMillis = getCurrentMillis(calendar);

        //Log.d(LOG_TAG,"silent start hour: "+((silent_start)*0.000000277778));
        //Log.d(LOG_TAG,"silent stop hour: "+((silent_stop)*0.000000277778));
        //Log.d(LOG_TAG,"current hour: "+((currentMillis)*0.000000277778));


        //silent hours stop on the next day
        if(silent_stop<silent_start){
           if(currentMillis>silent_start){
               return true;
           }
        }
        else{//silent hours stop on the same day
            if(currentMillis> silent_start && currentMillis < silent_stop){
                return true;
            }
        }
        return false;
    }

}
