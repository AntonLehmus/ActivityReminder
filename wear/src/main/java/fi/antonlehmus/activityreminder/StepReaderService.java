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

public class StepReaderService extends Service implements SensorEventListener {

    public static final int REQUEST_CODE = 931;

    private final String LOG_TAG ="StepReaderService";
    private final String SAVED_STEPS ="saved_steps";
    private final String PERSISTENT_STEPS = "persistent_steps" ;
    private final float DEFAULT_STEP_COUNT_TRIGGER = 10;
    private final int DEFAULT_INTERVAL = 1;


    private float steps;
    private float oldSteps;

    private AlarmManager scheduler;
    private Intent intentHelper;
    private PendingIntent scheduledIntent;


    private SensorManager sensorManager = null;
    private Sensor sensor = null;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        //read steps from last time
        SharedPreferences sharedPref =  getSharedPreferences(PERSISTENT_STEPS, Context.MODE_PRIVATE);
        oldSteps = sharedPref.getFloat(SAVED_STEPS,oldSteps);


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

        if(steps-oldSteps>DEFAULT_STEP_COUNT_TRIGGER){
            setAlarm(2);
        }
        else{
            setAlarm(DEFAULT_INTERVAL);
        }

        SharedPreferences sharedPref =  getSharedPreferences(PERSISTENT_STEPS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putFloat(SAVED_STEPS, steps);
        editor.commit();

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

}
