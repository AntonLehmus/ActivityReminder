package fi.antonlehmus.activityreminder;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;


import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;

import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;


import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;


public class wearApiListenerService extends WearableListenerService
        implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        DataApi.DataListener, MessageApi.MessageListener{

    public static final String USR_CONFIG = "user_config";
    public static final String REBOOT_KEY = "resume_on_reboot";
    public static final String SILENT_START_KEY = "silent_start";
    public static final String SILENT_STOP_KEY = "silent_stop";
    public static final String STEP_TRIGGER_KEY = "steps_trigger";
    public static final String REMIND_INTERVAL_KEY = "remind_interval";


    private static final String TAG = "wearApiListenerService";
    private static final String START_ACTIVITY_PATH = "/start-activity";
    public static final String CONFIG_PATH = "/config";


    private GoogleApiClient mGoogleApiClient;

    private static boolean resumeOnReboot;
    private static long silent_start;
    private static long silent_stop ;
    private static int remind_interval;
    private static int step_trigger;

    @Override
    public void onCreate() {
        super.onCreate();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onDestroy() {
        Wearable.DataApi.removeListener(mGoogleApiClient, this);
        Wearable.MessageApi.removeListener(mGoogleApiClient, this);
        mGoogleApiClient.disconnect();
        super.onDestroy();
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.d(TAG, "onConnected(): Successfully connected to Google API client");
        Wearable.DataApi.addListener(mGoogleApiClient, this);
        Wearable.MessageApi.addListener(mGoogleApiClient, this);
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.d(TAG, "onConnectionSuspended(): Connection to Google API client was suspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.e(TAG, "onConnectionFailed(): Failed to connect, with result: " + result);
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        Log.d(TAG, "onDataChanged(): " + dataEvents);

        for (DataEvent event : dataEvents) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                String path = event.getDataItem().getUri().getPath();


                if (CONFIG_PATH.equals(path)) {
                    Log.d(TAG, "Data Changed for CONFIG_PATH");
                    Log.d(TAG,("DataItem Changed"+event.getDataItem().toString()));

                    //read settings from data layer
                    DataMap dataMap = DataMapItem.fromDataItem(event.getDataItem()).getDataMap();
                    resumeOnReboot = dataMap.getBoolean(REBOOT_KEY);
                    silent_start = dataMap.getLong(SILENT_START_KEY);
                    silent_stop = dataMap.getLong(SILENT_STOP_KEY);
                    remind_interval = dataMap.getInt(REMIND_INTERVAL_KEY);
                    step_trigger = dataMap.getInt(STEP_TRIGGER_KEY);

                    /*
                    Log.d(TAG,"resume on reboot:"+resumeOnReboot);
                    Log.d(TAG,"silent start:"+(silent_start*0.000000277778));
                    Log.d(TAG,"silent stop:"+(silent_stop*0.000000277778));
                    Log.d(TAG,"remind interval:"+remind_interval);
                    Log.d(TAG,"step trigger:"+step_trigger);
                    */

                    //save settings to persistent storage
                    SharedPreferences sharedPref = getSharedPreferences(USR_CONFIG, Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putBoolean(REBOOT_KEY, resumeOnReboot);
                    editor.putLong(SILENT_START_KEY, silent_start);
                    editor.putLong(SILENT_STOP_KEY, silent_stop);
                    editor.putInt(REMIND_INTERVAL_KEY, remind_interval);
                    editor.putInt(STEP_TRIGGER_KEY, step_trigger);
                    editor.apply();


                } else {
                    Log.d(TAG, "Unrecognized path: " + path);
                }

            } else if (event.getType() == DataEvent.TYPE_DELETED) {
                Log.d(TAG,(("DataItem Deleted"+event.getDataItem().toString())));
            } else {
                Log.d(TAG,(("Unknown data event type"+"Type = " + event.getType())));
            }
        }
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.d(TAG, "onMessageReceived: " + messageEvent);

        // Check to see if the message is to start an activity
        if (messageEvent.getPath().equals(START_ACTIVITY_PATH)) {
            Intent startIntent = new Intent(this, MainActivity.class);
            startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(startIntent);
        }
    }


}

