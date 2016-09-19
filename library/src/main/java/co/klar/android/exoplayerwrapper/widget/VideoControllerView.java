package co.klar.android.exoplayerwrapper.widget;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.google.android.exoplayer.util.PlayerControl;

import java.lang.ref.WeakReference;
import java.util.Formatter;
import java.util.Locale;

import co.klar.android.exoplayerwrapper.R;
import timber.log.Timber;

/**
 * A view containing controls for a MediaPlayer. Typically contains the
 * buttons like "Play/Pause", "Rewind", "Fast Forward" and a progress
 * slider. It takes care of synchronizing the controls with the state
 * of the MediaPlayer.
 * <p>
 * The way to use this class is to instantiate it programatically.
 * The MediaController will create a default set of controls
 * and put them in a window floating above your application. Specifically,
 * the controls will float above the view specified with setAnchorView().
 * The window will disappear if left idle for three seconds and reappear
 * when the user touches the anchor view.
 * <p>
 * Functions like show() and hide() have no effect when MediaController
 * is created in an xml layout.
 * <p>
 * MediaController will hide and
 * show the buttons according to these rules:
 * <ul>
 * <li> The "previous" and "next" buttons are hidden until setPrevNextListeners()
 * has been called
 * <li> The "previous" and "next" buttons are visible but disabled if
 * setPrevNextListeners() was called with null listeners
 * <li> The "rewind" and "fastforward" buttons are shown unless requested
 * otherwise by using the MediaController(Context, boolean) constructor
 * with the boolean set to false
 * </ul>
 */
public class VideoControllerView extends FrameLayout {
    private static final String TAG = "VideoControllerView";

    private PlayerControl playerControl;
    private Activity activity;
    private ViewGroup anchorViewGroup;
    private View rootView;
    private ProgressBar progressBar;
    private TextView endTimeTextView, currentTimeTextView;
    private boolean isShowing;
    private boolean isDragging;
    private static final int DEFAULT_TIMEOUT = 3000;
    private static final int FADE_OUT = 1;
    private static final int SHOW_PROGRESS = 2;
    private boolean useFastForward;
    private boolean fromXml;
    private boolean nextListenersSet;
    private View.OnClickListener nextButtonListener, prevButtonListener;
    StringBuilder timeFormatBuilder;
    Formatter timeFormatter;
    private ImageView pauseButton;
    private ImageView ffwdButton;
    private ImageView rewButton;
    private ImageView nextButton;
    private ImageView prevButton;
    private ImageView fullscreenButton;
    private Handler messageHandler = new MessageHandler(this);

    @Deprecated
    private boolean isFullscreen;

    public VideoControllerView(Activity activity, AttributeSet attrs) {
        super(activity, attrs);
        rootView = null;
        this.activity = activity;
        useFastForward = true;
        fromXml = true;

        Log.i(TAG, TAG);
    }

    public VideoControllerView(Activity activity, boolean useFastForward) {
        super(activity);
        this.activity = activity;
        this.useFastForward = useFastForward;

        Log.i(TAG, TAG);
    }

    public VideoControllerView(Activity activity) {
        this(activity, true);

        Log.i(TAG, TAG);
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();
        if (rootView != null)
            initControllerView(rootView);
    }

    public void setMediaPlayer(PlayerControl player) {
        playerControl = player;
        updatePausePlay();
        updateFullScreen();
    }

    /**
     * Set the view that acts as the anchor for the control view.
     * This can for example be a VideoView, or your Activity's main view.
     *
     * @param view The view to which to anchor the controller when it is visible.
     */
    public void setAnchorView(ViewGroup view) {
        anchorViewGroup = view;

        FrameLayout.LayoutParams frameParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );

        removeAllViews();
        View v = makeControllerView();
        addView(v, frameParams);
    }

    /**
     * Create the view that holds the widgets that control playback.
     * Derived classes can override this to create their own.
     *
     * @return The controller view.
     * @hide This doesn't work as advertised
     */
    protected View makeControllerView() {
        LayoutInflater inflate = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//        rootView = inflate.inflate(R.layout.media_controller, null);
        rootView = inflate.inflate(R.layout.video_controls_overlay, null);

        initControllerView(rootView);

        return rootView;
    }

    private void initControllerView(View v) {
        pauseButton = (ImageView) v.findViewById(R.id.pause);
        if (pauseButton != null) {
            pauseButton.requestFocus();
            pauseButton.setOnClickListener(mPauseListener);
        }

        fullscreenButton = (ImageView) v.findViewById(R.id.fullscreen);
        if (fullscreenButton != null) {
            fullscreenButton.requestFocus();
            fullscreenButton.setOnClickListener(mFullscreenListener);
        }

        ffwdButton = (ImageView) v.findViewById(R.id.ffwd);
        if (ffwdButton != null) {
            ffwdButton.setOnClickListener(mFfwdListener);
            if (!fromXml) {
                ffwdButton.setVisibility(useFastForward ? View.VISIBLE : View.GONE);
            }
        }

        rewButton = (ImageView) v.findViewById(R.id.rew);
        if (rewButton != null) {
            rewButton.setOnClickListener(mRewListener);
            if (!fromXml) {
                rewButton.setVisibility(useFastForward ? View.VISIBLE : View.GONE);
            }
        }

        // By default these are hidden. They will be enabled when setPrevNextListeners() is called
        nextButton = (ImageView) v.findViewById(R.id.next);
        if (nextButton != null && !fromXml && !nextListenersSet) {
            nextButton.setVisibility(View.GONE);
        }
        prevButton = (ImageView) v.findViewById(R.id.prev);
        if (prevButton != null && !fromXml && !nextListenersSet) {
            prevButton.setVisibility(View.GONE);
        }

        progressBar = (SeekBar) v.findViewById(R.id.mediacontroller_progress);
        if (progressBar != null) {
            if (progressBar instanceof SeekBar) {
                SeekBar seeker = (SeekBar) progressBar;
                seeker.setOnSeekBarChangeListener(mSeekListener);
            }
            progressBar.setMax(1000);
        }

        v.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO check
                hide();
            }
        });

        endTimeTextView = (TextView) v.findViewById(R.id.time);
        currentTimeTextView = (TextView) v.findViewById(R.id.time_current);
        timeFormatBuilder = new StringBuilder();
        timeFormatter = new Formatter(timeFormatBuilder, Locale.getDefault());

        installPrevNextListeners();
    }

    /**
     * Show the controller on screen. It will go away
     * automatically after 3 seconds of inactivity.
     */
    public void show() {
        show(DEFAULT_TIMEOUT);
    }

    /**
     * Disable pause or seek buttons if the stream cannot be paused or seeked.
     * This requires the control interface to be a MediaPlayerControlExt
     */
    private void disableUnsupportedButtons() {
        if (playerControl == null) {
            return;
        }

        try {
            if (pauseButton != null && !playerControl.canPause()) {
                pauseButton.setEnabled(false);
            }
            if (rewButton != null && !playerControl.canSeekBackward()) {
                rewButton.setEnabled(false);
            }
            if (ffwdButton != null && !playerControl.canSeekForward()) {
                ffwdButton.setEnabled(false);
            }
        } catch (IncompatibleClassChangeError ex) {
            // We were given an old version of the interface, that doesn't have
            // the canPause/canSeekXYZ methods. This is OK, it just means we
            // assume the media can be paused and seeked, and so we don't disable
            // the buttons.
        }
    }

    /**
     * Show the controller on screen. It will go away
     * automatically after 'timeout' milliseconds of inactivity.
     *
     * @param timeout The timeout in milliseconds. Use 0 to show
     *                the controller until hide() is called.
     */
    public void show(int timeout) {
        if (!isShowing && anchorViewGroup != null) {
            setProgress();
            if (pauseButton != null) {
                pauseButton.requestFocus();
            }
            disableUnsupportedButtons();

            FrameLayout.LayoutParams tlp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.BOTTOM
            );

            anchorViewGroup.addView(this, tlp);
            isShowing = true;
        }
        updatePausePlay();
        updateFullScreen();

        // cause the progress bar to be updated even if isShowing
        // was already true.  This happens, for example, if we're
        // paused with the progress bar showing the user hits play.
        messageHandler.sendEmptyMessage(SHOW_PROGRESS);

        Message msg = messageHandler.obtainMessage(FADE_OUT);
        if (timeout != 0) {
            messageHandler.removeMessages(FADE_OUT);
            messageHandler.sendMessageDelayed(msg, timeout);
        }
    }

    public boolean isShowing() {
        return isShowing;
    }

    /**
     * Remove the controller from the screen.
     */
    public void hide() {
        if (anchorViewGroup == null) {
            return;
        }

        try {
            anchorViewGroup.removeView(this);
            messageHandler.removeMessages(SHOW_PROGRESS);
        } catch (IllegalArgumentException ex) {
            Log.w("MediaController", "already removed");
        }
        isShowing = false;
    }

    private String stringForTime(int timeMs) {
        int totalSeconds = timeMs / 1000;

        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours = totalSeconds / 3600;

        timeFormatBuilder.setLength(0);
        if (hours > 0) {
            return timeFormatter.format("%d:%02d:%02d", hours, minutes, seconds).toString();
        } else {
            return timeFormatter.format("%02d:%02d", minutes, seconds).toString();
        }
    }

    private int setProgress() {
        if (playerControl == null || isDragging) {
            return 0;
        }

        int position = playerControl.getCurrentPosition();
        int duration = playerControl.getDuration();
        if (progressBar != null) {
            if (duration > 0) {
                // use long to avoid overflow
                long pos = 1000L * position / duration;
                progressBar.setProgress((int) pos);
            }
            int percent = playerControl.getBufferPercentage();
            progressBar.setSecondaryProgress(percent * 10);
        }

        if (endTimeTextView != null)
            endTimeTextView.setText(stringForTime(duration));
        if (currentTimeTextView != null)
            currentTimeTextView.setText(stringForTime(position));

        return position;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        show(DEFAULT_TIMEOUT);
        return true;
    }

    @Override
    public boolean onTrackballEvent(MotionEvent ev) {
        show(DEFAULT_TIMEOUT);
        return false;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (playerControl == null) {
            return true;
        }

        int keyCode = event.getKeyCode();
        final boolean uniqueDown = event.getRepeatCount() == 0
                && event.getAction() == KeyEvent.ACTION_DOWN;
        if (keyCode == KeyEvent.KEYCODE_HEADSETHOOK
                || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                || keyCode == KeyEvent.KEYCODE_SPACE) {
            if (uniqueDown) {
                doPauseResume();
                show(DEFAULT_TIMEOUT);
                if (pauseButton != null) {
                    pauseButton.requestFocus();
                }
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY) {
            if (uniqueDown && !playerControl.isPlaying()) {
                playerControl.start();
                updatePausePlay();
                show(DEFAULT_TIMEOUT);
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_MEDIA_STOP
                || keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE) {
            if (uniqueDown && playerControl.isPlaying()) {
                playerControl.pause();
                updatePausePlay();
                show(DEFAULT_TIMEOUT);
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
                || keyCode == KeyEvent.KEYCODE_VOLUME_UP
                || keyCode == KeyEvent.KEYCODE_VOLUME_MUTE) {
            // don't show the controls for volume adjustment
            return super.dispatchKeyEvent(event);
        } else if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_MENU) {
            if (uniqueDown) {
                hide();
            }
            return true;
        } else if (playerControl.canSeekForward() && keyCode == KeyEvent.KEYCODE_MEDIA_FAST_FORWARD) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                playerControl.seekTo(playerControl.getCurrentPosition() + 15000); // milliseconds
                show();
            }
            return true;
        } else if (playerControl.canSeekBackward() && keyCode == KeyEvent.KEYCODE_MEDIA_REWIND) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                playerControl.seekTo(playerControl.getCurrentPosition() - 5000); // milliseconds
                show();
            }
            return true;
        }

        show(DEFAULT_TIMEOUT);
        return super.dispatchKeyEvent(event);
    }

    private View.OnClickListener mPauseListener = new View.OnClickListener() {
        public void onClick(View v) {
            doPauseResume();
            show(DEFAULT_TIMEOUT);
        }
    };

    private View.OnClickListener mFullscreenListener = new View.OnClickListener() {
        public void onClick(View v) {
            onClickFullscreen();
            show(DEFAULT_TIMEOUT);
        }
    };

    public void updatePausePlay() {
        if (rootView == null || pauseButton == null || playerControl == null) {
            return;
        }

        if (playerControl.isPlaying()) {
            pauseButton.setImageResource(android.R.drawable.ic_media_pause);
        } else {
            pauseButton.setImageResource(android.R.drawable.ic_media_play);
        }
    }

    public void updateFullScreen() {
        if (rootView == null || fullscreenButton == null || playerControl == null || activity == null) {
            return;
        }
        View decorView = activity.getWindow().getDecorView();

        if (decorView == null) {
            return;
        }
        if (isFullscreen()) {
            decorView.setSystemUiVisibility(getFullscreenUiFlags());
            fullscreenButton.setImageResource(R.drawable.ic_fullscreen_exit);
        } else {
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
            fullscreenButton.setImageResource(R.drawable.ic_fullscreen);
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

    private void doPauseResume() {
        if (playerControl == null) {
            return;
        }

        if (playerControl.isPlaying()) {
            playerControl.pause();
        } else {
            playerControl.start();
        }
        updatePausePlay();
    }

    private void onClickFullscreen() {
        if (playerControl == null) {
            return;
        }

        if (isFullscreen()) {
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else {
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        Timber.d("onClickFullscreen isFullScreen(): " + isFullscreen());
        updateFullScreen();
    }


    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Timber.d("onConfigurationChanged: ORIENTATION_LANDSCAPE");
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            Timber.d("onConfigurationChanged: ORIENTATION_PORTRAIT");
        } else {
            return;
        }
        Timber.d("onConfigurationChanged isFullScreen(): " + isFullscreen());
        updateFullScreen();
    }

    private boolean isFullscreen() {
        return getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
    }

    // There are two scenarios that can trigger the seekbar listener to trigger:
    //
    // The first is the user using the touchpad to adjust the posititon of the
    // seekbar's thumb. In this case onStartTrackingTouch is called followed by
    // a number of onProgressChanged notifications, concluded by onStopTrackingTouch.
    // We're setting the field "isDragging" to true for the duration of the dragging
    // session to avoid jumps in the position in case of ongoing playback.
    //
    // The second scenario involves the user operating the scroll ball, in this
    // case there WON'T BE onStartTrackingTouch/onStopTrackingTouch notifications,
    // we will simply apply the updated position without suspending regular updates.
    private OnSeekBarChangeListener mSeekListener = new OnSeekBarChangeListener() {
        public void onStartTrackingTouch(SeekBar bar) {
            show(3600000);

            isDragging = true;

            // By removing these pending progress messages we make sure
            // that a) we won't update the progress while the user adjusts
            // the seekbar and b) once the user is done dragging the thumb
            // we will post one of these messages to the queue again and
            // this ensures that there will be exactly one message queued up.
            messageHandler.removeMessages(SHOW_PROGRESS);
        }

        public void onProgressChanged(SeekBar bar, int progress, boolean fromuser) {
            if (playerControl == null) {
                return;
            }

            if (!fromuser) {
                // We're not interested in programmatically generated changes to
                // the progress bar's position.
                return;
            }

            long duration = playerControl.getDuration();
            long newposition = (duration * progress) / 1000L;
            playerControl.seekTo((int) newposition);
            if (currentTimeTextView != null)
                currentTimeTextView.setText(stringForTime((int) newposition));
        }

        public void onStopTrackingTouch(SeekBar bar) {
            isDragging = false;
            setProgress();
            updatePausePlay();
            show(DEFAULT_TIMEOUT);

            // Ensure that progress is properly updated in the future,
            // the call to show() does not guarantee this because it is a
            // no-op if we are already showing.
            messageHandler.sendEmptyMessage(SHOW_PROGRESS);
        }
    };

    @Override
    public void setEnabled(boolean enabled) {
        if (pauseButton != null) {
            pauseButton.setEnabled(enabled);
        }
        if (ffwdButton != null) {
            ffwdButton.setEnabled(enabled);
        }
        if (rewButton != null) {
            rewButton.setEnabled(enabled);
        }
        if (nextButton != null) {
            nextButton.setEnabled(enabled && nextButtonListener != null);
        }
        if (prevButton != null) {
            prevButton.setEnabled(enabled && prevButtonListener != null);
        }
        if (progressBar != null) {
            progressBar.setEnabled(enabled);
        }
        disableUnsupportedButtons();
        super.setEnabled(enabled);
    }

    private View.OnClickListener mRewListener = new View.OnClickListener() {
        public void onClick(View v) {
            if (playerControl == null) {
                return;
            }

            int pos = playerControl.getCurrentPosition();
            pos -= 5000; // milliseconds
            playerControl.seekTo(pos);
            setProgress();

            show(DEFAULT_TIMEOUT);
        }
    };

    private View.OnClickListener mFfwdListener = new View.OnClickListener() {
        public void onClick(View v) {
            if (playerControl == null) {
                return;
            }

            int pos = playerControl.getCurrentPosition();
            pos += 15000; // milliseconds
            playerControl.seekTo(pos);
            setProgress();

            show(DEFAULT_TIMEOUT);
        }
    };

    private void installPrevNextListeners() {
        if (nextButton != null) {
            nextButton.setOnClickListener(nextButtonListener);
            nextButton.setEnabled(nextButtonListener != null);
        }

        if (prevButton != null) {
            prevButton.setOnClickListener(prevButtonListener);
            prevButton.setEnabled(prevButtonListener != null);
        }
    }

    public void setPrevNextListeners(View.OnClickListener next, View.OnClickListener prev) {
        nextButtonListener = next;
        prevButtonListener = prev;
        nextListenersSet = true;

        if (rootView != null) {
            installPrevNextListeners();

            if (nextButton != null && !fromXml) {
                nextButton.setVisibility(View.VISIBLE);
            }
            if (prevButton != null && !fromXml) {
                prevButton.setVisibility(View.VISIBLE);
            }
        }
    }

    private static class MessageHandler extends Handler {
        private final WeakReference<VideoControllerView> mView;

        MessageHandler(VideoControllerView view) {
            mView = new WeakReference<VideoControllerView>(view);
        }

        @Override
        public void handleMessage(Message msg) {
            VideoControllerView view = mView.get();
            if (view == null || view.playerControl == null) {
                return;
            }

            int pos;
            switch (msg.what) {
                case FADE_OUT:
                    view.hide();
                    break;
                case SHOW_PROGRESS:
                    pos = view.setProgress();
                    if (!view.isDragging && view.isShowing && view.playerControl.isPlaying()) {
                        msg = obtainMessage(SHOW_PROGRESS);
                        sendMessageDelayed(msg, 1000 - (pos % 1000));
                    }
                    break;
            }
        }
    }
}