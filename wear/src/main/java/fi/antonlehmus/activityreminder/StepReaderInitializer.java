package fi.antonlehmus.activityreminder;


import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class StepReaderInitializer extends IntentService {

    public StepReaderInitializer() {
        super("StepReaderInitializer");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        SharedPreferences sharedPref = getSharedPreferences(StepReaderService.PERSISTENT_STEPS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putFloat(StepReaderService.SAVED_STEPS,0);
        editor.putLong(StepReaderService.CYCLE_START_TIME,0);
        editor.putBoolean(StepReaderService.FIRST_RUN,true);
        editor.apply();
    }

}
