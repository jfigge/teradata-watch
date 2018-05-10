package com.teradata.wearable.config;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.wearable.complications.ComplicationProviderInfo;
import android.support.wearable.complications.ProviderChooserIntent;
import android.support.wear.widget.WearableRecyclerView;
import android.util.Log;

import com.teradata.wearable.R;
import com.teradata.wearable.model.ComplicationConfigData;
import com.teradata.wearable.watchface.TeradataWatchService;

/**
 * The watch-side config activity for {@link TeradataWatchService}, which
 * allows for setting the four complications of watch face along with numerous
 * toggleable settings
 * Created by jason on 2/20/18.
 */

public class ComplicationConfigActivity extends Activity {

    private static final String TAG = ComplicationConfigActivity.class.getSimpleName();

    static final int COMPLICATION_CONFIG_REQUEST_CODE = 1001;

    private ComplicationConfigRecyclerViewAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_complication_config);

        adapter = new ComplicationConfigRecyclerViewAdapter(
                getApplicationContext(),
                ComplicationConfigData.getWatchFaceServiceClass(),
                ComplicationConfigData.getDataToPopulateAdapter(this));

        WearableRecyclerView wearableRecyclerView = findViewById(R.id.wearable_recycler_view);

        // Aligns the first and last items on the list vertically centered on the screen.
        wearableRecyclerView.setEdgeItemsCenteringEnabled(true);

        wearableRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Improves performance because we know changes in content do not change the layout size of
        // the RecyclerView.
        wearableRecyclerView.setHasFixedSize(true);

        wearableRecyclerView.setAdapter(adapter);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == COMPLICATION_CONFIG_REQUEST_CODE
                && resultCode == RESULT_OK) {

            // Retrieves information for selected Complication provider.
            ComplicationProviderInfo complicationProviderInfo =
                    data.getParcelableExtra(ProviderChooserIntent.EXTRA_PROVIDER_INFO);
            Log.d(TAG, "Provider: " + complicationProviderInfo);

            // Updates preview with new complication information for selected complication id.
            // Note: complication id is saved and tracked in the adapter class.
            adapter.updateSelectedComplication(complicationProviderInfo);

        }
    }
}
