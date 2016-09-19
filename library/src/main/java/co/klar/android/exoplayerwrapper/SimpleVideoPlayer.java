package co.klar.android.exoplayerwrapper;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.net.Uri;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.CaptioningManager;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.google.android.exoplayer.ExoPlayer;
import com.google.android.exoplayer.audio.AudioCapabilities;
import com.google.android.exoplayer.audio.AudioCapabilitiesReceiver;
import com.google.android.exoplayer.drm.UnsupportedDrmException;
import com.google.android.exoplayer.metadata.id3.GeobFrame;
import com.google.android.exoplayer.metadata.id3.Id3Frame;
import com.google.android.exoplayer.metadata.id3.PrivFrame;
import com.google.android.exoplayer.metadata.id3.TxxxFrame;
import com.google.android.exoplayer.text.CaptionStyleCompat;
import com.google.android.exoplayer.text.Cue;
import com.google.android.exoplayer.text.SubtitleLayout;
import com.google.android.exoplayer.util.Util;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.List;

import co.klar.android.exoplayerwrapper.extractor.DashRendererBuilder;
import co.klar.android.exoplayerwrapper.extractor.ExoPlayerWrapper;
import co.klar.android.exoplayerwrapper.extractor.ExtractorRendererBuilder;
import co.klar.android.exoplayerwrapper.extractor.HlsRendererBuilder;
import co.klar.android.exoplayerwrapper.extractor.SmoothStreamingRendererBuilder;
import co.klar.android.exoplayerwrapper.util.EventLogger;
import co.klar.android.exoplayerwrapper.util.ViewGroupUtils;
import co.klar.android.exoplayerwrapper.widget.VideoControllerView;
import co.klar.android.exoplayerwrapper.widget.VideoTextureView;


/**
 * Created by cklar on 23.09.15.
 */
public class SimpleVideoPlayer implements SurfaceHolder.Callback,
        ExoPlayerWrapper.Listener, ExoPlayerWrapper.CaptionListener,
        ExoPlayerWrapper.Id3MetadataListener, AudioCapabilitiesReceiver.Listener, VideoTextureView.OnSizeChangeListener {


    private static final String TAG = "SimpleVideoPlayer";

    private static final CookieManager defaultCookieManager;

    static {
        defaultCookieManager = new CookieManager();
        defaultCookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER);
    }

    private Activity activity;
    private Video video;
    private boolean autoplay;

    private EventLogger eventLogger;
    private VideoControllerView mediaController;
    private View shutterView;
    private FrameLayout videoFrame;
//    private SurfaceView surfaceView;
    private VideoTextureView exoVideoTextureView;
    private SubtitleLayout subtitleLayout;

    private ExoPlayerWrapper wrapper;
    private boolean playerNeedsPrepare;

    private long playerPosition;
    private final boolean autoAspectRatio;

    private AudioCapabilitiesReceiver audioCapabilitiesReceiver;
    private boolean enableBackgroundAudio; //Not jet implemented

    public SimpleVideoPlayer(Activity activity,
                             FrameLayout root,
                             Video video) {
        this(activity, root, video, true, 0, true);
    }

    public SimpleVideoPlayer(Activity activity,
                             FrameLayout root,
                             Video video,
                             boolean autoplay,
                             int startPostitionMs,
                             boolean autoAspectRatio) {
        this.activity = activity;
        this.video = video;
        this.autoplay = autoplay;
        this.playerPosition = startPostitionMs;
        this.autoAspectRatio = autoAspectRatio;

        bindView(root);
    }

    private void bindView(FrameLayout oldRoot) {
        @SuppressLint("InflateParams")
        ViewGroup root = (ViewGroup) activity.getLayoutInflater().inflate(R.layout.player_view_layout, null);
        ViewGroupUtils.replaceView(oldRoot, root);

        root.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    toggleControlsVisibility();
                } else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    view.performClick();
                }
                return true;
            }
        });
        root.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_MENU) {
                    return false;
                }
                return mediaController.dispatchKeyEvent(event);
            }
        });

        audioCapabilitiesReceiver = new AudioCapabilitiesReceiver(activity.getApplicationContext(),
                this);

        shutterView = root.findViewById(R.id.shutter);

        videoFrame = (FrameLayout) root.findViewById(R.id.video_frame);
//        surfaceView = (SurfaceView) root.findViewById(R.id.surface_view);
        exoVideoTextureView = (VideoTextureView) root.findViewById(R.id.texture_view);
//        surfaceView.getHolder().addCallback(this);

        subtitleLayout = (SubtitleLayout) root.findViewById(R.id.subtitles);

        mediaController = new VideoControllerView(activity, false);
        mediaController.setAnchorView(root);

        exoVideoTextureView.setSurfaceTextureListener(new videoSurfaceTextureListener());
        exoVideoTextureView.setOnSizeChangeListener(this);


        videoFrame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick videoFrame" + View.GONE);
            }
        });
        root.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int visibility) {
//                Log.d(TAG, "onCSystemUiVisibilityChange root: " + visibility); TODO fix detection of navbar
                if((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0)
                {
                    toggleControlsVisibility();
                    Log.d(TAG, "onCo open" );
                }else if(visibility == 7){
                    toggleControlsVisibility();
                    Log.d(TAG, "onCo close" );

                }
            }
        });

        CookieHandler currentHandler = CookieHandler.getDefault();
        if (currentHandler != defaultCookieManager) {
            CookieHandler.setDefault(defaultCookieManager);
        }

    }


    // old Activity lifecycle Must be called in ExoplayerWrapper#activity lifecycle

    /**
     * Lifecycle Method
     */
    public void onResume() {
        configureSubtitleView();

        // The wrapper will be prepared on receiving audio capabilities.
        audioCapabilitiesReceiver.register();
        if (wrapper == null) {
            preparePlayer(autoplay);
        } else {
            wrapper.setBackgrounded(false);
        }
    }

    /**
     * Lifecycle Method
     */
    public void onPause() {
        if (!enableBackgroundAudio) {
            releasePlayer();
        } else {
            wrapper.setBackgrounded(true);
        }
        shutterView.setVisibility(View.VISIBLE);
    }

    /**
     * Lifecycle Method
     */
    public void onDestroy() {
        audioCapabilitiesReceiver.unregister();
        releasePlayer();
    }


    // AudioCapabilitiesReceiver.Listener methods

    @Override
    public void onAudioCapabilitiesChanged(AudioCapabilities audioCapabilities) {
        if (wrapper == null) {
            return;
        }
        boolean backgrounded = wrapper.getBackgrounded();
        boolean playWhenReady = wrapper.getPlayWhenReady();
        releasePlayer();
        preparePlayer(playWhenReady);
        wrapper.setBackgrounded(backgrounded);
    }

    // Internal methods

    private ExoPlayerWrapper.RendererBuilder getRendererBuilder() {
        String userAgent = Util.getUserAgent(activity, "ExoPlayerDemo");
        switch (video.getVideoType()) {
            case Video.SS:
                return new SmoothStreamingRendererBuilder(activity, userAgent, video.getUrl(),
                        null);
            case Video.DASH:
                return new DashRendererBuilder(activity, userAgent, video.getUrl(),
                        null);
            case Video.HLS:
                return new HlsRendererBuilder(activity, userAgent, video.getUrl());
            case Video.OTHER:
                return new ExtractorRendererBuilder(activity, userAgent, Uri.parse(video.getUrl()));
            default:
                throw new IllegalStateException("Unsupported type: " + video.getVideoType());
        }
    }

    private void preparePlayer(boolean playWhenReady) {
        if (wrapper == null) {
            createNewWrapper();
        }
//        wrapper.setSurface(surfaceView.getHolder().getSurface());
        wrapper.setPlayWhenReady(playWhenReady);
    }

    private void createNewWrapper() {
        wrapper = new ExoPlayerWrapper(getRendererBuilder());
        wrapper.addListener(this);
        wrapper.setCaptionListener(this);
        wrapper.setMetadataListener(this);
        wrapper.seekTo(playerPosition);
        playerNeedsPrepare = true;
        mediaController.setMediaPlayer(wrapper.getPlayerControl());
        mediaController.setEnabled(true);
        eventLogger = new EventLogger();
        eventLogger.startSession();
        wrapper.addListener(eventLogger);
        wrapper.setInfoListener(eventLogger);
        wrapper.setInternalErrorListener(eventLogger);
        if (playerNeedsPrepare) {
            wrapper.prepare();
            playerNeedsPrepare = false;
        }
    }

    public void changeVideo(Video video, long playerPosition, boolean playWhenReady) {
        this.video = video;
        this.playerPosition = playerPosition;
        createNewWrapper();
        if (playerNeedsPrepare) {
            wrapper.prepare();
            playerNeedsPrepare = false;
        }
//        wrapper.setSurface(surfaceView.getHolder().getSurface());
        wrapper.setPlayWhenReady(playWhenReady);

    }

    public void releasePlayer() {
        if (wrapper != null) {
            playerPosition = wrapper.getCurrentPosition();
            wrapper.release();
            wrapper = null;
            eventLogger.endSession();
            eventLogger = null;
        }
    }

    /**
     * Pause video playback.
     */
    public void pause() {
        // Set the autoplay for the video surface layer in case the surface hasn't been created yet.
        // This way, when the surface is created, it won't start playing.
        wrapper.getPlayerControl().pause();
    }

    /**
     * Pause video playback.
     */
    public void play() {
        // Set the autoplay for the video surface layer in case the surface hasn't been created yet.
        // This way, when the surface is created, it won't start playing.
        wrapper.setPlayWhenReady(false);
    }

    /**
     * Returns the current playback position in milliseconds.
     */
    public long getCurrentPosition() {
        return wrapper.getCurrentPosition();
    }

    // ExoplayerWrapper.Listener implementation

    @Override
    public void onStateChanged(boolean playWhenReady, int playbackState) {
        if (playbackState == ExoPlayer.STATE_ENDED) {
            showControls();
        }
        String text = "playWhenReady=" + playWhenReady + ", playbackState=";
        switch (playbackState) {
            case ExoPlayer.STATE_BUFFERING:
                text += "buffering";
                break;
            case ExoPlayer.STATE_ENDED:
                text += "ended";
                break;
            case ExoPlayer.STATE_IDLE:
                text += "idle";
                break;
            case ExoPlayer.STATE_PREPARING:
                text += "preparing";
                break;
            case ExoPlayer.STATE_READY:
                text += "ready";
                break;
            default:
                text += "unknown";
                break;
        }
        Log.v(TAG, text);
    }

    @Override
    public void onError(Exception e) {
        if (e instanceof UnsupportedDrmException) {
            // Special case DRM failures.
            UnsupportedDrmException unsupportedDrmException = (UnsupportedDrmException) e;
            int stringId = Util.SDK_INT < 18 ? R.string.drm_error_not_supported
                    : unsupportedDrmException.reason == UnsupportedDrmException.REASON_UNSUPPORTED_SCHEME
                    ? R.string.drm_error_unsupported_scheme : R.string.drm_error_unknown;
            Toast.makeText(activity.getApplicationContext(), stringId, Toast.LENGTH_LONG).show();
        }
        playerNeedsPrepare = true;
        showControls();
    }

    @Override
    public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees,
                                   float pixelWidthHeightRatio) {
        float videoAspectRatio = height == 0 ? 1 : (width * pixelWidthHeightRatio) / height;
        exoVideoTextureView.setAspectRatio(videoAspectRatio);

        shutterView.setVisibility(View.GONE);
        if (autoAspectRatio) {
//            videoFrame.setAspectRatio(
//                    height == 0 ? 1 : (width * pixelWidthHeightRatio) / height);
        }
    }

    // User controls

    private void toggleControlsVisibility() {
        if (mediaController.isShowing()) {
            mediaController.hide();
        } else {
            showControls();
        }
    }

    private void showControls() {
        mediaController.show(0);
    }

    // ExoplayerWrapper.CaptionListener implementation

    @Override
    public void onCues(List<Cue> cues) {
        subtitleLayout.setCues(cues);
    }

    // ExoplayerWrapper.MetadataListener implementation

    @Override
    public void onId3Metadata(List<Id3Frame> id3Frames) {
        for (Id3Frame id3Frame : id3Frames) {
            if (id3Frame instanceof TxxxFrame) {
                TxxxFrame txxxFrame = (TxxxFrame) id3Frame;
                Log.i(TAG, String.format("ID3 TimedMetadata %s: description=%s, value=%s", txxxFrame.id,
                        txxxFrame.description, txxxFrame.value));
            } else if (id3Frame instanceof PrivFrame) {
                PrivFrame privFrame = (PrivFrame) id3Frame;
                Log.i(TAG, String.format("ID3 TimedMetadata %s: owner=%s", privFrame.id, privFrame.owner));
            } else if (id3Frame instanceof GeobFrame) {
                GeobFrame geobFrame = (GeobFrame) id3Frame;
                Log.i(TAG, String.format("ID3 TimedMetadata %s: mimeType=%s, filename=%s, description=%s",
                        geobFrame.id, geobFrame.mimeType, geobFrame.filename, geobFrame.description));
            } else {
                Log.i(TAG, String.format("ID3 TimedMetadata %s", id3Frame.id));
            }
        }
    }

    // SurfaceHolder.Callback implementation

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (wrapper != null) {
            wrapper.setSurface(holder.getSurface());
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // Do nothing.
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (wrapper != null) {
            wrapper.blockingClearSurface();
        }
    }

    private void configureSubtitleView() {
        CaptionStyleCompat captionStyle;
        float fontScale;
        if (Util.SDK_INT >= 19) {
            captionStyle = getUserCaptionStyleV19();
            fontScale = getUserCaptionFontScaleV19();
        } else {
            captionStyle = CaptionStyleCompat.DEFAULT;
            fontScale = 1.0f;
        }
        subtitleLayout.setStyle(captionStyle);
        subtitleLayout.setFractionalTextSize(SubtitleLayout.DEFAULT_TEXT_SIZE_FRACTION * fontScale);
    }

    @TargetApi(19)
    private float getUserCaptionFontScaleV19() {
        CaptioningManager captioningManager =
                (CaptioningManager) activity.getSystemService(Context.CAPTIONING_SERVICE);
        return captioningManager.getFontScale();
    }

    @TargetApi(19)
    private CaptionStyleCompat getUserCaptionStyleV19() {
        CaptioningManager captioningManager =
                (CaptioningManager) activity.getSystemService(Context.CAPTIONING_SERVICE);
        return CaptionStyleCompat.createFromCaptionStyle(captioningManager.getUserStyle());
    }

    @Override
    public void onVideoSurfaceSizeChange(int width, int height) {
//        Update Shutters
        Log.d(TAG, "onVideoSurfaceSizeChange(" + width + ", " + height + ")");
    }

    private class videoSurfaceTextureListener implements TextureView.SurfaceTextureListener {

        private Surface surface;

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
            if (wrapper != null) {
                surface = new Surface(surfaceTexture);
                wrapper.setSurface(surface);
                if (autoplay) {
                    wrapper.setPlayWhenReady(true);
                }
            }
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {
            // Purposefully left blank
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            surface.release();
            if (wrapper != null) {
                wrapper.blockingClearSurface();
            }

            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
            // Purposefully left blank
        }
    }

}
