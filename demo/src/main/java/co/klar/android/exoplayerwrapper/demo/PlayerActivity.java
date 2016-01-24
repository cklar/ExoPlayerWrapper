package co.klar.android.exoplayerwrapper.demo;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.FrameLayout;

import co.klar.android.exoplayerwrapper.demo.R;
import co.klar.android.exoplayerwrapper.SimpleVideoPlayer;
import co.klar.android.exoplayerwrapper.Video;


public class PlayerActivity extends AppCompatActivity {

    public static final String CONTENT_TYPE_EXTRA = "content_type";
    public static final String CONTENT_ID_EXTRA = "content_id";

    private SimpleVideoPlayer simpleVideoPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        Uri contentUri = intent.getData();
        int contentType = intent.getIntExtra(CONTENT_TYPE_EXTRA, -1);

        setContentView(R.layout.activity_player);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        FrameLayout root = (FrameLayout) findViewById(R.id.root_view);

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
}
