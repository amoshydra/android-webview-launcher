package com.amoshydra.androidapp;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.WindowInsets;
import android.webkit.WebView;

/**
 * A {@link WebView} whose reported safe-area insets can be overridden per edge.
 *
 * <p>WebView derives the page's {@code env(safe-area-inset-*)} values inside its
 * internal {@code AwContents} view, from the {@link WindowInsets} that reach it:
 *
 * <pre>
 *   int insetTypes = displayCutout() | systemBars();
 *   Insets safeArea = toWindowInsetsCompat(insets).getInsets(insetTypes);
 * </pre>
 *
 * <p>So the only way to influence {@code env(safe-area-inset-*)} natively is to change
 * those insets before they are dispatched to {@code AwContents}. A
 * {@code setOnApplyWindowInsetsListener} cannot do that: the platform listener returns a
 * boolean (consumed or not) and the AndroidX compat wrapper maps {@code != null} onto that
 * boolean, so a rebuilt {@code WindowInsetsCompat} never reaches the WebView. Overriding
 * {@link #onApplyWindowInsets} does work, because {@code ViewGroup.dispatchApplyWindowInsets}
 * passes its return value down to child views and {@code AwContents} is one.
 *
 * <p>Each edge must be set on <em>every</em> type in the mask, not just the type that
 * "owns" it, because {@code getInsets(mask)} returns the per-edge maximum across the mask.
 * Overriding only {@code navigationBars()} would leave a non-zero bottom wherever a display
 * cutout or caption bar also contributes one.
 */
public class InsetAwareWebView extends WebView {
    private static final String TAG = "InsetAwareWebView";

    private Integer topOverride;
    private Integer rightOverride;
    private Integer bottomOverride;
    private Integer leftOverride;
    private boolean loggedUnsupported = false;

    public InsetAwareWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public InsetAwareWebView(Context context) {
        super(context);
    }

    /**
     * Overrides {@code env(safe-area-inset-*)} for the given edges. Each parameter is in
     * pixels, or null to leave that edge at the real system value. Values are not clamped,
     * so a value larger than the real inset will push page content further in.
     *
     * <p>Call this before the first inset dispatch, i.e. before the page is loaded.
     */
    public void setInsetOverride(Integer top, Integer right, Integer bottom, Integer left) {
        this.topOverride = top;
        this.rightOverride = right;
        this.bottomOverride = bottom;
        this.leftOverride = left;
    }

    public boolean hasInsetOverride() {
        return topOverride != null || rightOverride != null
                || bottomOverride != null || leftOverride != null;
    }

    @Override
    public WindowInsets onApplyWindowInsets(WindowInsets insets) {
        if (!hasInsetOverride() || Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            if (hasInsetOverride() && !loggedUnsupported) {
                loggedUnsupported = true;
                Log.w(TAG, "Safe-area inset override needs API 30+; ignoring on API "
                        + Build.VERSION.SDK_INT);
            }
            return super.onApplyWindowInsets(insets);
        }
        return super.onApplyWindowInsets(applyOverrides(insets));
    }

    private WindowInsets applyOverrides(WindowInsets insets) {
        WindowInsets.Builder builder = new WindowInsets.Builder(insets);
        int[] types = {
                WindowInsets.Type.statusBars(),
                WindowInsets.Type.navigationBars(),
                WindowInsets.Type.captionBar(),
                WindowInsets.Type.displayCutout(),
        };
        for (int type : types) {
            android.graphics.Insets current = insets.getInsets(type);
            builder.setInsets(type, android.graphics.Insets.of(
                    leftOverride != null ? leftOverride : current.left,
                    topOverride != null ? topOverride : current.top,
                    rightOverride != null ? rightOverride : current.right,
                    bottomOverride != null ? bottomOverride : current.bottom));
        }
        return builder.build();
    }
}
