package fi.antonlehmus.activityreminder;


import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;


public class notifyUser extends WakefulBroadcastReceiver {

    @Override
    public void onReceive(final Context context, Intent intent) {
        //Log.d("notifyUser","creating wakeful notification");
        Intent service = new Intent(context, userNotificationBuilder.class);
        startWakefulService(context, service);
    }

}
