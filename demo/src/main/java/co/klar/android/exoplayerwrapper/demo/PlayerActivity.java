package co.klar.android.exoplayerwrapper.demo;

import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.widget.FrameLayout;

import butterknife.Bind;
import butterknife.ButterKnife;
import co.klar.android.exoplayerwrapper.SimpleVideoPlayer;
import co.klar.android.exoplayerwrapper.Video;
import co.klar.android.exoplayerwrapper.util.ViewGroupUtils;


public class PlayerActivity extends AppCompatActivity {

    private static final String TAG = PlayerActivity.class.getSimpleName();

    public static final String CONTENT_TYPE_EXTRA = "content_type";
    public static final String CONTENT_ID_EXTRA = "content_id";

    private SimpleVideoPlayer simpleVideoPlayer;

    @Bind(R.id.root_view)
    FrameLayout root;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);
//        setContentView(R.layout.activity_player_fitwindow);
        ButterKnife.bind(this);

        Intent intent = getIntent();
        Uri contentUri = intent.getData();
        int contentType = intent.getIntExtra(CONTENT_TYPE_EXTRA, -1);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);


        Video video = new Video(contentUri.toString(), contentType);

        simpleVideoPlayer = new SimpleVideoPlayer(this, root, video);
    }

    //Call the three lifecycle methods

    @Override
    protected void onPause() {
        super.onPause();
        simpleVideoPlayer.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        simpleVideoPlayer.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        simpleVideoPlayer.onResume();
        actionbarSwitch();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.d(TAG, newConfig.toString());
        actionbarSwitch();


    }

    /**
     * Applies the correct flags to the windows decor view to enter
     * or exit fullscreen mode
     *
     */
    private void actionbarSwitch() {
        if(getSupportActionBar() == null){
            return;
        }

        if (ViewGroupUtils.isLandscape(this)) {
            getSupportActionBar().hide();
        }
        else {
            getSupportActionBar().show();
        }
    }



}
