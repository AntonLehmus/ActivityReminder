package fi.antonlehmus.activityreminder;


import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

public class bootRestarter extends BroadcastReceiver {

    private boolean resume_on_reboot = true;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {

            //read user configs
            SharedPreferences sharedPrefUsr =  context.getSharedPreferences(wearApiListenerService.USR_CONFIG, Context.MODE_PRIVATE);
            resume_on_reboot = sharedPrefUsr.getBoolean(wearApiListenerService.REBOOT_KEY,true);

            if(resume_on_reboot) {
                AlarmManager scheduler = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                Intent alarmIntent = new Intent(context.getApplicationContext(), StepReaderService.class);
                PendingIntent scheduledIntent = PendingIntent.getService(context.getApplicationContext(), StepReaderService.REQUEST_CODE, alarmIntent, PendingIntent.FLAG_CANCEL_CURRENT);

                scheduler.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), MainActivity.DEFAULT_INTERVAL * 2, scheduledIntent);

                //Log.d("bootRestarter", "Restarted StepReaderService");
            }
        }
    }
}
