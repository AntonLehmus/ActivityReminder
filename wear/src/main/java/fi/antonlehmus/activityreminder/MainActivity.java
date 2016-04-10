package fi.antonlehmus.activityreminder;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.wearable.view.WatchViewStub;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;


public class MainActivity extends Activity {

    private Button mButton;

    /*
    * TODO:
    * Alota servive automaattisesti
    * Pistä se hitoon täyttyvä ympyrän kehä animaatio ja koko ruudun "onnistu" merkki loppuun
    * */

    private boolean alarmSet = false;

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

    }

    protected void onResume() {
        super.onResume();
    }

    protected void onPause() {
        super.onPause();
    }



    public void startReminder(View view){
        if(!alarmSet) {

            AlarmManager scheduler = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(getApplicationContext(), StepReaderService.class);
            PendingIntent scheduledIntent = PendingIntent.getService(getApplicationContext(), StepReaderService.REQUEST_CODE, intent, PendingIntent.FLAG_CANCEL_CURRENT);

            scheduler.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 30000, scheduledIntent);

            Toast.makeText(this, "Activity reminder started!", Toast.LENGTH_SHORT).show();
            alarmSet = true;
        }
        else{
            Toast.makeText(this, "Activity reminder already running!", Toast.LENGTH_SHORT).show();
        }
        //finish();
    }

}
