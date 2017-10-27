package fm.feed.android.radioplayer.tabbed;



import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import fm.feed.android.playersdk.FeedAudioPlayer;
import fm.feed.android.playersdk.models.*;
import fm.feed.android.playersdk.*;

/**
 *
 * Copyright Feed Media, 2016
 */

public class PlayerActivity extends AppCompatActivity implements PlayerFragment.PlayerFragmentEventListener {

    private final static String TAG = PlayerActivity.class.getSimpleName();

    // String array of stations that should be displayed (see createStationTabs())
    public final static String EXTRA_VISIBLE_STATION_LIST = "fm.feed.android.radioplayer.visibleStationList";
    // String array of stations that will be unhidden (see createStationTabs())
    public final static String EXTRA_UNHIDE_STATION_LIST = "fm.feed.android.radioplayer.unhideStationList";
    // Name of station to display by default, if desired
    public final static String EXTRA_DEFAULT_STATION = "fm.feed.android.radioplayer.defaultStation";
    // Intent that points to parent activity, if any
    public final static String EXTRA_PARENT_ACTIVITY = "fm.feed.android.radioplayer.tabbed.parentIntent";

    private FeedAudioPlayer mPlayer = null;

    private ViewPager mViewPager;
    private TabLayout mTabs;
    private Station[] mVisibleStations;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_player);

        if (getSupportActionBar() != null) {
            Log.e(TAG, "Please assign a theme to this activity with no action bar.");
        }

        Toolbar mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        ActionBar ab = getSupportActionBar();
        if ((ab != null) && (getSupportParentActivityIntent() != null)) {
            ab.setDisplayHomeAsUpEnabled(true);
        }
        // make sure we're using our sexy icons
        final NotificationStyle ni = new NotificationStyle()
                .setSmallIcon(R.drawable.ic_headset_white_24dp)
                .setPlayIcon(R.drawable.feedfm_ic_play_24dp)
                .setPauseIcon(R.drawable.feedfm_ic_pause_24dp)
                .setSkipIcon(R.drawable.feedfm_ic_skip_24dp)
                .setColor(Color.BLACK)
                .setThumbsUpIcons(R.drawable.feedfm_ic_thumbsup_hollow_24dp, R.drawable.feedfm_ic_thumbsup_24dp)
                .setThumbsDownIcons(R.drawable.feedfm_ic_thumbsdown_hollow_24dp, R.drawable.feedfm_ic_thumbsdown_24dp)

                // .. and our custom notification layouts
                .setBigContentView(getPackageName(), R.layout.notification_big)
                .setContentView(getPackageName(), R.layout.notification_small)
                .setMediaImageId(R.id.notification_icon)
                .setProgressId(R.id.progress)
                .setDislikeButtonId(R.id.dislike_button)
                .setLikeButtonId(R.id.like_button)
                .setPlayPauseButtonId(R.id.play_pause_button)
                .setSkipButtonId(R.id.skip_button)
                .setTrackTextId(R.id.notification_track_title)
                .setArtistTextId(R.id.notification_track_artist)
                .setReleaseTextId(R.id.notification_track_release);

        FeedPlayerService.getInstance(new FeedAudioPlayer.AvailabilityListener() {
            @Override
            public void onPlayerAvailable(FeedAudioPlayer feedAudioPlayer) {

                mPlayer = feedAudioPlayer;
                mPlayer.setNotificationStyle(ni);

                // pick out stations to display
                mVisibleStations = collectStations();
                int selectedTabIndex = selectDefaultStation(mVisibleStations);

                mViewPager = findViewById(R.id.playerContainer);
                mViewPager.setAdapter(new PlayerFragmentStatePagerAdapter(getSupportFragmentManager(), mVisibleStations, PlayerActivity.this));

                mTabs = findViewById(R.id.radioTabs);
                if (mVisibleStations.length > 1) {
                    for (Station station : mVisibleStations) {
                        mTabs.addTab(mTabs.newTab().setText(station.getName()));
                    }
                    // listen for tab clicks
                    mTabs.addOnTabSelectedListener(onTabSelectedListener);
                    // listen for swipes
                    mViewPager.addOnPageChangeListener(mOnPageChangeListener);

                } else {
                    // no point in displaying tabs if we have only one station
                    mTabs.setVisibility(View.GONE);
                }

                // display default station
                mViewPager.setCurrentItem(selectedTabIndex);
            }

            @Override
            public void onPlayerUnavailable(Exception e) {

            }
        });

    }

    @Nullable
    @Override
    public Intent getSupportParentActivityIntent() {
        Intent intent = super.getSupportParentActivityIntent();

        Intent ai = getIntent();
        if (ai.hasExtra(EXTRA_PARENT_ACTIVITY)) {
            Intent parent = ai.getParcelableExtra(EXTRA_PARENT_ACTIVITY);

            return parent;

        } else {
            return super.getSupportParentActivityIntent();
        }
    }

    private ViewPager.OnPageChangeListener mOnPageChangeListener = new ViewPager.SimpleOnPageChangeListener() {
        public void onPageSelected(int position) {
            TabLayout.Tab tab = mTabs.getTabAt(position);

            if (tab != null) {
                tab.select();
            }
        }
    };

    private TabLayout.OnTabSelectedListener onTabSelectedListener = new TabLayout.OnTabSelectedListener() {
        @Override
        public void onTabSelected(TabLayout.Tab tab) {
            mViewPager.setCurrentItem(tab.getPosition());
        }

        @Override
        public void onTabUnselected(TabLayout.Tab tab) {
            // don't care
        }

        @Override
        public void onTabReselected(TabLayout.Tab tab) {
            // don't care
        }
    };

    /**
     * Iterate through all the available stations and pick out the ones
     * to display. A station with a 'hidden' option from the server will not be shown
     * in the tabs unless it was specifically requested in the Intent
     * via the EXTRA_VISIBLE_STATION_LIST or EXTRA_UNHIDE_STATION_LIST
     *
     * @return the index of the selected tab
     */

    private Station[] collectStations() {
        Station activeStation = mPlayer.getActiveStation();
        LinkedList<Station> visibleStations = new LinkedList<Station>();

        Intent ai = getIntent();
        if (ai.hasExtra(EXTRA_VISIBLE_STATION_LIST)) {
            // caller provided us with the explicit list of stations to display
            ArrayList<String> visibleStationNames = ai.getStringArrayListExtra(EXTRA_VISIBLE_STATION_LIST);

            for (int i = 0; i < visibleStationNames.size(); i++) {
                Station station = getStationWithName(visibleStationNames.get(i), mPlayer.getStationList());

                if (station != null) {
                    visibleStations.add(station);
                }
            }

        } else {
            // collect all stations except for those that have 'hidden=true' option
            // and are not in the EXTRA_UNHIDE_STATION_LIST.
            HashSet<String> unhide = new HashSet<String>();
            if (ai.hasExtra(EXTRA_UNHIDE_STATION_LIST)) {
                unhide.addAll(ai.getStringArrayListExtra(EXTRA_UNHIDE_STATION_LIST));
            }

            if (mPlayer.getStationList() != null) {
                for (Station station : mPlayer.getStationList()) {
                    Object hidden = station.getOption("hidden");

                    Log.i(TAG, "debugging station " + station.getName() + ", " + station.getOption("mobile_app_backgrounds"));


                    if ((hidden == null) ||
                            ((hidden instanceof Boolean) && (!(Boolean) hidden)) ||
                            unhide.contains(station.getName())) {

                        visibleStations.add(station);
                    }
                }
            }
        }
        return visibleStations.toArray(new Station[visibleStations.size()]);
    }

    /**
     * Return the index of the station that should be visible when
     * the activity starts.
     *
     * @param stations the list of available stations
     * @return index into returned array of station to display on activity startup
     */

    private int selectDefaultStation(Station[] stations) {

        Intent ai = getIntent();

        Station activeStation = mPlayer.getActiveStation();

        String displayStation = ai.getStringExtra(EXTRA_DEFAULT_STATION);
        int displayStationTabIndex = -1;
        int activeStationTabIndex = 0;

        for (int i = 0; i < stations.length; i++) {
            Station station = stations[i];

            if (station.getId().equals(activeStation.getId())) {
                activeStationTabIndex = i;
            }

            if (station.getName().equals(displayStation)) {
                displayStationTabIndex = i;
            }
        }

        int selectedTab = (displayStationTabIndex > -1) ? displayStationTabIndex : activeStationTabIndex;

        return selectedTab;
    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.d(TAG, "fragment for station - activity resuming");

        // update the station selection buttons based on the current station
        FeedPlayerService.getInstance(new FeedAudioPlayer.AvailabilityListener() {
            @Override
            public void onPlayerAvailable(FeedAudioPlayer feedAudioPlayer) {
                setVolumeControlStream(AudioManager.STREAM_MUSIC);
            }

            @Override
            public void onPlayerUnavailable(Exception e) {
                Toast.makeText(PlayerActivity.this, "Sorry, music is not available in this location.", Toast.LENGTH_LONG).show();
            }

        });
    }

    /**
     * When the user tunes in to one of our stations, make sure the
     * notification_small that shows what is playing points to the tuned-in
     * station.
     *
     * @param station
     */

    @Override
    public void onStationStartedPlayback(Station station) {
        Log.d(TAG, "updating notification_small intent to point to " + station.getName());

        Intent ai = new Intent(getIntent());
        ai.putExtra(EXTRA_DEFAULT_STATION, station.getName());
        ai.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(this, 0, ai, PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_ONE_SHOT);
        mPlayer.setPendingIntent(pi);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (intent.hasExtra(EXTRA_DEFAULT_STATION)) {
            String name = intent.getStringExtra(EXTRA_DEFAULT_STATION);

            // find the index of this station and focus on it
            for (int i = 0; i < mVisibleStations.length; i++) {
                Station station = mVisibleStations[i];

                if (station.getName().equals(name)) {
                    mViewPager.setCurrentItem(i);
                }
            }
        }
    }

    /**
     * Display the 'powered by feed.fm' page
     */

    @Override
    public void onClickPoweredBy() {
        Intent ai = new Intent(this, PoweredByFeedActivity.class);
        startActivity(ai);
    }

    public static Station getStationWithName(String name, List<Station> stationList)
    {
        for (Station station: stationList)
        {
            if(station.getName().equals(name))
            {
                return station;
            }
        }
        return null;
    }
}
