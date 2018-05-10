package com.teradata.wearable.model.configitems;

import com.teradata.wearable.config.ComplicationConfigRecyclerViewAdapter;
import com.teradata.wearable.model.ComplicationConfigData;

/**
 * Data for Watch Face Preview with Complications Preview item in RecyclerView.
 * Created by jason on 3/17/18.
 */
public class PreviewAndComplicationsConfigItem implements ComplicationConfigData.ConfigItemType {

    private int defaultComplicationResourceId;

    public PreviewAndComplicationsConfigItem(int defaultComplicationResourceId) {
        this.defaultComplicationResourceId            = defaultComplicationResourceId;
    }

    public int getDefaultComplicationResourceId() {
        return defaultComplicationResourceId;
    }

    @Override
    public int getConfigType() {
        return ComplicationConfigRecyclerViewAdapter.TYPE_PREVIEW_AND_COMPLICATIONS_CONFIG;
    }
}