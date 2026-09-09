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

    private void enterKiosk() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                startLockTask();
            } catch (Exception ignored) {
            }
        }
    }

    private void exitKiosk() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                stopLockTask();
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public void onBackPressed() {
        // Back is disabled while the kiosk screen is active.
    }

    private int dp(float value) {

        return (int) (
                value
                        * getResources()
                        .getDisplayMetrics()
                        .density
                        + 0.5f
        );
    }

    private TextView makeText(
            String text,
            float size,
            int color
    ) {

        TextView view = new TextView(this);

        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setGravity(Gravity.CENTER_VERTICAL);

        return view;
    }

    private GradientDrawable makeBackground(
            int radius,
            int color
    ) {

        GradientDrawable drawable =
                new GradientDrawable();

        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));

        return drawable;
    }

    private void showKioskMenu() {

        if (
                kioskDialog != null
                        && kioskDialog.isShowing()
        ) {
            return;
        }

        kioskDialog = new Dialog(this);

        LinearLayout container =
                new LinearLayout(this);

        container.setOrientation(
                LinearLayout.VERTICAL
        );

        container.setPadding(
                dp(26),
                dp(14),
                dp(26),
                dp(8)
        );

        container.setBackground(
                makeBackground(
                        28,
                        Color.WHITE
                )
        );

        TextView title =
                makeText(
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

        container.addView(
                title,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(55)
                )
        );

        addDivider(container);

        addMenuRow(
                container,
                "Settings",
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        openSettings();
                    }
                }
        );

        addMenuRow(
                container,
                "Exit Kiosk",
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        confirmExit();
                    }
                }
        );

        addMenuRow(
                container,
                "Support",
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(
                                MainActivity.this,
                                "Support",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }
        );

        addMenuRow(
                container,
                "Open source",
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(
                                MainActivity.this,
                                "Open source",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }
        );

        addDivider(container);

        TextView version =
                makeText(
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

        container.addView(
                version,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(70)
                )
        );

        TextView date =
                makeText(
                        "Installed date\n260808",
                        16,
                        Color.rgb(
                                105,
                                105,
                                105
                        )
                );

        container.addView(
                date,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(70)
                )
        );

        TextView done =
                makeText(
                        "Done",
                        17,
                        Color.rgb(
                                55,
                                55,
                                55
                        )
                );

        done.setGravity(Gravity.CENTER);

        done.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
        );

        done.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        if (
                                kioskDialog != null
                                        && kioskDialog.isShowing()
                        ) {
                            kioskDialog.dismiss();
                        }
                    }
                }
        );

        container.addView(
                done,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(55)
                )
        );

        kioskDialog.setContentView(container);

        kioskDialog.setCanceledOnTouchOutside(true);

        kioskDialog.show();

        Window window =
                kioskDialog.getWindow();

        if (window != null) {

            window.setBackgroundDrawableResource(
                    android.R.color.transparent
            );

            int width = Math.min(
                    dp(610),
                    (int) (
                            getResources()
                                    .getDisplayMetrics()
                                    .widthPixels
                                    * 0.78f
                    )
            );

            window.setLayout(
                    width,
                    WindowManager.LayoutParams.WRAP_CONTENT
            );
        }
    }

    private void addDivider(
            LinearLayout container
    ) {

        View divider = new View(this);

        divider.setBackgroundColor(
                Color.rgb(
                        220,
                        220,
                        220
                )
        );

        container.addView(
                divider,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(1)
                )
        );
    }

    private void addMenuRow(
            LinearLayout container,
            String text,
            View.OnClickListener listener
    ) {

        LinearLayout row =
                new LinearLayout(this);

        row.setGravity(
                Gravity.CENTER_VERTICAL
        );

        TextView left =
                makeText(
                        text,
                        17,
                        Color.rgb(
                                90,
                                90,
                                90
                        )
                );

        TextView right =
                makeText(
                        "View",
                        17,
                        Color.rgb(
                                115,
                                135,
                                205
                        )
                );

        right.setGravity(
                Gravity.CENTER
        );

        row.addView(
                left,
                new LinearLayout.LayoutParams(
                        0,
                        dp(68),
                        1
                )
        );

        row.addView(
                right,
                new LinearLayout.LayoutParams(
                        dp(72),
                        dp(68)
                )
        );

        row.setOnClickListener(listener);

        container.addView(row);
    }

    private void confirmExit() {

        new AlertDialog.Builder(this)
                .setTitle("Exit Kiosk")
                .setMessage(
                        "Are you sure you want to exit kiosk mode?"
                )
                .setNegativeButton(
                        "Cancel",
                        null
                )
                .setPositiveButton(
                        "Exit",
                        new android.content.DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(
                                    android.content.DialogInterface dialog,
                                    int which
                            ) {
                                exitKiosk();
                            }
                        }
                )
                .show();
    }

    private void openSettings() {

        try {

            Intent intent =
                    new Intent(
                            Settings.ACTION_SETTINGS
                    );

            startActivity(intent);

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Settings unavailable",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void openInfinityMeta() {

        try {

            Intent launchIntent =
                    getPackageManager()
                            .getLaunchIntentForPackage(
                                    INFINITY_META_PACKAGE
                            );

            if (launchIntent == null) {

                Toast.makeText(
                        this,
                        "Infinity Meta app is not installed",
                        Toast.LENGTH_LONG
                ).show();

                return;
            }

            launchIntent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_CLEAR_TOP
            );

            startActivity(launchIntent);

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Unable to open Infinity Meta",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private class HomeView extends View {

        private Bitmap wallpaper;

        private final Paint paint =
                new Paint(
                        Paint.FILTER_BITMAP_FLAG
                );

        HomeView(Context context) {

            super(context);

            wallpaper =
                    BitmapFactory.decodeResource(
                            getResources(),
                            R.drawable.infinity_wallpaper
                    );

            setFocusable(true);
            setClickable(true);
        }

        @Override
        protected void onDraw(Canvas canvas) {

            super.onDraw(canvas);

            if (wallpaper != null) {

                canvas.drawBitmap(
                        wallpaper,
                        null,
                        new android.graphics.Rect(
                                0,
                                0,
                                getWidth(),
                                getHeight()
                        ),
                        paint
                );

            } else {

                canvas.drawColor(
                        Color.WHITE
                );
            }
        }

        @Override
        public boolean onTouchEvent(
                MotionEvent event
        ) {

            if (
                    event.getAction()
                            == MotionEvent.ACTION_UP
            ) {

                float x = event.getX();
                float y = event.getY();

                /*
                 * Bottom-left information button.
                 */
                if (
                        x < getWidth() * 0.13f
                                &&
                        y > getHeight() * 0.88f
                ) {

                    showKioskMenu();

                    return true;
                }

                /*
                 * Infinity Meta logo.
                 */
                if (
                        x < getWidth() * 0.32f
                                &&
                        y < getHeight() * 0.30f
                ) {

                    openInfinityMeta();

                    return true;
                }
            }

            return true;
        }
    }
}
