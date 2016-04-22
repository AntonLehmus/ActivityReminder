package fi.antonlehmus.activityreminder;



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
                //initialize StepReaderService
                Intent initializerIntent = new Intent(context, StepReaderInitializer.class);
                context.startService(initializerIntent);

                Log.d("bootRestarter", "Restarted StepReaderService");
            }
        }
    }
}
