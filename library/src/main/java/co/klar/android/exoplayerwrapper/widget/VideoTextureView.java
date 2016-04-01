package co.klar.android.exoplayerwrapper.widget;

import android.content.Context;
import android.graphics.Point;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.TextureView;

/**
 * A SurfaceView that resizes itself to match a specified aspect ratio.
 */
public class VideoTextureView extends TextureView {
    /**
     * The surface view will not resize itself if the fractional difference between its default
     * aspect ratio and the aspect ratio of the video falls below this threshold.
     * <p>
     * This tolerance is useful for fullscreen playbacks, since it ensures that the surface will
     * occupy the whole of the screen when playing content that has the same (or virtually the same)
     * aspect ratio as the device. This typically reduces the number of view layers that need to be
     * composited by the underlying system, which can help to reduce power consumption.
     */
    private static final float MAX_ASPECT_RATIO_DEFORMATION_FRACTION  = 0.01f;

    public interface OnSizeChangeListener {
        void onVideoSurfaceSizeChange(int width, int height);
    }

    private float videoAspectRatio;

    @Nullable
    private OnSizeChangeListener listener;
    private Point oldSize = new Point(0, 0);

    public VideoTextureView(Context context) {
        super(context);
    }

    public VideoTextureView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Set the aspect ratio that this {@link VideoTextureView} should satisfy.
     *
     * @param widthHeightRatio The width to height ratio.
     */
    public void setAspectRatio(float widthHeightRatio) {
        if (this.videoAspectRatio != widthHeightRatio) {
            this.videoAspectRatio = widthHeightRatio;
            requestLayout();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (videoAspectRatio == 0) {
            // Aspect ratio not set.
            return;
        }

        int width = getMeasuredWidth();
        int height = getMeasuredHeight();
        float viewAspectRatio = (float) width / height;
        float aspectDeformation = videoAspectRatio / viewAspectRatio - 1;
        if (Math.abs(aspectDeformation) <= MAX_ASPECT_RATIO_DEFORMATION_FRACTION) {
            // We're within the allowed tolerance, so leave the values from super
            notifyListener(width, height);
            return;
        }

        if (aspectDeformation > 0) {
            height = (int) (width / videoAspectRatio);
        } else {
            width = (int) (height * videoAspectRatio);
        }

        super.onMeasure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
        notifyListener(width, height);
    }

    public void setOnSizeChangeListener(@Nullable OnSizeChangeListener listener) {
        this.listener = listener;
    }

    private void notifyListener(int width, int height) {
        if (listener != null && (oldSize.x != width || oldSize.y != height)) {
            oldSize.x = width;
            oldSize.y = height;
            listener.onVideoSurfaceSizeChange(width, height);
        }
    }
}
