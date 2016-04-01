package co.klar.android.exoplayerwrapper.demo;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;

import butterknife.Bind;
import butterknife.ButterKnife;
import co.klar.android.exoplayerwrapper.demo.R;
import co.klar.android.exoplayerwrapper.SimpleVideoPlayer;
import co.klar.android.exoplayerwrapper.Video;


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
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            setUiFlags(true);

        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
            setUiFlags(false);

        }
    }

    /**
     * Applies the correct flags to the windows decor view to enter
     * or exit fullscreen mode
     *
     * @param fullscreen True if entering fullscreen mode
     */
    private void setUiFlags(boolean fullscreen) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            View decorView = getWindow().getDecorView();
            if (decorView != null) {
                decorView.setSystemUiVisibility(fullscreen ? getFullscreenUiFlags() : View.SYSTEM_UI_FLAG_VISIBLE);
            }
        }
        if (fullscreen){
            getSupportActionBar().hide();
        } else {
            getSupportActionBar().show();
        }
    }

    /**
     * Determines the appropriate fullscreen flags based on the
     * systems API version.
     *
     * @return The appropriate decor view flags to enter fullscreen mode when supported
     */
    private int getFullscreenUiFlags() {
        int flags = View.SYSTEM_UI_FLAG_LOW_PROFILE | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            flags |= View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    ;
        }
        return flags;
    }

}
