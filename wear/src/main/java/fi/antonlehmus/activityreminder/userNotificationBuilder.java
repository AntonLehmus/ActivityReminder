package fi.antonlehmus.activityreminder;

import android.app.IntentService;
import android.app.Notification;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;


public class userNotificationBuilder extends IntentService {

    public userNotificationBuilder(){
        super("userNotificationBuilder");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        //read user configs
        SharedPreferences sharedPrefUsr =  this.getSharedPreferences(wearApiListenerService.USR_CONFIG, Context.MODE_PRIVATE);
        int notification_priority = sharedPrefUsr.getInt(wearApiListenerService.NOTIFICATION_PRIORITY_KEY,1);

        //Log.d("userNotificationBuilder","notifying user with priority "+notification_priority);

        Bitmap bitmap = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(ContextCompat.getColor(this, R.color.colorAccent));

        // Create a WearableExtender to add functionality for wearables
        NotificationCompat.WearableExtender wearableExtender =
                new NotificationCompat.WearableExtender()
                        .setBackground(bitmap);


        Notification notif = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_directions_walk_24dp)
                        .setContentTitle(this.getString(R.string.notification_title_default))
                        .setContentText(this.getString(R.string.notification_text_default))
                        .setAutoCancel(true)
                        .setDefaults(NotificationCompat.DEFAULT_VIBRATE)
                        .setPriority(notification_priority)
                        .extend(wearableExtender)
                        .build()
                ;


        // Sets an ID for the notification
        int mNotificationId = 931;


        // Gets a NotificationManager which is used to notify the user of the background event
        NotificationManagerCompat notificationManager =
                NotificationManagerCompat.from(this);

        // Issue the notification with notification manager.
        notificationManager.notify(mNotificationId,notif);

        //release wake lock
        notifyUser.completeWakefulIntent(intent);
    }
}
