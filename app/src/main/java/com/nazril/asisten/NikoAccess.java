package com.nazril.asisten;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Intent;
import android.graphics.Path;
import android.view.accessibility.AccessibilityEvent;

public class NikoAccess extends AccessibilityService {
    public static NikoAccess inst;

    @Override protected void onServiceConnected() { inst = this; }
    @Override public void onAccessibilityEvent(AccessibilityEvent e) {}
    @Override public void onInterrupt() {}
    @Override public boolean onUnbind(Intent i) { inst = null; return super.onUnbind(i); }

    public void ambilLayar(final java.util.function.Consumer<android.graphics.Bitmap> ok, final Runnable gagal) {
if (android.os.Build.VERSION.SDK_INT < 30) { gagal.run(); return; }
try {
takeScreenshot(android.view.Display.DEFAULT_DISPLAY, getMainExecutor(), new AccessibilityService.TakeScreenshotCallback() {
@Override public void onSuccess(AccessibilityService.ScreenshotResult res) {
try {
android.graphics.Bitmap hw = android.graphics.Bitmap.wrapHardwareBuffer(res.getHardwareBuffer(), res.getColorSpace());
android.graphics.Bitmap b = hw.copy(android.graphics.Bitmap.Config.ARGB_8888, false);
hw.recycle(); res.getHardwareBuffer().close();
ok.accept(b);
} catch (Exception e) { gagal.run(); }
}
@Override public void onFailure(int code) { gagal.run(); }
});
} catch (Exception e) { gagal.run(); }
}
public void swipe(float x1, float y1, float x2, float y2, long ms) {
        Path p = new Path();
        p.moveTo(x1, y1);
        p.lineTo(x2, y2);
        dispatchGesture(new GestureDescription.Builder()
                .addStroke(new GestureDescription.StrokeDescription(p, 0, ms)).build(), null, null);
    }

    public void tap(float x, float y) {
        Path p = new Path();
        p.moveTo(x, y);
        dispatchGesture(new GestureDescription.Builder()
                .addStroke(new GestureDescription.StrokeDescription(p, 0, 60)).build(), null, null);
    }
}
