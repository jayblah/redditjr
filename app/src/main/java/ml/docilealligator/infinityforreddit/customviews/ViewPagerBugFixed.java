package ml.docilealligator.infinityforreddit.customviews;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.ViewPager;
import java.util.function.BooleanSupplier;

public class ViewPagerBugFixed extends ViewPager {
    private boolean vertical;
    @Nullable
    private BooleanSupplier keepsDrag;
    private final int touchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
    private float downX;
    private float downY;
    /** Whether this vertical pager has taken the current gesture away from the page under the finger. */
    private boolean takenOver;

    public ViewPagerBugFixed(@NonNull Context context) {
        super(context);
    }

    public ViewPagerBugFixed(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Pages up and down instead of sideways. A ViewPager only knows one axis, so this lays the pages
     * out along the other and hands the pager its touches with the two axes swapped; the children
     * still get their touches as they were, until {@link #dispatchTouchEvent} takes a drag from them.
     */
    public void setVertical(boolean vertical) {
        this.vertical = vertical;
        setPageTransformer(true, vertical ? (page, position) -> {
            page.setTranslationX(page.getWidth() * -position);
            page.setTranslationY(position * page.getHeight());
        } : null);
        setOverScrollMode(vertical ? OVER_SCROLL_NEVER : OVER_SCROLL_ALWAYS);
    }

    /**
     * While this says yes, a vertical pager leaves an up or down drag to the page under the finger
     * instead of turning it into a page change: a zoomed picture is panned, not paged away from.
     */
    public void setKeepsDrag(@Nullable BooleanSupplier keepsDrag) {
        this.keepsDrag = keepsDrag;
    }

    /**
     * A vertical pager decides for itself, ahead of its pages, when a drag is its own. A ViewPager
     * normally waits to be offered the drag by its parent, but the gallery's image view takes every
     * touch and asks everything above it to keep out until the finger moves sideways, which is the
     * wrong direction here: left to that, an up or down drag would never reach the pager. So once a
     * single finger has moved up or down past the touch slop the page is cancelled and the pager is
     * driven directly, whatever has been asked of it.
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (!vertical) {
            return super.dispatchTouchEvent(ev);
        }
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                takenOver = false;
                downX = ev.getX();
                downY = ev.getY();
                // The scrolling wrappers above this take any up or down drag for their own; their
                // say ends when they are asked to let go.
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (!takenOver && ev.getPointerCount() == 1 && !(keepsDrag != null && keepsDrag.getAsBoolean())) {
                    float dy = Math.abs(ev.getY() - downY);
                    if (dy > touchSlop && dy > Math.abs(ev.getX() - downX)) {
                        takeOver(ev);
                    }
                }
                break;
            default:
                break;
        }
        if (takenOver) {
            onTouchEvent(ev);
            int action = ev.getActionMasked();
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                takenOver = false;
            }
            return true;
        }
        return super.dispatchTouchEvent(ev);
    }

    /** Cancels the page under the finger, then replays the gesture from where it began into the pager. */
    private void takeOver(MotionEvent ev) {
        takenOver = true;
        MotionEvent cancel = MotionEvent.obtain(ev);
        cancel.setAction(MotionEvent.ACTION_CANCEL);
        super.dispatchTouchEvent(cancel);
        cancel.recycle();
        MotionEvent down = MotionEvent.obtain(ev.getDownTime(), ev.getDownTime(), MotionEvent.ACTION_DOWN, downX, downY, 0);
        onTouchEvent(down);
        down.recycle();
    }

    /** {@code ev} with its x and y exchanged, in the pager's own proportions; applying it twice undoes it. */
    private MotionEvent swapAxes(MotionEvent ev) {
        float width = getWidth();
        float height = getHeight();
        ev.setLocation(ev.getY() / height * width, ev.getX() / width * height);
        return ev;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        try {
            if (!vertical) {
                return super.onTouchEvent(ev);
            }
            // Handed back as it came, since whatever looks at the event next expects the screen's axes.
            boolean handled = super.onTouchEvent(swapAxes(ev));
            swapAxes(ev);
            return handled;
        } catch (IllegalArgumentException ignore) {
            Log.d("ViewPagerBugFixed", "onTouchEvent: ignoring IllegalArgumentException", ignore);
        }

        return false;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (vertical) {
            // Everything a vertical pager takes, it takes in dispatchTouchEvent.
            return false;
        }
        try {
            return super.onInterceptTouchEvent(ev);
        } catch (IllegalArgumentException ignore) {
            Log.d("ViewPagerBugFixed", "onInterceptTouchEvent: ignoring IllegalArgumentException", ignore);
        }

        return false;
    }
}
