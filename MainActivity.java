package com.infinitymeta.kiosk;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final int BLUE = Color.rgb(45, 83, 180);
    private Dialog kioskDialog;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(103, 184, 213));
        getWindow().setNavigationBarColor(Color.BLACK);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
        setContentView(new HomeView(this));
        enterKiosk();
    }

    private void enterKiosk() {
        try { if (android.os.Build.VERSION.SDK_INT >= 21) startLockTask(); }
        catch (Exception ignored) { }
    }

    private void exitKiosk() {
        try { if (android.os.Build.VERSION.SDK_INT >= 21) stopLockTask(); }
        catch (Exception ignored) { }
    }

    @Override public void onBackPressed() {
        // Back is deliberately consumed while the kiosk is running.
    }

    private int dp(float n) { return (int)(n * getResources().getDisplayMetrics().density + .5f); }

    private TextView label(String s, float size, int color) {
        TextView t = new TextView(this);
        t.setText(s); t.setTextSize(size); t.setTextColor(color);
        t.setGravity(Gravity.CENTER_VERTICAL);
        return t;
    }

    private GradientDrawable rounded(int radius, int color) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color); g.setCornerRadius(dp(radius));
        return g;
    }

    private void showKioskMenu() {
        if (kioskDialog != null && kioskDialog.isShowing()) return;

        kioskDialog = new Dialog(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(26), dp(14), dp(26), dp(8));
        box.setBackground(rounded(28, Color.WHITE));

        TextView title = label("Kiosk", 20, Color.rgb(65,65,65));
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        box.addView(title, new LinearLayout.LayoutParams(-1, dp(55)));
        divider(box);

        menuRow(box, "Settings", v -> openSettings());
        menuRow(box, "Exit Kiosk", v -> confirmExit());
        menuRow(box, "Support", v -> Toast.makeText(this, "Support", Toast.LENGTH_SHORT).show());
        menuRow(box, "Open source", v -> Toast.makeText(this, "Open source", Toast.LENGTH_SHORT).show());
        divider(box);

        TextView version = label("Version\n1.0.5", 16, Color.rgb(105,105,105));
        version.setPadding(0, dp(10), 0, 0);
        box.addView(version, new LinearLayout.LayoutParams(-1, dp(70)));

        TextView date = label("Installed date\n260808", 16, Color.rgb(105,105,105));
        box.addView(date, new LinearLayout.LayoutParams(-1, dp(70)));

        TextView done = label("Done", 17, Color.rgb(55,55,55));
        done.setGravity(Gravity.CENTER);
        done.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        done.setOnClickListener(v -> kioskDialog.dismiss());
        box.addView(done, new LinearLayout.LayoutParams(-1, dp(55)));

        kioskDialog.setContentView(box);
        Window w = kioskDialog.getWindow();
        if (w != null) w.setBackgroundDrawableResource(android.R.color.transparent);
        kioskDialog.setCanceledOnTouchOutside(true);
        kioskDialog.show();
        w = kioskDialog.getWindow();
        if (w != null) {
            int width = Math.min(dp(610), (int)(getResources().getDisplayMetrics().widthPixels * .78f));
            w.setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT);
        }
    }

    private void divider(LinearLayout box) {
        View line = new View(this);
        line.setBackgroundColor(Color.rgb(220,220,220));
        box.addView(line, new LinearLayout.LayoutParams(-1, dp(1)));
    }

    private void menuRow(LinearLayout box, String text, View.OnClickListener action) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        TextView left = label(text, 17, Color.rgb(90,90,90));
        TextView right = label("View", 17, Color.rgb(115,135,205));
        right.setGravity(Gravity.CENTER);
        row.addView(left, new LinearLayout.LayoutParams(0, dp(68), 1));
        row.addView(right, new LinearLayout.LayoutParams(dp(72), dp(68)));
        row.setOnClickListener(action);
        box.addView(row);
    }

    private void confirmExit() {
        new AlertDialog.Builder(this)
            .setTitle("Exit Kiosk")
            .setMessage("Are you sure you want to exit kiosk mode?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Exit", (d, which) -> exitKiosk())
            .show();
    }

    private void openSettings() {
        try { startActivity(new Intent(Settings.ACTION_SETTINGS)); }
        catch (Exception e) { Toast.makeText(this, "Settings unavailable", Toast.LENGTH_SHORT).show(); }
    }

    private class HomeView extends View {
        private final android.graphics.Bitmap wallpaper;
        HomeView(Context c) {
            super(c);
            wallpaper = android.graphics.BitmapFactory.decodeResource(getResources(), R.drawable.infinity_wallpaper);
            setFocusable(true);
        }
        @Override protected void onDraw(android.graphics.Canvas c) {
            super.onDraw(c);
            android.graphics.Paint p = new android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG);
            c.drawBitmap(wallpaper, null, new android.graphics.Rect(0,0,getWidth(),getHeight()), p);
        }
        @Override public boolean onTouchEvent(MotionEvent e) {
            if (e.getAction() == MotionEvent.ACTION_UP) {
                float x=e.getX(), y=e.getY();
                // i button is at the lower-left of the supplied portrait screenshot.
                if (x < getWidth()*0.13f && y > getHeight()*0.88f) {
                    showKioskMenu(); return true;
                }
                // Desktop Infinity Meta icon is upper-left. The wallpaper already contains the exact icon.
                if (x < getWidth()*0.32f && y < getHeight()*0.30f) return true;
            }
            return true;
        }
    }
}
