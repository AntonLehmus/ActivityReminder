package fi.antonlehmus.activityreminder;




import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.util.Log;
import android.view.View;
import android.widget.Toast;



import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public class SettingsActivity extends AppCompatActivity
        implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener  {

    private static final String TAG ="SettingsActivity";
    private static final String REBOOT_KEY = "resume_on_reboot";
    private static final String SILENT_START_KEY = "silent_start";
    private static final String SILENT_STOP_KEY = "silent_stop";
    private static final String STEP_TRIGGER_KEY = "steps_trigger";
    private static final String REMIND_INTERVAL_KEY = "remind_interval";
    private static final String STATE_RESOLVING_ERROR = "resolving_error";


    private GoogleApiClient mGoogleApiClient;
    private boolean mResolvingError;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);


        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

    }

    @Override
    public void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    public void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onConnected(Bundle bundle) {
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e(TAG, "Failed to connect to Google API Client");
        if (connectionResult.getErrorCode() == ConnectionResult.API_UNAVAILABLE) {
            Toast.makeText(this,R.string.no_wear_api, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Returns a string built from the current time
     */
    private String now() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_RESOLVING_ERROR, mResolvingError);
    }

    public void btnSyncToWear(View view){
        syncToWear();
        Toast.makeText(this,R.string.synced_to_wear, Toast.LENGTH_SHORT).show();
    }


    private void syncToWear() {

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        boolean resumeOnReboot = sharedPref.getBoolean(getString(R.string.key_resume_on_boot),true);
        long silent_start = sharedPref.getLong(getString(R.string.key_silent_start),72000000);
        long silent_stop = sharedPref.getLong(getString(R.string.key_silent_stop),28800000);
        int remind_interval = Integer.valueOf(sharedPref.getString(getString(R.string.key_remind_interval),"60"));
        int step_trigger = Integer.valueOf(sharedPref.getString(getString(R.string.key_step_trigger),"15"));

        //note:times are in GMT
        Log.d(TAG,"\n");
        Log.d(TAG,"resume on reboot:"+resumeOnReboot);
        Log.d(TAG,"silent start:"+(silent_start*0.000000277778));
        Log.d(TAG,"silent stop:"+(silent_stop*0.000000277778));
        Log.d(TAG,"remind interval:"+remind_interval);
        Log.d(TAG,"step trigger:"+step_trigger);

        if (mGoogleApiClient.isConnected()) {

            Log.d(TAG,"connected to wear. Trying to send.");

            PutDataMapRequest putDataMapRequest = PutDataMapRequest.create("/preferences");

            //DEBUG
            putDataMapRequest.getDataMap().putLong("time_stamp",System.currentTimeMillis());
            Log.d(TAG,"timestamp:"+System.currentTimeMillis());
            //END DEBUG

            putDataMapRequest.getDataMap().putBoolean(REBOOT_KEY, resumeOnReboot);
            putDataMapRequest.getDataMap().putLong(SILENT_START_KEY, silent_start);
            putDataMapRequest.getDataMap().putLong(SILENT_STOP_KEY, silent_stop);
            putDataMapRequest.getDataMap().putInt(REMIND_INTERVAL_KEY, remind_interval);
            putDataMapRequest.getDataMap().putInt(STEP_TRIGGER_KEY, step_trigger);
            PutDataRequest request = putDataMapRequest.asPutDataRequest();
            request.setUrgent();
            Wearable.DataApi.putDataItem(mGoogleApiClient, request)
                    .setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                        @Override
                        public void onResult(DataApi.DataItemResult dataItemResult) {
                            if (!dataItemResult.getStatus().isSuccess()) {
                                Log.e(TAG, "syncToWear(): Failed to set the data, "
                                        + "status: " + dataItemResult.getStatus().getStatusCode());
                            }
                            else{
                                Log.d(TAG,"Successfully sent settings to wear");
                            }
                        }
                    });
        } else {
            Log.e(TAG, "syncToWear(): no Google API Client connection");
        }

    }

}
