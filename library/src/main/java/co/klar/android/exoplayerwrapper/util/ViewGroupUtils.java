package co.klar.android.exoplayerwrapper.util;

import android.content.Context;
import android.content.res.Configuration;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by cklar on 23.09.15.
 */
public class ViewGroupUtils {

    public static ViewGroup getParent(View view) {
        return (ViewGroup)view.getParent();
    }

    public static void removeView(View view) {
        ViewGroup parent = getParent(view);
        if(parent != null) {
            parent.removeView(view);
        }
    }

    public static void replaceView(View currentView, View newView) {
        ViewGroup parent = getParent(currentView);
        if(parent == null) {
            return;
        }
        final int index = parent.indexOfChild(currentView);
        removeView(currentView);
        removeView(newView);
        parent.addView(newView, index);
    }

    public static boolean isLandscape(@NonNull Context context){
        return context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
    }
}
