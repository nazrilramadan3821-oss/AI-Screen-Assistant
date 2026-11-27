package com.nazril.asisten;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class OverlayController {
  public interface Listener {
    void onCaptureRequested();
    void onStopRequested();
  }

  private final Context ctx;
  private final WindowManager wm;
  private final int screenHeight;
  private final Listener listener;
  private final Handler ui = new Handler(Looper.getMainLooper());
  private View bubble, panel;
  private TextView resultView;

  public OverlayController(Context ctx, WindowManager wm, int screenHeight, Listener listener) {
    this.ctx = ctx;
    this.wm = wm;
    this.screenHeight = screenHeight;
    this.listener = listener;
  }

  private int dp(int v) {
    return (int) (v * ctx.getResources().getDisplayMetrics().density);
  }

  public void showBubble() {
    if (bubble != null) return;
    RobotBodyView t = new RobotBodyView(ctx);
    t.setText("AI");
    t.setTextColor(Color.WHITE);
    t.setTextSize(18);
    t.setGravity(Gravity.CENTER);

    GradientDrawable g = new GradientDrawable();
    g.setShape(GradientDrawable.OVAL);
    g.setColor(Color.parseColor("#3b82f6"));
    t.setBackground(g);

    final WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
      dp(56), dp(56),
      WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
      WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
      PixelFormat.TRANSLUCENT);
    lp.gravity = Gravity.TOP | Gravity.START;
        lp.flags |= WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;
    lp.x = dp(8);
    lp.y = dp(200);

    final Runnable stopper = listener::onStopRequested;
    t.setOnTouchListener(new View.OnTouchListener() {
      int sx, sy;
      float tx, ty;
      boolean moved;

      @Override
      public boolean onTouch(View v, MotionEvent e) {
        int a = e.getAction();
        if (a == MotionEvent.ACTION_DOWN) {
          sx = lp.x;
          sy = lp.y;
          tx = e.getRawX();
          ty = e.getRawY();
          moved = false;
          ((RobotBodyView) v).poke();
          ui.postDelayed(stopper, 1000);
        } else if (a == MotionEvent.ACTION_MOVE) {
          float dx = e.getRawX() - tx;
          float dy = e.getRawY() - ty;
          if (Math.abs(dx) > dp(8) || Math.abs(dy) > dp(8)) moved = true;
          if (moved) {
            ui.removeCallbacks(stopper);
            lp.x = sx + (int) dx;
            lp.y = sy + (int) dy;
            wm.updateViewLayout(v, lp);
          }
        } else if (a == MotionEvent.ACTION_UP) {
          ui.removeCallbacks(stopper);
          if (!moved) listener.onCaptureRequested();
        } else if (a == MotionEvent.ACTION_CANCEL) {
          ui.removeCallbacks(stopper);
        }
        return true;
      }
    });

    bubble = t;
    wm.addView(bubble, lp);
  }

  public void setBubbleVisible(boolean visible) {
    if (bubble != null) {
      bubble.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
    }
  }

  private CharSequence rapi(CharSequence t) {
if (t == null) return "";
try {
String[] baris = t.toString().split("\n", -1);
android.text.SpannableStringBuilder sb = new android.text.SpannableStringBuilder();
java.util.regex.Pattern pn = java.util.regex.Pattern.compile("^\\s*(\\d{1,3}\\s*[\\.\\)]|Jawaban\\b).*", java.util.regex.Pattern.CASE_INSENSITIVE);
for (int i = 0; i < baris.length; i++) {
String b = baris[i];
boolean judul = pn.matcher(b).matches();
if (judul && sb.length() > 0 && !sb.toString().endsWith("\n\n")) sb.append("\n");
int a = sb.length();
sb.append(b);
if (judul) {
sb.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD), a, sb.length(), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
sb.setSpan(new android.text.style.ForegroundColorSpan(0xFF93C5FD), a, sb.length(), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
}
if (i < baris.length - 1) sb.append("\n");
}
return sb;
} catch (Exception e) { return t; }
}
public void showPanel(String text) {
    if (panel == null) {
      LinearLayout box = new LinearLayout(ctx);
      box.setOrientation(LinearLayout.VERTICAL);
      GradientDrawable g = new GradientDrawable();
      g.setColor(Color.parseColor("#F2131a30"));
      g.setCornerRadius(dp(16));
      g.setStroke(dp(1), Color.parseColor("#24304f"));
      box.setBackground(g);
      box.setPadding(dp(14), dp(10), dp(14), dp(10));

      resultView = new TextView(ctx) { @Override public void setText(CharSequence t, BufferType bt) { super.setText(rapi(t), BufferType.SPANNABLE); } };
      resultView.setTextColor(Color.WHITE);
      resultView.setTextSize(17); resultView.setLineSpacing(dp(4), 1f);

      ScrollView sv = new ScrollView(ctx);
      sv.addView(resultView);

      Button close = new Button(ctx);
      close.setText("Tutup");
      close.setOnClickListener(v -> hidePanel());

      box.addView(sv, new LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
      box.addView(close, new LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

      WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT, (int) (screenHeight * 0.45f),
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
      lp.gravity = Gravity.BOTTOM;
      panel = box;
      wm.addView(panel, lp);
    }
    resultView.setText(text == null ? "" : text.replace("$", "").replace("\\times", "\u00d7").replace("\\div", "\u00f7"));
  }

  public void hidePanel() {
    if (panel != null) {
      try {
        wm.removeView(panel);
      } catch (Exception ignored) {
      }
      panel = null;
    }
  }

  public void destroy() {
    ui.removeCallbacksAndMessages(null);
    hidePanel();
    if (bubble != null) {
      try {
        wm.removeView(bubble);
      } catch (Exception ignored) {
      }
      bubble = null;
    }
  }
}
