package fm.feed.android.radioplayer;

/**
 * Copyright Feed Media, 2017
 */

import fm.feed.android.playersdk.FeedPlayerService;

public class Application extends android.app.Application {

    public void onCreate() {
        super.onCreate();

        // initialize player
        FeedPlayerService.initialize(getApplicationContext(),"demo", "demo");
    }

}
