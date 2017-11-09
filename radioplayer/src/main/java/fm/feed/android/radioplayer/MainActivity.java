package fm.feed.android.radioplayer;

/**
 * Copyright Feed Media, 2016
 */

import android.annotation.SuppressLint;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import fm.feed.android.playersdk.FeedAudioPlayer;
import fm.feed.android.playersdk.FeedPlayerService;
import fm.feed.android.radioplayer.tabbed.PlayerActivity;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        configureRadio();
    }

    @SuppressLint("SetTextI18n")
    private void configureRadio() {
        Button openPlayer = (Button) findViewById(R.id.openPlayerButton);

        openPlayer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // start the player and show _all_ stations not marked as hidden
                Intent ai = new Intent(MainActivity.this, PlayerActivity.class);
                ai.putExtra(PlayerActivity.EXTRA_PARENT_ACTIVITY, new Intent(MainActivity.this, MainActivity.class));

                startActivity(ai);
            }
        });

        Button openHidden = findViewById(R.id.openHiddenButton);

        openHidden.setOnClickListener(new View.OnClickListener() {
                                         @Override
                                         public void onClick(View v) {
                 // start the player and show non-hidden stations plus the station named "Promo"
                 Intent ai = new Intent(MainActivity.this, PlayerActivity.class);
                 ArrayList<String> stations = new ArrayList<String>();
                 stations.add("Hidden Station");
                 ai.putStringArrayListExtra(PlayerActivity.EXTRA_UNHIDE_STATION_LIST, stations);
                 ai.putExtra(PlayerActivity.EXTRA_DEFAULT_STATION, "Hidden Station");
                 ai.putExtra(PlayerActivity.EXTRA_PARENT_ACTIVITY, new Intent(MainActivity.this, MainActivity.class));
                 startActivity(ai);
             }
        });


        // if we want, we can just pull up a specific list of stations to display

        Button openJustHidden = findViewById(R.id.openJustHiddenButton);

        openJustHidden.setOnClickListener(new View.OnClickListener() {
                                         @Override
                                         public void onClick(View v) {
                 // start the player and show only the station named "Promo"
                 Intent ai = new Intent(MainActivity.this, PlayerActivity.class);
                 ArrayList<String> stations = new ArrayList<String>();
                 stations.add("Hidden Station");
                 ai.putStringArrayListExtra(PlayerActivity.EXTRA_VISIBLE_STATION_LIST, stations);
                 ai.putExtra(PlayerActivity.EXTRA_PARENT_ACTIVITY, new Intent(MainActivity.this, MainActivity.class));
                 startActivity(ai);
             }
         });

        // make buttons visible if radio is available




    }

    @Override
    protected void onResume() {
        super.onResume();
        FeedPlayerService.getInstance(new FeedAudioPlayer.AvailabilityListener() {

            @Override
            public void onPlayerAvailable(FeedAudioPlayer feedAudioPlayer) {

                findViewById(R.id.openButtons).setVisibility(View.VISIBLE);

                ((TextView)findViewById(R.id.helloText)).setText("Music Available!");
            }

            @Override
            public void onPlayerUnavailable(Exception e) {
                ((TextView)findViewById(R.id.helloText)).setText("Sorry, music is not available for you right now");
                Toast.makeText(MainActivity.this, "Sorry, music is not available for you right now "+ e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
