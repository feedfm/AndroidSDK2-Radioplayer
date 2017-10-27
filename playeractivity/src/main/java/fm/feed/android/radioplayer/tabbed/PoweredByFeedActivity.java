package fm.feed.android.radioplayer.tabbed;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;

/**
 * Created by eml.
 * Copyright 2017, Feed Media LLC
 */

public class PoweredByFeedActivity extends AppCompatActivity {

    private final static String TAG = PoweredByFeedActivity.class.getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_powered_by_feed);

        if (getSupportActionBar() != null) {
            Log.e(TAG, "Please assign a theme to this activity with no action bar.");
        }

        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        ActionBar ab = getSupportActionBar();
        ab.setDisplayHomeAsUpEnabled(true);
        ab.setHomeAsUpIndicator(R.drawable.feedfm_ic_close_white_24dp);
    }

}
