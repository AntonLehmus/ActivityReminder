package fi.antonlehmus.activityreminder;

import android.app.Activity;
import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.wearable.activity.ConfirmationActivity;
import android.support.wearable.view.WatchViewStub;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;


public class MainActivity extends Activity  {

    private Button mButton;


    public static boolean alarmSet = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mButton = (Button) stub.findViewById(R.id.btnStartReminder);
            }
        });

        if(StepReaderService.getScheduledIntent() != null){
            alarmSet=true;
        }

    }


    protected void onResume() {
        super.onResume();
    }

    protected void onPause() {
        super.onPause();
    }



    public void startReminder(View view){

        if(!alarmSet) {

            //initialize and start StepReaderService
            Intent initializerIntent = new Intent(this, StepReaderInitializer.class);
            startService(initializerIntent);

            alarmSet = true;

            //show success animation
            Intent animationIntent = new Intent(this, ConfirmationActivity.class);
            animationIntent.putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE,
                    ConfirmationActivity.SUCCESS_ANIMATION);
            animationIntent.putExtra(ConfirmationActivity.EXTRA_MESSAGE,
                    getString(R.string.startedReminder));
            startActivity(animationIntent);
        }
        else{
            Toast.makeText(this, "Activity reminder already running!", Toast.LENGTH_SHORT).show();
        }


    }

    public void stopReminder(View view){
        AlarmManager scheduler = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        scheduler.cancel(StepReaderService.getScheduledIntent());

        Intent animationIntent = new Intent(this, ConfirmationActivity.class);
        animationIntent.putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE,
                ConfirmationActivity.FAILURE_ANIMATION);
        animationIntent.putExtra(ConfirmationActivity.EXTRA_MESSAGE,
                getString(R.string.stoppedReminder));
        startActivity(animationIntent);

        alarmSet=false;
    }

}
