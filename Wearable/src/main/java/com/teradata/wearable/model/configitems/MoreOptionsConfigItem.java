package com.teradata.wearable.model.configitems;

import com.teradata.wearable.config.ComplicationConfigRecyclerViewAdapter;
import com.teradata.wearable.model.ComplicationConfigData;

/**
 * Data for "more options" item in RecyclerView.
 * Created by jason on 3/17/18.
 */
public class MoreOptionsConfigItem implements ComplicationConfigData.ConfigItemType {

    private int iconResourceId;

    public MoreOptionsConfigItem(int iconResourceId) {
        this.iconResourceId = iconResourceId;
    }

    public int getIconResourceId() {
        return iconResourceId;
    }

    @Override
    public int getConfigType() {
        return ComplicationConfigRecyclerViewAdapter.TYPE_MORE_OPTIONS;
    }
}