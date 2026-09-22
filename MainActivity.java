package com.infinitymeta.kiosk;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    /*
     * Package name of the Infinity Learn application.
     */
    private static final String INFINITY_META_PACKAGE =
            "apps.infinitylearn.lms";

    private DevicePolicyManager devicePolicyManager;
    private ComponentName adminComponent;

    private Dialog kioskDialog;

    /*
     * Once Exit Kiosk is pressed, this prevents onResume()
     * from hiding the system bars or restarting kiosk behavior.
     */
    private boolean kioskExitRequested = false;

    @
