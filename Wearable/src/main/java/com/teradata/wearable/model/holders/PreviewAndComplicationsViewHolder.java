package com.teradata.wearable.model.holders;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.support.wearable.complications.ComplicationHelperActivity;
import android.support.wearable.complications.ComplicationProviderInfo;
import android.support.wearable.complications.ProviderInfoRetriever;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.teradata.wearable.R;
import com.teradata.wearable.config.ComplicationConfigRecyclerViewAdapter;
import com.teradata.wearable.watchface.TeradataWatchService;

/**
 * Displays watch face preview along with complication locations. Allows user to tap on the
 * complication they want to change and preview updates dynamically.
 */
public class PreviewAndComplicationsViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

    private static final String TAG = PreviewAndComplicationsViewHolder.class.getSimpleName();

    private Drawable defaultAddComplicationDrawable;
    private ImageButton complicationUpper;
    private ImageButton complicationLower;

    // ComponentName used to identify a specific service that renders the watch face.
    private ComponentName watchFaceComponentName;

    // Selected complication id by user.
    private int selectedComplicationId;

    private int complicationUpperId;
    private int complicationLowerId;

    private Context context;

    public PreviewAndComplicationsViewHolder(final View view, final Class watchServiceClass, final Context context) {
        super(view);

        this.context                = context;
        this.watchFaceComponentName = new ComponentName(context, watchServiceClass);

        // Default value is invalid (only changed when user taps to change complication).
        selectedComplicationId = -1;

        complicationUpperId = TeradataWatchService.getComplicationId(ComplicationConfigRecyclerViewAdapter.ComplicationLocation.UPPER);
        complicationUpper = view.findViewById(R.id.upper_complication);
        complicationUpper.setOnClickListener(this);
        complicationUpper.setImageDrawable(defaultAddComplicationDrawable);

        complicationLower = view.findViewById(R.id.lower_complication);
        complicationLower.setOnClickListener(this);
        complicationLower.setImageDrawable(defaultAddComplicationDrawable);
        complicationLowerId = TeradataWatchService.getComplicationId(ComplicationConfigRecyclerViewAdapter.ComplicationLocation.LOWER);

    }

    @Override
    public void onClick(View view) {
        if (view.equals(complicationUpper)) {
            Log.d(TAG, "Upper complication click()");
            launchComplicationHelperActivity((Activity) view.getContext(), ComplicationConfigRecyclerViewAdapter.ComplicationLocation.UPPER);
        } else if (view.equals(complicationLower)) {
            Log.d(TAG, "Lower complication click()");
            launchComplicationHelperActivity((Activity)view.getContext(), ComplicationConfigRecyclerViewAdapter.ComplicationLocation.LOWER);
        }
    }

    // Verifies the watch face supports the complication location, then launches the helper
    // class, so user can choose their complication data provider.
    private void launchComplicationHelperActivity(Activity currentActivity, ComplicationConfigRecyclerViewAdapter.ComplicationLocation complicationLocation) {

        selectedComplicationId = TeradataWatchService.getComplicationId(complicationLocation);

        if (selectedComplicationId >= 0) {
            int[] supportedTypes = TeradataWatchService.getSupportedComplicationTypes(complicationLocation);
            currentActivity.startActivityForResult(
                    ComplicationHelperActivity.createProviderChooserHelperIntent(
                            currentActivity,
                            watchFaceComponentName,
                            selectedComplicationId,
                            supportedTypes
                    ),
                    ComplicationConfigRecyclerViewAdapter.COMPLICATION_CONFIG_REQUEST_CODE
            );
        } else {
            Log.d(TAG, "Complication not supported by watch face.");
        }
    }

    public void setDefaultComplicationDrawable(int resourceId) {
        defaultAddComplicationDrawable = context.getDrawable(resourceId);
    }

    public void updateComplicationViews(ComplicationProviderInfo complicationProviderInfo) {
        updateComplicationViews(selectedComplicationId, complicationProviderInfo);
    }

    private void updateComplicationViews (int complicationId, ComplicationProviderInfo complicationProviderInfo) {
        Log.d(TAG, "updateComplicationViews(): id: " + complicationId);
        Log.d(TAG, "\tinfo: " + complicationProviderInfo);

        if (complicationId == complicationUpperId) {
            if (complicationProviderInfo != null) {
                complicationUpper.setImageIcon(complicationProviderInfo.providerIcon);
            } else {
                complicationUpper.setImageDrawable(defaultAddComplicationDrawable);
            }
        } else if (complicationId == complicationLowerId) {
            if (complicationProviderInfo != null) {
                complicationLower.setImageIcon(complicationProviderInfo.providerIcon);
            } else {
                complicationLower.setImageDrawable(defaultAddComplicationDrawable);
            }
        }
    }

    public void initializesColorsAndComplications(ProviderInfoRetriever providerInfoRetriever) {

        final int[] complicationIds = TeradataWatchService.getComplicationIds();

        providerInfoRetriever.retrieveProviderInfo(
                new ProviderInfoRetriever.OnProviderInfoReceivedCallback() {
                    @Override
                    public void onProviderInfoReceived(
                            int watchFaceComplicationId,
                            @Nullable ComplicationProviderInfo complicationProviderInfo) {
                        Log.d(TAG, "onProviderInfoReceived: " + complicationProviderInfo);
                        updateComplicationViews(watchFaceComplicationId, complicationProviderInfo);
                    }
                },
                watchFaceComponentName,
                complicationIds);
    }
}

