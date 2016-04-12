package fi.antonlehmus.activityreminder;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;


public class notifyUser extends WakefulBroadcastReceiver {

    @Override
    public void onReceive(final Context context, Intent intent) {

        Bitmap bitmap = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(context.getResources().getColor(R.color.teal_default));


        //FOR DEBUG
        float steps;
        SharedPreferences sharedPref =  context.getSharedPreferences("persistent_steps", Context.MODE_PRIVATE);
        steps = sharedPref.getFloat("saved_steps",0);
        //

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context)
                        .setSmallIcon(R.drawable.ic_directions_walk_24dp)
                        .setContentTitle(context.getString(R.string.notification_title_default))
                        .setContentText(context.getString(R.string.notification_text_default))
                        .setAutoCancel(true)
                        .extend(new NotificationCompat.WearableExtender().setBackground(bitmap));

        mBuilder.build();


        // Sets an ID for the notification
        int mNotificationId = 931;

        // Set the default notification option
        // DEFAULT_SOUND : Make sound
        // DEFAULT_VIBRATE : Vibrate
        // DEFAULT_LIGHTS : Use the default light notification
        // DEFAULT_ALL  : All of the above
        mBuilder.setDefaults(Notification.DEFAULT_ALL);

        // Auto cancels the notification when clicked on in the task bar
        mBuilder.setAutoCancel(true);

        // Gets a NotificationManager which is used to notify the user of the background event
        NotificationManager mNotifyMgr =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Builds the notification and issues it.
        mNotifyMgr.notify(mNotificationId, mBuilder.build());
    }

}
