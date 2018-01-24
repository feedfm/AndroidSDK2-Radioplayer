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
        FeedPlayerService.initialize(getApplicationContext(),"35953db2c82c4956bc2053bb1425282851f6be67", "581fb939e63be7925b9c4c9518566e274a0b9de0");
    }

}
