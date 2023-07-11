package fm.feed.android.radioplayer.tabbed;

/**
 * Copyright Feed Media, 2017
 */

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;


import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;

import java.text.NumberFormat;

import fm.feed.android.playersdk.*;
import fm.feed.android.playersdk.models.AudioFile;
import fm.feed.android.playersdk.models.Play;
import fm.feed.android.playersdk.models.Station;

/**
 *
 * Simple fragment to render a music station.
 *
 * This fragment must be provided with a station name that the fragment
 * will be rendering. The newInstance() convenience methods handle that
 * for you.
 *
 * The fragment searches for a resource that is an array of drawables
 * that will be periodically swapped out in the fragment. The array
 * has an id that is
 * constructed by converting the station name to lower case, removing
 * all non-alphanumeric characters, converting strings of spaces to
 * a single '_', and then appending "_backgrounds". So, "My Cool  Station"
 * is converted to "my_cool_station_backgrounds" and and we expect
 * to find an array of drawables. If no resource with that id is found,
 * we look for "default_backgrounds".
 *
 * The fragment has three viewgroups that it makes visible at different times:
 *   tuneInView - this is displayed when the current station is not
 *      being played.
 *   tuningView - this is displayed when we are stalled waiting for audio data
 *      to arrive over the network.
 *   playerControlsView - this holds player controls that are revealed
 *      when this station is playing music in the current station.
 *
 */

public class PlayerFragment extends Fragment {

    public final static String TAG = PlayerFragment.class.getSimpleName();

    public final static String EXTRA_STATION_NAME = "fm.feed.android.radioplayer.stationName";

    /**
     * Frequency of background rotations
     */

    private static long MINIMUM_STATION_ROTATION_TIME_IN_SECONDS = 5;

    public FeedAudioPlayer mPlayer;
    private boolean mUserInteraction; // true if the user has tapped to request music
    private Station mStation;
    private boolean areListenersRegistered;
    private View mTuneInView;
    private View mTuningView;
    private View mPlayerControlsView;
    TextView trackText, ArtistText, elapsedText, remainingTextView;
    ImageButton likeButton, dislikeButton, playPauseButton, skipButton;
    private ProgressBar mProgressBar;
    ImageButton sb;
    private ImageView mBackgroundImageView;


    /**
     * When we change stations or run out of music, update
     * the displayed metadata.
     */

    StateListener stateListener = new StateListener() {
        @Override
        public void onStateChanged(State state) {


            displayMetadataGroupOrNot();
            if(state == State.PLAYING)
            {
                playPauseButton.setImageResource(R.drawable.feedfm_ic_pause_36dp);
            }
            else if(state == State.PAUSED)
            {
                playPauseButton.setImageResource(R.drawable.feedfm_ic_play_36dp);
            }

        }
    };

    PlayListener playListener = new PlayListener() {

        @Override
        public void onSkipStatusChanged(boolean b) {
            skipButton.setEnabled(b);
        }

        @Override
        public void onProgressUpdate(@NonNull Play play, float elapsedTime, float duration) {

            long elapsedMinutes = (long) elapsedTime / 60;
            int elapsedSeconds = (int) elapsedTime % 60;

            NumberFormat f = NumberFormat.getNumberInstance();
            f.setMinimumIntegerDigits(2);
            f.setMaximumIntegerDigits(2);
            elapsedText.setText((f.format(elapsedMinutes) + ":" + f.format(elapsedSeconds)));

            long durationMinutes = (long) (duration - elapsedTime) / 60;
            int durationSeconds = (int) (duration - elapsedTime) % 60;
            remainingTextView.setText((f.format(durationMinutes) + ":" + f.format(durationSeconds)));

            mProgressBar.setMax((int) (duration * 1000));
            mProgressBar.setProgress((int) elapsedTime * 1000);
        }

        @Override
        public void onPlayStarted(Play play) {
            trackText.setText(play.getAudioFile().getTrack().getTitle());
            ArtistText.setText(play.getAudioFile().getArtist().getName());
            likeStatusChangeListener.onLikeStatusChanged(play.getAudioFile());
            displayMetadataGroupOrNot();
        }
        @Override
        public void onPlayerError(FeedFMError error){

        }


    };

    LikeStatusChangeListener likeStatusChangeListener = new LikeStatusChangeListener() {
        @Override
        public void onLikeStatusChanged(AudioFile audioFile) {
            if (audioFile.isDisliked()) {
                dislikeButton.setImageResource(R.drawable.feedfm_ic_thumbsdown_36dp);
            } else if (audioFile.isLiked()) {
                likeButton.setImageResource(R.drawable.feedfm_ic_thumbsup_36dp);
            } else {
                likeButton.setImageResource(R.drawable.feedfm_ic_thumbsup_hollow_36dp);
                dislikeButton.setImageResource(R.drawable.feedfm_ic_thumbsdown_hollow_36dp);
            }
        }
    };

    /**
     * This listener is called when various things in the station
     * fragment are poked.
     */

    public interface PlayerFragmentEventListener {
        void onStationStartedPlayback(Station station);
        void onClickPoweredBy();
    }

    private PlayerFragmentEventListener mPlayerFragmentEventListener;

    public static PlayerFragment newInstance(String name, PlayerFragmentEventListener psListener) {
        PlayerFragment pf = new PlayerFragment();
        Bundle args = new Bundle(1);
        args.putString(EXTRA_STATION_NAME, name);
        pf.setArguments(args);

        if (psListener != null) {
            pf.mPlayerFragmentEventListener = psListener;
        }

        return pf;
    }

    public static PlayerFragment newInstance(String name) {
        return PlayerFragment.newInstance(name, null);
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {

            FeedPlayerService.getInstance(new AvailabilityListener() {
                @Override
                public void onPlayerAvailable(FeedAudioPlayer feedAudioPlayer) {

                    mPlayer = feedAudioPlayer;
                    Bundle args = getArguments();
                    if (args != null) {

                        String stationName = args.getString(EXTRA_STATION_NAME);
                        mStation = PlayerActivity.getStationWithName(stationName, mPlayer.getStationList());

                        if (mStation == null) {
                            Log.w(TAG, "PlayerFragment created with station name '" + stationName + "', but no such station exists");
                            // default to active station to keep this boat floatin'
                            mStation = mPlayer.getActiveStation();
                        }


                        State state = mPlayer.getState();
                        mUserInteraction = (state != State.READY_TO_PLAY) && (state != State.STALLED);
                        assignBackground();

                        registerListeners();
                        areListenersRegistered = true;
                        displayMetadataGroupOrNot();
                    }
                }

                @Override
                public void onPlayerUnavailable(Exception e) {

                }
            });
        }

        if(mStation != null)
        {
            assignBackground();
        }
        displayMetadataGroupOrNot();

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


    }


    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_player, container, false);

        mTuneInView = rootView.findViewById(R.id.tuneInView);
        mTuningView = rootView.findViewById(R.id.tuningView);
        mPlayerControlsView = rootView.findViewById(R.id.playerControlsView);

        mBackgroundImageView = (ImageView) rootView.findViewById(R.id.backgroundImageView);
        mProgressBar = (ProgressBar) rootView.findViewById(R.id.progressBar);

        skipButton = (ImageButton) rootView.findViewById(R.id.skipButton);
        skipButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                mPlayer.skip();
            }
        });
        likeButton = (ImageButton) rootView.findViewById(R.id.likeButton);
        likeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mPlayer.getCurrentPlay() != null) {
                    if (!mPlayer.getCurrentPlay().getAudioFile().isLiked()) {
                        mPlayer.like();
                    } else {
                        mPlayer.unlike();
                    }
                }
            }
        });
        dislikeButton = (ImageButton) rootView.findViewById(R.id.dislikeButton);
        dislikeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mPlayer.getCurrentPlay() != null) {
                    if (!mPlayer.getCurrentPlay().getAudioFile().isDisliked()) {
                        mPlayer.dislike();
                    } else {
                        mPlayer.unlike();
                    }
                }
            }
        });
        playPauseButton = (ImageButton) rootView.findViewById(R.id.playButton);
        playPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mPlayer.getState() != State.PLAYING) {
                    mPlayer.play();
                } else if (mPlayer.getState() == State.PLAYING) {
                    mPlayer.pause();
                }
            }
        });
        trackText = (TextView) rootView.findViewById(R.id.trackText);
        ArtistText = (TextView) rootView.findViewById(R.id.artistText);
        elapsedText = (TextView) rootView.findViewById(R.id.elapsedTextView);
        remainingTextView = (TextView) rootView.findViewById(R.id.remainingTextView);

        // station description
        TextView description = (TextView) rootView.findViewById(R.id.description);

        description.setText((String) "Tune in!");

        // 'powered by feed.fm' link
        Button poweredByText = (Button) rootView.findViewById(R.id.powered_by);
        poweredByText.setOnClickListener(onClickPoweredByText);

        // clicking on this button will now start playback
        // in this station
        sb = (ImageButton) rootView.findViewById(R.id.startButton);
        sb.setOnClickListener(onClickStationButton);

        //sb.(mStation.getName());

        return rootView;
    }

    /**
     * Make a note when the user clicks to start music playback
     * in the current station.
     */

    private View.OnClickListener onClickStationButton = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            // if the player is TUNING but the user hasn't interacted with
            // anything yet, then assume we're pre-loading stuff in the background
            // and don't show the spinner.
            mUserInteraction = true;

            if (mPlayerFragmentEventListener != null) {
                if(mPlayer != null)
                {
                    mPlayer.setActiveStation(mStation, true);
                    mPlayer.play();
                    mPlayerFragmentEventListener.onStationStartedPlayback(mStation);
                }
            }
        }
    };

    private final View.OnClickListener onClickPoweredByText = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mPlayerFragmentEventListener != null) {
                mPlayerFragmentEventListener.onClickPoweredBy();
            }
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        if(mPlayer!= null)
        {
            registerListeners();
            if(mPlayer.getCurrentPlay()!=null) {
                stateListener.onStateChanged(mPlayer.getState());
                trackText.setText(mPlayer.getCurrentPlay().getAudioFile().getTrack().getTitle());
                ArtistText.setText(mPlayer.getCurrentPlay().getAudioFile().getArtist().getName());
                likeStatusChangeListener.onLikeStatusChanged(mPlayer.getCurrentPlay().getAudioFile());
            }
        }
        if(mStation != null)
        {
            assignBackground();

            displayMetadataGroupOrNot();
        }

        // update the view when we switch stations
    }

    @Override
    public void onPause() {
        super.onPause();
        if(mPlayer!= null) {
            unregisterListeners();
        }
    }

    /**
     * Determine which of the tuneInView, tuningView, or playerControlsView
     * do display
     */

    private void registerListeners()
    {
        if(mPlayer != null && !areListenersRegistered) {
            mPlayer.addStateListener(stateListener);
            mPlayer.addLikeStatusChangeListener(likeStatusChangeListener);
            mPlayer.addPlayListener(playListener);
        }
    }
    private void unregisterListeners()
    {
        mPlayer.removePlayListener(playListener);
        mPlayer.removeStateListener(stateListener);
        mPlayer.removeLikeStatusChangeListener(likeStatusChangeListener);
        areListenersRegistered = false;
    }

    private void displayMetadataGroupOrNot() {
        if(mTuneInView == null)
        {
            return;
        }
        Station station = mPlayer.getActiveStation();
        State state = mPlayer.getState();

        Log.i(TAG, "determining metadata display, state is " + state.name());

        if (!mUserInteraction
                || (station == null)
                || (state == State.READY_TO_PLAY)
                || (state == State.WAITING_FOR_ITEM)) {
            Log.i(TAG, "showing tune in text, state is " + state);

            // display 'tune in! text
            mTuneInView.setVisibility(View.VISIBLE);
            mTuningView.setVisibility(View.INVISIBLE);
            mPlayerControlsView.setVisibility(View.INVISIBLE);
            mProgressBar.setVisibility(View.INVISIBLE);

        } else if (!station.getId().equals(mStation.getId())) {
            Log.i(TAG, "showing tune in text, since station does not match");

            // display controls for non-tuned in stations
            mTuneInView.setVisibility(View.VISIBLE);
            mTuningView.setVisibility(View.INVISIBLE);
            mPlayerControlsView.setVisibility(View.INVISIBLE);
            mProgressBar.setVisibility(View.INVISIBLE);

        } else if (state == State.STALLED) {
            Log.i(TAG,"showing 'tuning...' text since we're tuning");

            // show tuning text if that's what we're doing
            mTuneInView.setVisibility(View.INVISIBLE);
            mTuningView.setVisibility(View.VISIBLE);
            mPlayerControlsView.setVisibility(View.INVISIBLE);
            mProgressBar.setVisibility(View.INVISIBLE);

        } else if(state == State.PLAYING || state == State.PAUSED){
            Log.i(TAG, "showing controls");

            // display controls
            mTuneInView.setVisibility(View.INVISIBLE);
            mTuningView.setVisibility(View.INVISIBLE);
            mPlayerControlsView.setVisibility(View.VISIBLE);
            mProgressBar.setVisibility(View.VISIBLE);
            assignLockScreen();

        }
    }

    private void assignBackground() {
        // find a bitmap and assign it to 'bm'
        String bgUrl;

        try {
            bgUrl = (String) mStation.getOption("background_image_url");
        } catch (ClassCastException e) {
            bgUrl = null;
        }

        if (bgUrl != null && mBackgroundImageView != null) {

            try {
                Glide.with(getContext()).load(bgUrl).centerCrop().into(mBackgroundImageView);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        } else if(mBackgroundImageView != null){
            Bitmap bm = BitmapFactory.decodeResource(getResources(), R.drawable.default_station_background);

            // update background image
            mBackgroundImageView.setImageBitmap(bm);
        }
    }

    private void assignLockScreen() {
        String bgUrl;
        try {
            bgUrl = (String) mStation.getOption("background_image_url");
        } catch (ClassCastException e) {
            bgUrl = null;
        }

        if (bgUrl != null) {

            Glide.with(getContext()).load(bgUrl).asBitmap().into(new SimpleTarget<Bitmap>() {
                @Override
                public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                    // set this image for the lockscreen and notifications if we're
                    // playing this station

                }
            });
        } else {
            Bitmap bm = BitmapFactory.decodeResource(getResources(), R.drawable.default_station_background);

            //mPlayer.setArtwork(bm);
        }
    }



}
