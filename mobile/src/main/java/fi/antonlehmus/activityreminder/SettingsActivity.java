package fi.antonlehmus.activityreminder;



import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Wearable;


public class SettingsActivity extends AppCompatActivity {

    private final String TAG ="SettingsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

    }

    @Override
    protected void onPause (){
        super.onPause();
        syncToWear(this.findViewById(android.R.id.content));
    }

    public void syncToWear(View view){

    }

}
