package androidx.core.view;

import android.graphics.Rect;
import android.graphics.Region;
import android.util.ArrayMap;
import android.util.Log;
import android.view.MotionEvent;
import android.view.TouchDelegate;
import android.view.View;
import android.view.ViewParent;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.Iterator;

public class SeslTouchTargetDelegate extends TouchDelegate {
    private static final String TAG = "SeslTouchTargetDelegate";
    @NonNull
    final View mAnchorView;
    @NonNull
    final ArrayList<CapturedTouchDelegate> mTouchDelegateList = new ArrayList<>();
    @Nullable
    AccessibilityNodeInfo.TouchDelegateInfo mTouchDelegateInfo = null;

    public SeslTouchTargetDelegate(@NonNull View anchorView) {
        super(new Rect(), anchorView);
        mAnchorView = anchorView;
    }

    @Nullable
    public TouchDelegate addTouchDelegate(@NonNull Rect bounds, @NonNull View delegateView) {
        CapturedTouchDelegate delegate = new CapturedTouchDelegate(bounds, delegateView);
        mTouchDelegateList.add(delegate);
        return delegate;
    }

    @Nullable
    public TouchDelegate addTouchDelegate(@NonNull View view) {
        return addTouchDelegate(view, null);
    }

    @Nullable
    public TouchDelegate addTouchDelegate(@NonNull View delegateView, @Nullable ExtraInsets expandInsets) {
        Rect viewBounds = new Rect(0, 0, delegateView.getWidth(), delegateView.getHeight());

        View view = delegateView;
        while (view != mAnchorView) {
            Rect bounds = new Rect();
            view.getHitRect(bounds);
            viewBounds.left += bounds.left;
            viewBounds.right += bounds.left;
            viewBounds.top += bounds.top;
            viewBounds.bottom += bounds.top;

            ViewParent parent = view.getParent();
            if (!(parent instanceof View)) {
                break;
            }

            view = (View) parent;
        }

        if (view != mAnchorView) {
            Log.w(TAG, "Must be delegateView is child of anchorView");
            return null;
        }

        if (expandInsets != null) {
            viewBounds.left -= expandInsets.left;
            viewBounds.top -= expandInsets.top;
            viewBounds.right += expandInsets.right;
            viewBounds.bottom += expandInsets.bottom;
        }

        return addTouchDelegate(viewBounds, delegateView);
    }

    public boolean removeTouchDelegate(@NonNull TouchDelegate delegate) {
        if (delegate instanceof CapturedTouchDelegate) {
            return mTouchDelegateList.remove(delegate);
        } else {
            return false;
        }
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        Iterator<CapturedTouchDelegate> it = mTouchDelegateList.iterator();
        boolean consumed = false;
        while (it.hasNext()) {
            consumed |= it.next().onTouchEvent(event);
        }
        return consumed;
    }

    @Override
    @RequiresApi(29)
    public boolean onTouchExplorationHoverEvent(@NonNull MotionEvent event) {
        Iterator<CapturedTouchDelegate> it = mTouchDelegateList.iterator();
        boolean consumed = false;
        while (it.hasNext()) {
            consumed |= it.next().onTouchExplorationHoverEvent(event);
        }
        return consumed;
    }

    @Override
    @NonNull
    @RequiresApi(29)
    public AccessibilityNodeInfo.TouchDelegateInfo getTouchDelegateInfo() {
        if (mTouchDelegateInfo == null) {
            ArrayMap<Region, View> map = new ArrayMap<>(mTouchDelegateList.size());
            Iterator<CapturedTouchDelegate> it = mTouchDelegateList.iterator();
            while (it.hasNext()) {
                CapturedTouchDelegate delegate = it.next();
                map.put(new Region(delegate.mBounds), delegate.mView);
            }
            mTouchDelegateInfo = new AccessibilityNodeInfo.TouchDelegateInfo(map);
        }

        return mTouchDelegateInfo;
    }

    public static class ExtraInsets {
        public static final @NonNull ExtraInsets NONE = new ExtraInsets(0, 0, 0, 0);

        int left;
        int top;
        int right;
        int bottom;

        private ExtraInsets(int left, int top, int right, int bottom) {
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
        }

        public static @NonNull ExtraInsets of(int left, int top, int right, int bottom) {
            if (left == 0 && top == 0 && right == 0 && bottom == 0) {
                return NONE;
            }
            return new ExtraInsets(left, top, right, bottom);
        }

        public static @NonNull ExtraInsets of(int horizontal, int vertical) {
            if (horizontal == 0 && vertical == 0) {
                return NONE;
            }
            return new ExtraInsets(horizontal, vertical, horizontal, vertical);
        }

        public static @NonNull ExtraInsets of(@Nullable Rect r) {
            return (r == null) ? NONE : of(r.left, r.top, r.right, r.bottom);
        }

        public @NonNull Rect toRect() {
            return new Rect(left, top, right, bottom);
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;


            ExtraInsets extraInsets = (ExtraInsets) o;

            if (bottom != extraInsets.bottom) return false;
            if (left != extraInsets.left) return false;
            if (right != extraInsets.right) return false;
            if (top != extraInsets.top) return false;

            return true;
        }

        public int hashCode() {
            int result = left;
            result = 31 * result + top;
            result = 31 * result + right;
            result = 31 * result + bottom;
            return result;
        }

        public String toString() {
            return "ExtraInsets{" +
                    "left=" + left +
                    ", top=" + top +
                    ", right=" + right +
                    ", bottom=" + bottom +
                    '}';
        }
    }

    private static class CapturedTouchDelegate extends TouchDelegate {
        @NonNull
        protected final Rect mBounds;
        @NonNull
        protected final View mView;

        public CapturedTouchDelegate(@NonNull Rect bounds, @NonNull View delegateView) {
            super(bounds, delegateView);
            mBounds = bounds;
            mView = delegateView;
        }
    }

}
