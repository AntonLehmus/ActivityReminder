package fi.antonlehmus.activityreminder;





import android.content.IntentSender;
import android.content.SharedPreferences;
import android.os.AsyncTask;
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

import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public class SettingsActivity extends AppCompatActivity implements
        DataApi.DataListener,
        MessageApi.MessageListener, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    /**
     * Request code for launching the Intent to resolve Google Play services errors.
     */
    private static final int REQUEST_RESOLVE_ERROR = 1000;

    private static final String START_ACTIVITY_PATH = "/start-activity";
    private static final String CONFIG_PATH = "/config";



    private static final String TAG ="SettingsActivity";
    private static final String REBOOT_KEY = "resume_on_reboot";
    private static final String SILENT_START_KEY = "silent_start";
    private static final String SILENT_STOP_KEY = "silent_stop";
    private static final String STEP_TRIGGER_KEY = "steps_trigger";
    private static final String REMIND_INTERVAL_KEY = "remind_interval";
    private static final String STATE_RESOLVING_ERROR = "resolving_error";


    private GoogleApiClient mGoogleApiClient;
    private boolean mResolvingError;

    // Send DataItems.
    private ScheduledExecutorService mGeneratorExecutor;
    private ScheduledFuture<?> mDataItemGeneratorFuture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);


        mGeneratorExecutor = new ScheduledThreadPoolExecutor(1);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!mResolvingError) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        if (!mResolvingError) {
            Wearable.DataApi.removeListener(mGoogleApiClient, this);
            Wearable.MessageApi.removeListener(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }

    @Override //ConnectionCallbacks
    public void onConnected(Bundle connectionHint) {
        //Log.d(TAG, "Google API Client was connected");
        mResolvingError = false;
        Wearable.DataApi.addListener(mGoogleApiClient, this);
        Wearable.MessageApi.addListener(mGoogleApiClient, this);
    }

    @Override //ConnectionCallbacks
    public void onConnectionSuspended(int cause) {
        //Log.d(TAG, "Connection to Google API client was suspended");
    }

    @Override //OnConnectionFailedListener
    public void onConnectionFailed(ConnectionResult result) {
        if (mResolvingError) {
            // Already attempting to resolve an error.
            return;
        } else if (result.hasResolution()) {
            try {
                mResolvingError = true;
                result.startResolutionForResult(this, REQUEST_RESOLVE_ERROR);
            } catch (IntentSender.SendIntentException e) {
                // There was an error with the resolution intent. Try again.
                mGoogleApiClient.connect();
            }
        } else {
            Log.e(TAG, "Connection to Google API client has failed");
            mResolvingError = false;
            Wearable.DataApi.removeListener(mGoogleApiClient, this);
            Wearable.MessageApi.removeListener(mGoogleApiClient, this);
        }
    }

    @Override //DataListener
    public void onDataChanged(DataEventBuffer dataEvents) {
        //Log.d(TAG, "onDataChanged: " + dataEvents);
    }

    @Override //MessageListener
    public void onMessageReceived(final MessageEvent messageEvent) {
        //Log.d(TAG, "onMessageReceived() A message from watch was received:" + messageEvent
        //        .getRequestId() + " " + messageEvent.getPath());
    }

    private class DataItemGenerator implements Runnable {

        @Override
        public void run() {

            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplication());
            boolean resumeOnReboot = sharedPref.getBoolean(getString(R.string.key_resume_on_boot), true);
            long silent_start = sharedPref.getLong(getString(R.string.key_silent_start), 72000000);
            long silent_stop = sharedPref.getLong(getString(R.string.key_silent_stop), 28800000);
            int remind_interval = Integer.valueOf(sharedPref.getString(getString(R.string.key_remind_interval), "60"));
            int step_trigger = Integer.valueOf(sharedPref.getString(getString(R.string.key_step_trigger), "15"));

            //note:times are in GMT
            //Log.d(TAG, "\n");
            //Log.d(TAG, "resume on reboot:" + resumeOnReboot);
            Log.d(TAG, "silent start:" + (silent_start * 0.000000277778));
            Log.d(TAG, "silent stop:" + (silent_stop * 0.000000277778));
            //Log.d(TAG, "remind interval:" + remind_interval);
            //Log.d(TAG, "step trigger:" + step_trigger);


            PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(CONFIG_PATH);
            putDataMapRequest.getDataMap().putBoolean(REBOOT_KEY, resumeOnReboot);
            putDataMapRequest.getDataMap().putLong(SILENT_START_KEY, silent_start);
            putDataMapRequest.getDataMap().putLong(SILENT_STOP_KEY, silent_stop);
            putDataMapRequest.getDataMap().putInt(REMIND_INTERVAL_KEY, remind_interval);
            putDataMapRequest.getDataMap().putInt(STEP_TRIGGER_KEY, step_trigger);

            PutDataRequest request = putDataMapRequest.asPutDataRequest();
            request.setUrgent();

            Log.d(TAG, "Generating DataItem: " + request);
            if (!mGoogleApiClient.isConnected()) {
                return;
            }
            Wearable.DataApi.putDataItem(mGoogleApiClient, request)
                    .setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                        @Override
                        public void onResult(DataApi.DataItemResult dataItemResult) {
                            if (!dataItemResult.getStatus().isSuccess()) {
                                Log.e(TAG, "ERROR: failed to putDataItem, status code: "
                                        + dataItemResult.getStatus().getStatusCode());
                            }
                        }
                    });
        }
    }


    /**
     * Returns a string built from the current time
     */
    private String now() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }


    public void btnSyncToWear(View view){
        sendSettingsToWear();
        Toast.makeText(this,R.string.synced_to_wear, Toast.LENGTH_SHORT).show();
    }

    private void sendSettingsToWear(){
        mDataItemGeneratorFuture = mGeneratorExecutor.schedule(
                new DataItemGenerator(), 100,TimeUnit.MILLISECONDS);
    }

    public void btnStartWearActivity(View view){
        //Log.d(TAG, "Generating RPC");

        // Trigger an AsyncTask that will query for a list of connected nodes and send a
        // "start-activity" message to each connected node.
        new StartWearableActivityTask().execute();

        Toast.makeText(this,R.string.wear_opened, Toast.LENGTH_SHORT).show();
    }


    private Collection<String> getNodes() {
        HashSet<String> results = new HashSet<>();
        NodeApi.GetConnectedNodesResult nodes =
                Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();

        for (Node node : nodes.getNodes()) {
            results.add(node.getId());
        }

        return results;
    }

    private void sendStartActivityMessage(String node) {
        Wearable.MessageApi.sendMessage(
                mGoogleApiClient, node, START_ACTIVITY_PATH, new byte[0]).setResultCallback(
                new ResultCallback<MessageApi.SendMessageResult>() {
                    @Override
                    public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                        if (!sendMessageResult.getStatus().isSuccess()) {
                            Log.e(TAG, "Failed to send message with status code: "
                                    + sendMessageResult.getStatus().getStatusCode());
                        }
                    }
                }
        );
    }

    private class StartWearableActivityTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... args) {
            Collection<String> nodes = getNodes();
            for (String node : nodes) {
                sendStartActivityMessage(node);
            }
            return null;
        }
    }

}
