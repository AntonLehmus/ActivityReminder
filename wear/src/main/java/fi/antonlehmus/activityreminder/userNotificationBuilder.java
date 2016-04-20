package fi.antonlehmus.activityreminder;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.WindowManager;


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

        //mBuilder.build();




        // Sets an ID for the notification
        int mNotificationId = 931;

        // Set the default notification option
        // DEFAULT_SOUND : Make sound
        // DEFAULT_VIBRATE : Vibrate
        // DEFAULT_LIGHTS : Use the default light notification
        // DEFAULT_ALL  : All of the above
        //mBuilder.setDefaults(Notification.DEFAULT_ALL);


        // Gets a NotificationManager which is used to notify the user of the background event
        //NotificationManager mNotifyMgr =
        //        (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationManagerCompat notificationManager =
                NotificationManagerCompat.from(this);



        // Builds the notification and issues it.
        //mNotifyMgr.notify(mNotificationId, mBuilder.build());

        // Issue the notification with notification manager.
        notificationManager.notify(mNotificationId,notif);

        //release wake lock
        notifyUser.completeWakefulIntent(intent);
    }
}
