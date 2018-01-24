package fm.feed.android.radioplayer;

/**
 * Copyright Feed Media, 2017
 */

import fm.feed.android.playersdk.FeedPlayerService;
import com.crashlytics.android.Crashlytics;
import io.fabric.sdk.android.Fabric;

public class Application extends android.app.Application {

    public void onCreate() {
        super.onCreate();
        Fabric.with(this, new Crashlytics());

        // initialize player
        FeedPlayerService.initialize(getApplicationContext(),"demo", "demo");
    }

}
