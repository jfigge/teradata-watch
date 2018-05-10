/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.teradata.wearable.config;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.widget.RecyclerView;
import android.support.wearable.complications.ComplicationProviderInfo;
import android.support.wearable.complications.ProviderInfoRetriever;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.teradata.wearable.model.ComplicationConfigData.ConfigItemType;
import com.teradata.wearable.model.configitems.MoreOptionsConfigItem;
import com.teradata.wearable.model.configitems.PreviewAndComplicationsConfigItem;
import com.teradata.wearable.model.configitems.ToggleConfigItem;
import com.teradata.wearable.model.holders.MoreOptionsViewHolder;
import com.teradata.wearable.model.holders.PreviewAndComplicationsViewHolder;
import com.teradata.wearable.model.holders.ToggleOptionsViewHolder;
import com.teradata.wearable.watchface.TeradataWatchService;

import java.util.ArrayList;
import java.util.concurrent.Executors;

import com.teradata.wearable.R;

/**
 * The watch-side config activity for {@link TeradataWatchService}, which allows for setting
 * the four complications of watch face.
 */
public class ComplicationConfigRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = ComplicationConfigRecyclerViewAdapter.class.getSimpleName();

    public static final int COMPLICATION_CONFIG_REQUEST_CODE  = 1001;

    /**
     * Used by associated watch face ({@link TeradataWatchService}) to let this
     * configuration Activity know which complication locations are supported, their ids, and
     * supported complication data types.
     */
    public enum ComplicationLocation {
        UPPER,
        LOWER
    }

    public static final int TYPE_PREVIEW_AND_COMPLICATIONS_CONFIG = 0;
    public static final int TYPE_MORE_OPTIONS = 1;
    public static final int TYPE_SHOW_DATE = 2;
    public static final int TYPE_SHOW_SECONDS = 3;
    public static final int TYPE_SHOW_BATTERY_STATUS = 4;
    public static final int TYPE_MILITARY_TIME = 5;
    public static final int TYPE_AMBIENT_DRIFT = 6;
    public static final int TYPE_NOTIFICATIONS = 7;

    private ArrayList<ConfigItemType> settingsDataSet;

    private Context context;
    private Class watchServiceClass;

    private SharedPreferences sharedPreferences;

    // Required to retrieve complication data from watch face for preview.
    private ProviderInfoRetriever providerInfoRetriever;

    private PreviewAndComplicationsViewHolder previewAndComplicationsViewHolder;

    ComplicationConfigRecyclerViewAdapter(Context context, Class watchServiceClass, ArrayList<ConfigItemType> settingsDataSet) {
        this.context           = context;
        this.settingsDataSet   = settingsDataSet;
        this.watchServiceClass = watchServiceClass;

        sharedPreferences = context.getSharedPreferences(context.getString(R.string.analog_complication_preference_file_key), Context.MODE_PRIVATE);

        providerInfoRetriever = new ProviderInfoRetriever(context, Executors.newCachedThreadPool());
        providerInfoRetriever.init();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Log.d(TAG, "onCreateViewHolder(): viewType: " + viewType);

        RecyclerView.ViewHolder viewHolder = null;

        switch (viewType) {
            case TYPE_PREVIEW_AND_COMPLICATIONS_CONFIG:
                // Need direct reference to watch face preview view holder to update watch face
                // preview based on selections from the user.
                previewAndComplicationsViewHolder =
                        new PreviewAndComplicationsViewHolder(
                                LayoutInflater.from(parent.getContext())
                                        .inflate(
                                                R.layout.settings_activity,
                                                parent,
                                                false),
                                watchServiceClass,
                                context);
                viewHolder = previewAndComplicationsViewHolder;
                break;

            case TYPE_MORE_OPTIONS:
                viewHolder =
                        new MoreOptionsViewHolder(
                                LayoutInflater.from(parent.getContext())
                                        .inflate(
                                                R.layout.config_list_more_options_item,
                                                parent,
                                                false));
                break;

            case TYPE_SHOW_DATE:
                viewHolder =
                        new ToggleOptionsViewHolder(
                                LayoutInflater.from(parent.getContext())
                                        .inflate(
                                                R.layout.config_list_show_date_item,
                                                parent,
                                                false),
                                sharedPreferences,
                                R.id.show_date);
                break;

            case TYPE_SHOW_SECONDS:
                viewHolder =
                        new ToggleOptionsViewHolder(
                                LayoutInflater.from(parent.getContext())
                                        .inflate(
                                                R.layout.config_list_show_seconds_item,
                                                parent,
                                                false),
                                sharedPreferences,
                                R.id.show_seconds);
                break;

            case TYPE_SHOW_BATTERY_STATUS:
                viewHolder =
                        new ToggleOptionsViewHolder(
                                LayoutInflater.from(parent.getContext())
                                        .inflate(
                                                R.layout.config_list_show_battery_status_item,
                                                parent,
                                                false),
                                sharedPreferences,
                                R.id.show_battery_status);
                break;

            case TYPE_MILITARY_TIME:
                viewHolder =
                        new ToggleOptionsViewHolder(
                                LayoutInflater.from(parent.getContext())
                                        .inflate(
                                                R.layout.config_list_military_time_item,
                                                parent,
                                                false),
                                sharedPreferences,
                                R.id.military_time);
                break;

            case TYPE_AMBIENT_DRIFT:
                viewHolder =
                        new ToggleOptionsViewHolder(
                                LayoutInflater.from(parent.getContext())
                                        .inflate(
                                                R.layout.config_list_ambient_drift_item,
                                                parent,
                                                false),
                                sharedPreferences,
                                R.id.ambient_drift);
                break;

            case TYPE_NOTIFICATIONS:
                viewHolder =
                        new ToggleOptionsViewHolder(
                                LayoutInflater.from(parent.getContext())
                                        .inflate(
                                                R.layout.config_list_notification_item,
                                                parent,
                                                false),
                                sharedPreferences,
                                R.id.show_notifications);
                break;
        }

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        Log.d(TAG, "Element " + position + " set.");

        // Pulls all data required for creating the UX for the specific setting option.
        ConfigItemType configItemType = settingsDataSet.get(position);

        switch (viewHolder.getItemViewType()) {
            case TYPE_PREVIEW_AND_COMPLICATIONS_CONFIG:
                Log.d(TAG, "1");
                PreviewAndComplicationsViewHolder previewAndComplicationsViewHolder = (PreviewAndComplicationsViewHolder) viewHolder;
                PreviewAndComplicationsConfigItem previewAndComplicationsConfigItem = (PreviewAndComplicationsConfigItem) configItemType;

                int defaultComplicationResourceId = previewAndComplicationsConfigItem.getDefaultComplicationResourceId();
                previewAndComplicationsViewHolder.setDefaultComplicationDrawable(defaultComplicationResourceId);
                previewAndComplicationsViewHolder.initializesColorsAndComplications(providerInfoRetriever);
                break;

            case TYPE_MORE_OPTIONS:
                Log.d(TAG, "2");
                MoreOptionsViewHolder moreOptionsViewHolder = (MoreOptionsViewHolder) viewHolder;
                MoreOptionsConfigItem moreOptionsConfigItem = (MoreOptionsConfigItem) configItemType;
                moreOptionsViewHolder.setIcon(moreOptionsConfigItem.getIconResourceId());
                break;

            case TYPE_SHOW_DATE:
                Log.d(TAG, "3");
                ToggleOptionsViewHolder showDate    = (ToggleOptionsViewHolder) viewHolder;
                ToggleConfigItem showDateConfigItem = (ToggleConfigItem) configItemType;

                int showDateEnabledIconResourceId  = showDateConfigItem.getIconEnabledResourceId();
                int showDateDisabledIconResourceId = showDateConfigItem.getIconDisabledResourceId();

                String showDateName      = showDateConfigItem.getName();
                int showDateSharedPrefId = showDateConfigItem.getSharedPrefId();

                showDate.setIcons(showDateEnabledIconResourceId, showDateDisabledIconResourceId);
                showDate.setName(showDateName);
                showDate.setSharedPrefId(showDateSharedPrefId, true);
                break;

            case TYPE_SHOW_SECONDS:
                Log.d(TAG, "4");
                ToggleOptionsViewHolder showSeconds    = (ToggleOptionsViewHolder) viewHolder;
                ToggleConfigItem showSecondsConfigItem = (ToggleConfigItem) configItemType;

                int showsecondsEnabledIconResourceId  = showSecondsConfigItem.getIconEnabledResourceId();
                int showSecondsDisabledIconResourceId = showSecondsConfigItem.getIconDisabledResourceId();

                String showSecondsName      = showSecondsConfigItem.getName();
                int showSecondsSharedPrefId = showSecondsConfigItem.getSharedPrefId();

                showSeconds.setIcons(showsecondsEnabledIconResourceId, showSecondsDisabledIconResourceId);
                showSeconds.setName(showSecondsName);
                showSeconds.setSharedPrefId(showSecondsSharedPrefId, false);
                break;

            case TYPE_SHOW_BATTERY_STATUS:
                Log.d(TAG, "5");
                ToggleOptionsViewHolder showBattery    = (ToggleOptionsViewHolder) viewHolder;
                ToggleConfigItem showBatteryConfigItem = (ToggleConfigItem) configItemType;

                int showBatteryEnabledIconResourceId  = showBatteryConfigItem.getIconEnabledResourceId();
                int showBatteryDisabledIconResourceId = showBatteryConfigItem.getIconDisabledResourceId();

                String showBatteryName      = showBatteryConfigItem.getName();
                int showBatterySharedPrefId = showBatteryConfigItem.getSharedPrefId();

                showBattery.setIcons(showBatteryEnabledIconResourceId, showBatteryDisabledIconResourceId);
                showBattery.setName(showBatteryName);
                showBattery.setSharedPrefId(showBatterySharedPrefId, true);
                break;

            case TYPE_MILITARY_TIME:
                Log.d(TAG, "6");
                ToggleOptionsViewHolder militaryTime    = (ToggleOptionsViewHolder) viewHolder;
                ToggleConfigItem militaryTimeConfigItem = (ToggleConfigItem) configItemType;

                int militaryTimeEnabledIconResourceId  = militaryTimeConfigItem.getIconEnabledResourceId();
                int militaryTimeDisabledIconResourceId = militaryTimeConfigItem.getIconDisabledResourceId();

                String militaryTimeName      = militaryTimeConfigItem.getName();
                int militaryTimeSharedPrefId = militaryTimeConfigItem.getSharedPrefId();

                militaryTime.setIcons(militaryTimeEnabledIconResourceId, militaryTimeDisabledIconResourceId);
                militaryTime.setName(militaryTimeName);
                militaryTime.setSharedPrefId(militaryTimeSharedPrefId, true);
                break;

            case TYPE_AMBIENT_DRIFT:
                Log.d(TAG, "7");
                ToggleOptionsViewHolder ambientDrift    = (ToggleOptionsViewHolder) viewHolder;
                ToggleConfigItem ambientDriftConfigItem = (ToggleConfigItem) configItemType;

                int ambientDriftEnabledResourceIconId  = ambientDriftConfigItem.getIconEnabledResourceId();
                int ambientDriftDisabledIconResourceId = ambientDriftConfigItem.getIconDisabledResourceId();

                String ambientDriftName      = ambientDriftConfigItem.getName();
                int ambientDriftSharedPrefId = ambientDriftConfigItem.getSharedPrefId();

                ambientDrift.setIcons(ambientDriftEnabledResourceIconId, ambientDriftDisabledIconResourceId);
                ambientDrift.setName(ambientDriftName);
                ambientDrift.setSharedPrefId(ambientDriftSharedPrefId, true);
                break;

            case TYPE_NOTIFICATIONS:
                Log.d(TAG, "8");
                ToggleOptionsViewHolder notifications    = (ToggleOptionsViewHolder) viewHolder;
                ToggleConfigItem notificationsConfigItem = (ToggleConfigItem) configItemType;

                int notificationsEnabledResourceIconId  = notificationsConfigItem.getIconEnabledResourceId();
                int notificationsDisabledIconResourceId = notificationsConfigItem.getIconDisabledResourceId();

                String notificationsName      = notificationsConfigItem.getName();
                int notificationsSharedPrefId = notificationsConfigItem.getSharedPrefId();

                notifications.setIcons(notificationsEnabledResourceIconId, notificationsDisabledIconResourceId);
                notifications.setName(notificationsName);
                notifications.setSharedPrefId(notificationsSharedPrefId, true);
                break;
        }
    }

    @Override
    public int getItemViewType(int position) {
        ConfigItemType configItemType = settingsDataSet.get(position);
        return configItemType.getConfigType();
    }

    @Override
    public int getItemCount() {
        return settingsDataSet.size();
    }

    /** Updates the selected complication id saved earlier with the new information. */
    void updateSelectedComplication(ComplicationProviderInfo complicationProviderInfo) {

        Log.d(TAG, "updateSelectedComplication: " + previewAndComplicationsViewHolder);

        // Checks if view is inflated and complication id is valid.
        if (previewAndComplicationsViewHolder != null) {
            previewAndComplicationsViewHolder.updateComplicationViews(complicationProviderInfo);
        }
    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        // Required to release retriever for active complication data on detach.
        providerInfoRetriever.release();
    }
}

