package com.teradata.wearable.model;

import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;

import com.teradata.wearable.config.ComplicationConfigRecyclerViewAdapter;
import com.teradata.wearable.model.configitems.MoreOptionsConfigItem;
import com.teradata.wearable.model.configitems.PreviewAndComplicationsConfigItem;
import com.teradata.wearable.model.configitems.ToggleConfigItem;
import com.teradata.wearable.watchface.TeradataWatchService;
import com.teradata.wearable.R;

import java.util.ArrayList;

/**
 * Settings properties
 * Created by jason on 2/20/18.
 */

public class ComplicationConfigData {

    /**
     * Interface all ConfigItems must implement so the {@link RecyclerView}'s Adapter associated
     * with the configuration activity knows what type of ViewHolder to inflate.
     */
    public interface ConfigItemType {
        int getConfigType();
    }

    /**
     * Returns Watch Face Service class associated with configuration Activity.
     */
    public static Class getWatchFaceServiceClass() {
        return TeradataWatchService.class;
    }

    /**
     * Includes all data to populate each of the 3 different custom
     * {@link RecyclerView.ViewHolder} types in {@link ComplicationConfigRecyclerViewAdapter}.
     */
    public static ArrayList<ConfigItemType> getDataToPopulateAdapter(Context context) {

        ArrayList<ConfigItemType> settingsConfigData = new ArrayList<>();

        // Data for watch face preview and complications UX in settings Activity.
        ConfigItemType complicationConfigItem = new PreviewAndComplicationsConfigItem(R.drawable.add_complication);
        settingsConfigData.add(complicationConfigItem);

        // Data for "more options" UX in settings Activity.
        ConfigItemType moreOptionsConfigItem = new MoreOptionsConfigItem(R.drawable.ic_expand_more_white_18dp);
        settingsConfigData.add(moreOptionsConfigItem);

        // Show date toggle
        ConfigItemType showDate = new ToggleConfigItem(
                context.getString(R.string.always_show_date_label),
                R.drawable.ic_show_date_on_ambient_screen_24dp,
                R.drawable.ic_hide_date_on_ambient_screen_24dp,
                R.string.always_show_date_pref,
                ComplicationConfigRecyclerViewAdapter.TYPE_SHOW_DATE);
        settingsConfigData.add(showDate);

        // Show seconds toggle
        ConfigItemType showSeconds = new ToggleConfigItem(
                context.getString(R.string.show_seconds_label),
                R.drawable.ic_show_seconds_24dp,
                R.drawable.ic_hide_seconds_24dp,
                R.string.show_seconds_pref,
                ComplicationConfigRecyclerViewAdapter.TYPE_SHOW_SECONDS);
        settingsConfigData.add(showSeconds);

        // Show seconds toggle
        ConfigItemType batteryStatus = new ToggleConfigItem(
                context.getString(R.string.show_battery_status_label),
                R.drawable.ic_show_battery_level_24dp,
                R.drawable.ic_hide_battery_level_24dp,
                R.string.show_battery_status_pref,
                ComplicationConfigRecyclerViewAdapter.TYPE_SHOW_BATTERY_STATUS);
        settingsConfigData.add(batteryStatus);

        // 24 hour clock toggle
        ConfigItemType militaryTime = new ToggleConfigItem(
                context.getString(R.string.military_time_label),
                R.drawable.ic_milirary_time_24_hour_24dp,
                R.drawable.ic_milirary_time_12_hour_24dp,
                R.string.military_time_pref,
                ComplicationConfigRecyclerViewAdapter.TYPE_SHOW_DATE);
        settingsConfigData.add(militaryTime);

        // ambient drift toggle
        ConfigItemType ambientDrift = new ToggleConfigItem(
                context.getString(R.string.ambient_drift_label),
                R.drawable.ic_random_ambint_drift_24dp,
                R.drawable.ic_random_ambint_drift_off_24dp,
                R.string.ambient_drift_pref,
                ComplicationConfigRecyclerViewAdapter.TYPE_AMBIENT_DRIFT);
        settingsConfigData.add(ambientDrift);

        // ambient drift toggle
        ConfigItemType showNotifications = new ToggleConfigItem(
                context.getString(R.string.config_unread_notifications_label),
                R.drawable.ic_show_notification_alert_24dp,
                R.drawable.ic_hide_notification_alert_24dp,
                R.string.saved_unread_notifications_pref,
                ComplicationConfigRecyclerViewAdapter.TYPE_NOTIFICATIONS);
        settingsConfigData.add(showNotifications);

        return settingsConfigData;
    }
}
