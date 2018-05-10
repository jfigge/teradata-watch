package com.teradata.wearable.model.configitems;

import com.teradata.wearable.model.ComplicationConfigData;

/**
 * Data for whether or not the date is shown
 */
public class ToggleConfigItem implements ComplicationConfigData.ConfigItemType {

    private String name;
    private int iconEnabledResourceId;
    private int iconDisabledResourceId;
    private int sharedPrefId;
    private int configType;

    public ToggleConfigItem(String name, int iconEnabledResourceId, int iconDisabledResourceId, int sharedPrefId, int configType) {
        this.name = name;
        this.iconEnabledResourceId = iconEnabledResourceId;
        this.iconDisabledResourceId = iconDisabledResourceId;
        this.sharedPrefId = sharedPrefId;
        this.configType = configType;
    }

    public String getName() {
        return name;
    }

    public int getIconEnabledResourceId() {
        return iconEnabledResourceId;
    }

    public int getIconDisabledResourceId() {
        return iconDisabledResourceId;
    }

    public int getSharedPrefId() {
        return sharedPrefId;
    }

    @Override
    public int getConfigType() {
        return configType;
    }
}

