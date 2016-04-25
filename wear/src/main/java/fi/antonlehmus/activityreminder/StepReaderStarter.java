package fi.antonlehmus.activityreminder;


import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

public class StepReaderStarter extends WakefulBroadcastReceiver {

    private static final String TAG = "StepReaderStarter";

    @Override
    public void onReceive(final Context context, Intent intent) {
        //Log.d(TAG,"starting StepReaderService");
        Intent service = new Intent(context, StepReaderService.class);
        startWakefulService(context, service);
    }
}
