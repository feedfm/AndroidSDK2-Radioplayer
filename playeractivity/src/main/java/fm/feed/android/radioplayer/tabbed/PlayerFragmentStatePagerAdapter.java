package fm.feed.android.radioplayer.tabbed;


import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import fm.feed.android.playersdk.models.Station;


/**
 * Copyright Feed Media, 2017
 * This adapter returns PlayerFragment instances.
 *
 */

public class PlayerFragmentStatePagerAdapter extends FragmentStatePagerAdapter {

    private Station[] mStations;
    private PlayerFragment.PlayerFragmentEventListener mPlayerFragmentEventListener;

    public PlayerFragmentStatePagerAdapter(FragmentManager fm, Station[] stations, PlayerFragment.PlayerFragmentEventListener spListener) {
        super(fm);

        mStations = stations;
        mPlayerFragmentEventListener = spListener;
    }

    @Override
    public Fragment getItem(int position) {
        return PlayerFragment.newInstance(mStations[position].getName(), mPlayerFragmentEventListener);
    }

    @Override
    public int getCount() {
        return mStations.length;
    }
}
