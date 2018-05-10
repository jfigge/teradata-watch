package com.teradata.wearable.watchface;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.wearable.complications.ComplicationData;
import android.support.wearable.complications.ComplicationHelperActivity;
import android.support.wearable.complications.SystemProviders;
import android.support.wearable.complications.rendering.ComplicationDrawable;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import static java.lang.Math.pow;
import static java.lang.Math.sqrt;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import com.teradata.wearable.config.ComplicationConfigRecyclerViewAdapter;
import com.teradata.wearable.R;

public class TeradataWatchService extends CanvasWatchFaceService {

    private static final String TAG = "TeradataWatchService";
    private static final String SINGLE_DIGIT = "%d";
    private static final String DOUBLE_DIGIT = "%02d";
    private static final Locale LOCAL = Locale.getDefault();
    /**
     * Update rate in milliseconds for interactive mode. Defaults to one second
     * because the watch face needs to update seconds in interactive mode.
     */
    private static final int LINE_OFFSET  = 9;
    private static final int STROKE_WIDTH = 1;

    private static final int UPPER_COMPLICATION_ID          = 0;
    private static final int LOWER_COMPLICATION_ID          = 1;
    private static final int BATTERY_STATUS_COMPLICATION_ID = 2;

    private static final float SECOND_TICK_STROKE_WIDTH = 2f;

    private static final int[] COMPLICATION_IDS = {
            UPPER_COMPLICATION_ID,
            LOWER_COMPLICATION_ID,
            BATTERY_STATUS_COMPLICATION_ID
    };

    /**
     * Handler message id for updating the time periodically in interactive mode.
     */
    private static final int MSG_UPDATE_TIME = 0;

    // Left and right dial supported types.
    private static final int[][] COMPLICATION_SUPPORTED_TYPES = {
        {
            ComplicationData.TYPE_RANGED_VALUE, ComplicationData.TYPE_ICON,
            ComplicationData.TYPE_SHORT_TEXT,   ComplicationData.TYPE_SMALL_IMAGE
        },
        {
            ComplicationData.TYPE_RANGED_VALUE, ComplicationData.TYPE_ICON,
            ComplicationData.TYPE_SHORT_TEXT,   ComplicationData.TYPE_SMALL_IMAGE
        }
    };

    public static int[] getComplicationIds() {
        return COMPLICATION_IDS;
    }

    public static int getComplicationId(ComplicationConfigRecyclerViewAdapter.ComplicationLocation complicationLocation) {
        switch (complicationLocation) {
            case UPPER:          return UPPER_COMPLICATION_ID;
            case LOWER:          return LOWER_COMPLICATION_ID;
            default:
                return -1;
        }
    }

    public static int[] getSupportedComplicationTypes(ComplicationConfigRecyclerViewAdapter.ComplicationLocation complicationLocation) {
        return COMPLICATION_SUPPORTED_TYPES[complicationLocation.ordinal()];
    }

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private static class EngineHandler extends Handler {
        private final WeakReference<TeradataWatchService.Engine> mWeakReference;

        EngineHandler(TeradataWatchService.Engine reference) {
            mWeakReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            TeradataWatchService.Engine engine = mWeakReference.get();
            if (engine != null) {
                switch (msg.what) {
                    case MSG_UPDATE_TIME:
                        engine.handleUpdateTimeMessage();
                        break;
                }
            }
        }
    }

    private class Engine extends CanvasWatchFaceService.Engine {

        private final Handler updateTimeHandler = new EngineHandler(this);
        private Calendar         calendar;
        private Date             currentDate;
        private long             now;
        private SimpleDateFormat dateFormatter;

        private final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                calendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            }
        };

        private boolean mRegisteredTimeZoneReceiver = false;
        private Paint   backgroundPaint;
        private Paint   primaryPaint;
        private Paint   secondaryPaint;
        private Paint   notificationPaint;
        private Paint centerLinePaint;
        private Paint   tertiaryPaint;
        private int     primaryColour;
        private int     secondaryColour;
        private int     ambientColour;
        private Bitmap  teradataLogo;
        private Bitmap  teradataLogoAmbient;

        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        private boolean  lowBitAmbient;
        private boolean  burnInProtection;
        private boolean  ambientMode;
        private boolean  muteMode;

        private Typeface teradataFont;
        private int      midpointOfScreen;
        private Rect     hourBounds        = new Rect();
        private Rect     minutesBounds     = new Rect();
        private Rect     dateBounds        = new Rect();
        private Rect     logoBound         = new Rect();
        private Rect     ambientShift      = new Rect();
        private double   ambientLength     = 0;
        private double   ambientPie        = 0;
        private Point    ambientSize       = new Point();
        private Point    ambientOffset     = new Point();
        private Point    notificationPoint = new Point();

        private String hours,     minutes,     date,     seconds;
        private String lastHours, lastMinutes, lastDate;

        // Customization settings
        private boolean militaryTime      = true;
        private boolean showDate          = false;
        private boolean ambientDrift      = true;
        private boolean showSeconds       = false;
        private boolean batteryStatus     = true;
        private boolean showNotifications = true;

        /* Maps active complication ids to the data for that complication. Note: Data will only be
         * present if the user has chosen a provider via the settings activity for the watch face.
         */
        private SparseArray<ComplicationData> activeComplicationDataCache;

        /* Maps complication ids to corresponding ComplicationDrawable that renders the
         * the complication data on the watch face.
         */
        private SparseArray<ComplicationDrawable> complicationDrawableSparseArray;

        // List of watch complication ids
        List<Integer> complicationIds = new ArrayList<>();

        // User's preference for if they want visual shown to indicate unread notifications.
        private int numberOfUnreadNotifications = 0;

        // Used to pull user's preferences
        SharedPreferences sharedPreferences;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            Context context = getApplicationContext();
            sharedPreferences = context.getSharedPreferences(
                                    getString(R.string.analog_complication_preference_file_key),
                                    Context.MODE_PRIVATE);

            setWatchFaceStyle(new WatchFaceStyle.Builder(TeradataWatchService.this)
                    .setAcceptsTapEvents(true)
                    .setHideNotificationIndicator(true)
                    .build());

            calendar = Calendar.getInstance();

            loadSavedPreferences();
            initializeComplications();
            initializeWatchFace(context);
        }

        // Pulls all user's preferences for watch face appearance.
        private void loadSavedPreferences() {
            showDate          = sharedPreferences.getBoolean(getString(R.string.always_show_date_pref),           false);
            militaryTime      = sharedPreferences.getBoolean(getString(R.string.military_time_pref),              true);
            ambientDrift      = sharedPreferences.getBoolean(getString(R.string.ambient_drift_pref),              true);
            showSeconds       = sharedPreferences.getBoolean(getString(R.string.show_seconds_pref),               false);
            batteryStatus     = sharedPreferences.getBoolean(getString(R.string.show_battery_status_pref),        true);
            showNotifications = sharedPreferences.getBoolean(getString(R.string.saved_unread_notifications_pref), true);

            updateTimer();
        }

        private void initializeComplications() {
            Log.d(TAG, "initializeComplications()");
            setDefaultSystemComplicationProvider(BATTERY_STATUS_COMPLICATION_ID, SystemProviders.WATCH_BATTERY, ComplicationData.TYPE_RANGED_VALUE);

            activeComplicationDataCache = new SparseArray<>(3);

            ComplicationDrawable upperComplicationDrawable = (ComplicationDrawable) getDrawable(R.drawable.custom_complication_styles);
            if (upperComplicationDrawable != null) {
                upperComplicationDrawable.setContext(getApplicationContext());
                upperComplicationDrawable.setTextTypefaceActive(teradataFont);
            }

            ComplicationDrawable lowerComplicationDrawable = (ComplicationDrawable) getDrawable(R.drawable.custom_complication_styles);
            if (lowerComplicationDrawable != null) {
                lowerComplicationDrawable.setContext(getApplicationContext());
                lowerComplicationDrawable.setTextTypefaceActive(teradataFont);
            }

            complicationDrawableSparseArray = new SparseArray<>(COMPLICATION_IDS.length);
            complicationDrawableSparseArray.put(UPPER_COMPLICATION_ID, upperComplicationDrawable);
            complicationDrawableSparseArray.put(LOWER_COMPLICATION_ID, lowerComplicationDrawable);

            complicationIds.add(UPPER_COMPLICATION_ID);
            complicationIds.add(LOWER_COMPLICATION_ID);

            setActiveComplications(COMPLICATION_IDS);
        }

        private void initializeWatchFace(Context context) {
            currentDate   = new Date();
            dateFormatter = new SimpleDateFormat("E d ", Locale.getDefault());

            // Retrieve the teradata logo bitmaps
            Drawable logo = context.getDrawable(R.drawable.teradata_logo);
            teradataLogo = logo != null ? ((BitmapDrawable)logo).getBitmap() : null;
            logo = context.getDrawable(R.drawable.teradata_logo_ambiant);
            teradataLogoAmbient = logo != null ? ((BitmapDrawable)logo).getBitmap() : null;

            // Initializes background.
            backgroundPaint = new Paint();
            backgroundPaint.setColor(ContextCompat.getColor(context, R.color.background));

            // Load the teradata font

            teradataFont = ResourcesCompat.getFont(context, R.font.afterheadline);

            // Colours
            primaryColour   = ContextCompat.getColor(context, R.color.primary_text);
            secondaryColour = ContextCompat.getColor(context, R.color.secondary_text);
            ambientColour   = ContextCompat.getColor(context, R.color.ambient_text);

            // Initializes Watch Face.
            primaryPaint = new Paint();
            primaryPaint.setTypeface(teradataFont);
            primaryPaint.setAntiAlias(true);
            primaryPaint.setColor(primaryColour);
            primaryPaint.setStrokeWidth(1);

            secondaryPaint = new Paint();
            secondaryPaint.setTypeface(teradataFont);
            secondaryPaint.setTextSize(22f);
            secondaryPaint.setAntiAlias(true);
            secondaryPaint.setColor(secondaryColour);

            centerLinePaint = new Paint();
            centerLinePaint.setTypeface(teradataFont);
            centerLinePaint.setTextSize(22f);
            centerLinePaint.setAntiAlias(true);
            centerLinePaint.setColor(secondaryColour);

            tertiaryPaint = new Paint();
            tertiaryPaint.setTypeface(teradataFont);
            tertiaryPaint.setTextSize(22f);
            tertiaryPaint.setAntiAlias(true);
            tertiaryPaint.setColor(primaryColour);

            notificationPaint = new Paint();
            notificationPaint.setColor(secondaryColour);
            notificationPaint.setStrokeWidth(SECOND_TICK_STROKE_WIDTH);
            notificationPaint.setAntiAlias(true);
            notificationPaint.setStyle(Paint.Style.STROKE);


            logoBound.set(0, 0, teradataLogo.getWidth(), teradataLogo.getHeight());
        }

        @Override
        public void onDestroy() {
            updateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        @Override
        public void onComplicationDataUpdate(int complicationId, ComplicationData complicationData) {
            Log.d(TAG, "onComplicationDataUpdate() id: " + complicationId);

            // Adds/updates active complication data in the array.
            activeComplicationDataCache.put(complicationId, complicationData);

            if (complicationIds.contains(complicationId)) {
                // Updates correct ComplicationDrawable with updated data.
                ComplicationDrawable complicationDrawable = complicationDrawableSparseArray.get(complicationId);
                complicationDrawable.setComplicationData(complicationData);

                invalidate();
            }
        }

        /*
         * Determines if tap inside a complication area or returns -1.
         */
        private int getTappedComplicationId(int x, int y) {

            ComplicationData complicationData;
            ComplicationDrawable complicationDrawable;

            long currentTimeMillis = System.currentTimeMillis();

            for (int complicationId : complicationIds) {
                complicationData = activeComplicationDataCache.get(complicationId);

                if ((complicationData != null)
                        && (complicationData.isActive(currentTimeMillis))
                        && (complicationData.getType() != ComplicationData.TYPE_NOT_CONFIGURED)
                        && (complicationData.getType() != ComplicationData.TYPE_EMPTY)) {

                    complicationDrawable = complicationDrawableSparseArray.get(complicationId);
                    Rect complicationBoundingRect = complicationDrawable.getBounds();

                    if (complicationBoundingRect.width() > 0) {
                        if (complicationBoundingRect.contains(x, y)) {
                            return complicationId;
                        }
                    } else {
                        Log.e(TAG, "Not a recognized complication id.");
                    }
                }
            }
            return -1;
        }

        // Fires PendingIntent associated with complication (if it has one).
        private void onComplicationTap(int complicationId) {
            Log.d(TAG, "onComplicationTap()");

            ComplicationData complicationData =
                    activeComplicationDataCache.get(complicationId);

            if (complicationData != null) {

                if (complicationData.getTapAction() != null) {
                    try {
                        complicationData.getTapAction().send();
                    } catch (PendingIntent.CanceledException e) {
                        Log.e(TAG, "onComplicationTap() tap action error: " + e);
                    }

                } else if (complicationData.getType() == ComplicationData.TYPE_NO_PERMISSION) {

                    // Watch face does not have permission to receive complication data, so launch
                    // permission request.
                    ComponentName componentName = new ComponentName(getApplicationContext(), TeradataWatchService.class);
                    Intent permissionRequestIntent = ComplicationHelperActivity.createPermissionRequestHelperIntent(getApplicationContext(), componentName);
                    startActivity(permissionRequestIntent);
                }

            } else {
                Log.d(TAG, "No PendingIntent for complication " + complicationId + ".");
            }
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {

                // Preferences might have changed since last time watch face was visible.
                loadSavedPreferences();

                registerReceiver();

                // Update time zone in case it changed while we weren't visible.
                calendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            } else {
                unregisterReceiver();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        @Override
        public void onUnreadCountChanged(int count) {
            Log.d(TAG, "onUnreadCountChanged(): " + count);

            if (showNotifications) {

                if (numberOfUnreadNotifications != count) {
                    numberOfUnreadNotifications = count;
                    invalidate();
                }
            }
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            TeradataWatchService.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            TeradataWatchService.this.unregisterReceiver(mTimeZoneReceiver);
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);

            // Load resources that have alternate values for round watches.
            Resources resources = TeradataWatchService.this.getResources();
            boolean isRound = insets.isRound();
            float textSize = resources.getDimension(isRound ? R.dimen.digital_text_size_round : R.dimen.digital_text_size);
            primaryPaint.setTextSize(textSize);

            // Calculate ambient rectangle
            primaryPaint.getTextBounds("88", 0, 2, ambientShift);
            ambientShift.set(midpointOfScreen - ambientShift.width() / 2, midpointOfScreen - ambientShift.height() - LINE_OFFSET, midpointOfScreen * 2 - LINE_OFFSET, midpointOfScreen + ambientShift.height() + LINE_OFFSET);

            ambientSize.set(
                    (int)sqrt(pow(midpointOfScreen, 2) - pow(ambientShift.height() / 2, 2)) - ambientShift.width()  / 2,
                    (int)sqrt(pow(midpointOfScreen, 2) - pow(ambientShift.width()  / 2, 2)) - ambientShift.height() / 2
            );
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            lowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT,    false);
            burnInProtection = properties.getBoolean(PROPERTY_BURN_IN_PROTECTION, false);
            lastHours = null;

            ComplicationDrawable complicationDrawable;
            for (int complicationId : complicationIds) {
                complicationDrawable = complicationDrawableSparseArray.get(complicationId);
                complicationDrawable.setLowBitAmbient(lowBitAmbient);
                complicationDrawable.setBurnInProtection(burnInProtection);
            }
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);

            ambientMode = inAmbientMode;

            primaryPaint.setAntiAlias(!ambientMode);
            secondaryPaint.setAntiAlias(!ambientMode);
            tertiaryPaint.setAntiAlias(!ambientMode);
            notificationPaint.setAntiAlias(!ambientMode);

            primaryPaint.setColor     (ambientMode ? ambientColour : primaryColour);
            secondaryPaint.setColor   (ambientMode ? ambientColour : secondaryColour);
            tertiaryPaint.setColor    (ambientMode ? ambientColour : primaryColour);
            notificationPaint.setColor(ambientMode ? ambientColour : secondaryColour);

            ComplicationDrawable complicationDrawable;
            for (int complicationId : complicationIds) {
                complicationDrawable = complicationDrawableSparseArray.get(complicationId);
                complicationDrawable.setInAmbientMode(ambientMode);
            }

            // Reset the drifting back to normal
            ambientOffset.x = 0;
            ambientOffset.y = 0;

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        @Override
        public void onInterruptionFilterChanged(int interruptionFilter) {
            super.onInterruptionFilterChanged(interruptionFilter);
            boolean inMuteMode = (interruptionFilter == TeradataWatchService.INTERRUPTION_FILTER_NONE);
            Log.d(TAG, "Changing muteMode: " + inMuteMode);

            /* Dim display in mute mode. */
            if (muteMode != inMuteMode) {
                muteMode = inMuteMode;
                primaryPaint.setAlpha  (muteMode ? 100 : 255);
                secondaryPaint.setAlpha(100);
                tertiaryPaint.setAlpha (255);
                invalidate();
            }
        }

        /**
         * Captures tap event (and tap type) and toggles the background color if the user finishes
         * a tap.
         */
        @Override
        public void onTapCommand(int tapType, int x, int y, long eventTime) {
            Log.d(TAG, "OnTapCommand()");
            switch (tapType) {
                case TAP_TYPE_TOUCH:
                    // The user has started touching the screen.
                    break;
                case TAP_TYPE_TOUCH_CANCEL:
                    // The user has started a different gesture or otherwise cancelled the tap.
                    break;
                case TAP_TYPE_TAP:
                    int tappedComplicationId = getTappedComplicationId(x, y);
                    if (tappedComplicationId != -1) {
                        onComplicationTap(tappedComplicationId);
                    }
                    // The user has completed the tap gesture.
//                    Toast.makeText(getApplicationContext(), R.string.message, Toast.LENGTH_SHORT).show();
                    break;
            }
            invalidate();
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            // Find the mid point of the screen.
            int sizeOfComplication = (int)(width / 6.5);
            Log.d(TAG, "****** Size: " + sizeOfComplication);
            if (sizeOfComplication % 2 == 1) sizeOfComplication--;
            midpointOfScreen = height / 2;
            int leftStart = (midpointOfScreen - sizeOfComplication) / 2 - LINE_OFFSET * 2;
            int voffset = LINE_OFFSET;

            logoBound.offsetTo(width - logoBound.width() - LINE_OFFSET, midpointOfScreen - LINE_OFFSET - logoBound.height());

            //Rect topLeftBounds = new Rect(edge, edge, edge + sizeOfComplication, edge + sizeOfComplication);
            Rect upperBounds = new Rect(leftStart, midpointOfScreen - sizeOfComplication - voffset, leftStart + sizeOfComplication, midpointOfScreen - voffset);
            ComplicationDrawable topLeftComplicationDrawable = complicationDrawableSparseArray.get(UPPER_COMPLICATION_ID);
            topLeftComplicationDrawable.setBounds(upperBounds);

            //Rect bottomLeftBounds = new Rect(edge, width - edge - sizeOfComplication, edge + sizeOfComplication, width - edge);
            Rect lowerBounds = new Rect(leftStart, midpointOfScreen + voffset, leftStart + sizeOfComplication, midpointOfScreen + sizeOfComplication + voffset);
            ComplicationDrawable bottomLeftComplicationDrawable = complicationDrawableSparseArray.get(LOWER_COMPLICATION_ID);
            bottomLeftComplicationDrawable.setBounds(lowerBounds);

            notificationPoint.set(width / 2, height - 20);
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {

            ambientOffset.x = 0;
            ambientOffset.y = 0;

            now = System.currentTimeMillis();
            calendar.setTimeInMillis(now);
            currentDate.setTime(now);

            drawBackground(canvas, bounds);
            drawFace(canvas, bounds);
            drawUnreadNotificationIcon(canvas);
            drawComplications(canvas, now);
        }

        private void drawBackground(Canvas canvas, Rect bounds) {
            // Draw the background.
            if (ambientMode) {
                canvas.drawColor(Color.BLACK);

                // If ambient movement enable then randomize where the face is displayed
                if (ambientDrift) {
                    ambientLength = Math.random() * 100;
                    ambientPie = (int) Math.round(Math.random() * 360);
                    ambientOffset.x = (int) (((ambientSize.x * Math.cos(Math.toRadians(ambientPie))) - ambientSize.x) / 100.0 * ambientLength);
                    ambientOffset.y = (int) ((ambientSize.y * Math.sin(Math.toRadians(ambientPie))) / 100.0 * ambientLength);
                }
            } else {
                canvas.drawRect(0, 0, bounds.width(), bounds.height(), backgroundPaint);
            }
        }

        private void drawFace(Canvas canvas, Rect bounds) {
            hours   = String.format(LOCAL, !militaryTime ? SINGLE_DIGIT : DOUBLE_DIGIT, calendar.get(militaryTime ? Calendar.HOUR_OF_DAY : Calendar.HOUR));
            minutes = String.format(LOCAL, DOUBLE_DIGIT, calendar.get(Calendar.MINUTE));
            if (hours.equals("0") && !militaryTime) hours = "12";
            date    = dateFormatter.format(currentDate);

            if (!hours.equals(lastHours)) {
                Log.d(TAG, "Hour change");
                primaryPaint.getTextBounds(hours, 0, hours.length(), hourBounds);
                lastHours = hours;
                hourBounds.offsetTo((bounds.width() - hourBounds.width()) / 2, midpointOfScreen - (STROKE_WIDTH + LINE_OFFSET));
            }

            if (!minutes.equals(lastMinutes)) {
                Log.d(TAG, "Minute change");
                primaryPaint.getTextBounds(minutes, 0, minutes.length(), minutesBounds);
                lastMinutes = minutes;
                minutesBounds.offsetTo((bounds.width() - minutesBounds.width()) / 2, midpointOfScreen + (STROKE_WIDTH + LINE_OFFSET) + minutesBounds.height());
            }

            if (showSeconds) {
                seconds = String.format(LOCAL, DOUBLE_DIGIT, calendar.get(Calendar.SECOND));
                Log.d(TAG, "Second change");
            }

            if (!date.equals(lastDate) && (showDate || !ambientMode)) {
                Log.d(TAG, "Date change");
                secondaryPaint.getTextBounds(date, 0, date.length(), dateBounds);
                lastDate = date;
                dateBounds.offsetTo(logoBound.left + (logoBound.width() - dateBounds.width()) / 2, midpointOfScreen + dateBounds.height() + LINE_OFFSET);
            }

            // Time
            canvas.drawText(hours,      hourBounds.left + ambientOffset.x,    hourBounds.top + ambientOffset.y, primaryPaint);
            canvas.drawText(minutes, minutesBounds.left + ambientOffset.x, minutesBounds.top + ambientOffset.y, primaryPaint);
            if (showSeconds && !ambientMode) canvas.drawText(seconds, minutesBounds.right + LINE_OFFSET + ambientOffset.x, minutesBounds.top - LINE_OFFSET + ambientOffset.y, tertiaryPaint);

            // midpointOfScreen line
            canvas.drawLine(0, midpointOfScreen + ambientOffset.y, bounds.width(), midpointOfScreen + ambientOffset.y, secondaryPaint);

            // Logo / date
            canvas.drawBitmap(!ambientMode ? teradataLogo : teradataLogoAmbient, logoBound.left + ambientOffset.x, logoBound.top + ambientOffset.y, null);
            if (!ambientMode || showDate) canvas.drawText(date, dateBounds.left + ambientOffset.x, dateBounds.top + ambientOffset.y, secondaryPaint);
        }

        private void drawUnreadNotificationIcon(Canvas canvas) {
            if (showNotifications && (numberOfUnreadNotifications > 0)) {
                canvas.drawCircle(notificationPoint.x, notificationPoint.y, 8, notificationPaint);
                if (!ambientMode) {
                    canvas.drawCircle(notificationPoint.x, notificationPoint.y, 4, primaryPaint);
                }
            }
        }

        private void drawComplications(Canvas canvas, long currentTimeMillis) {
            if (!ambientMode) {
                ComplicationDrawable complicationDrawable;

                for (int complicationId : complicationIds) {
                    complicationDrawable = complicationDrawableSparseArray.get(complicationId);
                    complicationDrawable.draw(canvas, currentTimeMillis);
                }

                if (batteryStatus) {
                    ComplicationData data = activeComplicationDataCache.get(BATTERY_STATUS_COMPLICATION_ID);
                    if (data != null) {
                        //canvas.drawRect(0, midpointOfScreen + ambientOffset.y - 1, (int) (midpointOfScreen * 0.02 * data.getValue()), midpointOfScreen + ambientOffset.y + 1, centerLinePaint);
                        canvas.drawLine(0, midpointOfScreen + ambientOffset.y, (int) (midpointOfScreen * 0.02 * data.getValue()), midpointOfScreen + ambientOffset.y, primaryPaint);
                    }
                }
            }
        }

        /**
         * Starts the {@link #updateTimeHandler} timer if it should be running and isn't currently
         * or stops it if it shouldn't be running but currently is.
         */
        private void updateTimer() {
            updateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                updateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * Returns whether the {@link #updateTimeHandler} timer should be running. The timer should
         * only run when we're visible and in interactive mode.
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

        /**
         * Handle updating the time periodically in interactive mode.
         */
        private void handleUpdateTimeMessage() {
            Log.d(TAG, "Timer fired");
            invalidate();
            if (shouldTimerBeRunning()) {
                long interval = TimeUnit.SECONDS.toMillis(showSeconds ? 1 : 60);
                long timeMs = System.currentTimeMillis();
                long delayMs = interval - (timeMs % interval);
                updateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
            }
        }
    }
}
