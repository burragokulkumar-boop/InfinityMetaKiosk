package com.infinitymeta.kiosk;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
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

    /*
     * Infinity Learn application package.
     */
    private static final String INFINITY_META_PACKAGE =
            "apps.infinitylearn.lms";

    private Dialog kioskDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(
                Color.rgb(103, 184, 213)
        );

        getWindow().setNavigationBarColor(
                Color.BLACK
        );

        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
        );

        hideSystemBars();

        setContentView(new HomeView(this));

        enterKiosk();
    }

    @Override
    protected void onResume() {
        super.onResume();

        hideSystemBars();
        enterKiosk();
    }

    /*
     * Hide status and navigation bars.
     *
     * Uses the older SYSTEM_UI_FLAG API for maximum
     * compatibility with the current project.
     */
    private void hideSystemBars() {

        getWindow()
                .getDecorView()
                .setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                );
    }

    /*
     * Enter Android Lock Task mode.
     */
    private void enterKiosk() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            try {
                startLockTask();
            } catch (Exception ignored) {
            }
        }
    }

    /*
     * Exit Android Lock Task mode.
     */
    private void exitKiosk() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            try {
                stopLockTask();
            } catch (Exception ignored) {
            }
        }
    }

    /*
     * Disable the Back button while this activity is running.
     */
    @Override
    public void onBackPressed() {
        // Intentionally disabled for kiosk mode.
    }

    /*
     * Convert dp to pixels.
     */
    private int dp(float value) {

        return (int) (
                value
                        * getResources()
                        .getDisplayMetrics()
                        .density
                        + 0.5f
        );
    }

    /*
     * Create a TextView.
     */
    private TextView label(
            String text,
            float size,
            int color
    ) {

        TextView textView =
                new TextView(this);

        textView.setText(text);
        textView.setTextSize(size);
        textView.setTextColor(color);
        textView.setGravity(
                Gravity.CENTER_VERTICAL
        );

        return textView;
    }

    /*
     * Create a rounded background.
     */
    private GradientDrawable rounded(
            int radius,
            int color
    ) {

        GradientDrawable drawable =
                new GradientDrawable();

        drawable.setColor(color);
        drawable.setCornerRadius(
                dp(radius)
        );

        return drawable;
    }

    /*
     * Show kiosk menu.
     */
    private void showKioskMenu() {

        if (
                kioskDialog != null
                        && kioskDialog.isShowing()
        ) {
            return;
        }

        kioskDialog =
                new Dialog(this);

        LinearLayout box =
                new LinearLayout(this);

        box.setOrientation(
                LinearLayout.VERTICAL
        );

        box.setPadding(
                dp(26),
                dp(14),
                dp(26),
                dp(8)
        );

        box.setBackground(
                rounded(
                        28,
                        Color.WHITE
                )
        );

        /*
         * Title.
         */
        TextView title =
                label(
                        "Kiosk",
                        20,
                        Color.rgb(
                                65,
                                65,
                                65
                        )
                );

        title.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
        );

        box.addView(
                title,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(55)
                )
        );

        divider(box);

        /*
         * Settings.
         */
        menuRow(
                box,
                "Settings",
                v -> openSettings()
        );

        /*
         * Exit kiosk.
         */
        menuRow(
                box,
                "Exit Kiosk",
                v -> confirmExit()
        );

        /*
         * Support.
         */
        menuRow(
                box,
                "Support",
                v -> Toast.makeText(
                        MainActivity.this,
                        "Support",
                        Toast.LENGTH_SHORT
                ).show()
        );

        /*
         * Open source.
         */
        menuRow(
                box,
                "Open source",
                v -> Toast.makeText(
                        MainActivity.this,
                        "Open source",
                        Toast.LENGTH_SHORT
                ).show()
        );

        divider(box);

        /*
         * Version.
         */
        TextView version =
                label(
                        "Version\n1.0.5",
                        16,
                        Color.rgb(
                                105,
                                105,
                                105
                        )
                );

        version.setPadding(
                0,
                dp(10),
                0,
                0
        );

        box.addView(
                version,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(70)
                )
        );

        /*
         * Installed date.
         */
        TextView date =
                label(
                        "Installed date\n260808",
                        16,
                        Color.rgb(
                                105,
                                105,
                                105
                        )
                );

        box.addView(
                date,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(70)
                )
        );

        /*
         * Done button.
         */
        TextView done =
                label(
                        "Done",
                        17,
                        Color.rgb(
                                55,
                                55,
                                55
                        )
                );

        done.setGravity(
                Gravity.CENTER
        );

        done.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
        );

        done.setOnClickListener(
                v -> {

                    if (
                            kioskDialog != null
                                    && kioskDialog.isShowing()
                    ) {
                        kioskDialog.dismiss();
                    }
                }
        );

        box.addView(
                done,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(55)
                )
        );

        kioskDialog.setContentView(box);

        Window window =
                kioskDialog.getWindow();

        if (window != null) {

            window.setBackgroundDrawableResource(
                    android.R.color.transparent
            );
        }

        kioskDialog.setCanceledOnTouchOutside(
                true
        );

        kioskDialog.show();

        window =
                kioskDialog.getWindow();

        if (window != null) {

            int screenWidth =
                    getResources()
                            .getDisplayMetrics()
                            .widthPixels;

            int width =
                    Math.min(
                            dp(610),
                            (int) (
                                    screenWidth * 0.78f
                            )
                    );

            window.setLayout(
                    width,
                    WindowManager.LayoutParams.WRAP_CONTENT
            );
        }
    }

    /*
     * Horizontal divider.
     */
    private void divider(
            LinearLayout box
    ) {

        View line =
                new View(this);

        line.setBackgroundColor(
                Color.rgb(
                        220,
                        220,
                        220
                )
        );

        box.addView(
                line,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(1)
                )
        );
    }

    /*
     * Create a kiosk menu row.
     */
    private void menuRow(
            LinearLayout box,
            String text,
            View.OnClickListener action
    ) {

        LinearLayout row =
                new LinearLayout(this);

        row.setGravity(
                Gravity.CENTER_VERTICAL
        );

        TextView left =
                label(
                        text,
                        17,
                        Color.rgb(
                                90,
                                90,
                                90
                        )
                );

        TextView right =
                label(
                        "View",
                        17,
                        Color.rgb(
                                115,
                                
