package com.teradata.wearable.model.holders;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Switch;

/**
 * Simple view holder for toggleable setting entries
 * Created by jason on 3/17/18.
 */
public class ToggleOptionsViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

    private static final String TAG = ToggleOptionsViewHolder.class.getSimpleName();

    private Switch toggleSwitch;

    private int enabledIconResourceId;
    private int disabledIconResourceId;

    private int sharedPrefResourceId;

    private SharedPreferences sharedPreferences;

    public ToggleOptionsViewHolder(View view, SharedPreferences sharedPreferences, int resourceId) {
        super(view);

        this.sharedPreferences = sharedPreferences;
        this.toggleSwitch      = view.findViewById(resourceId);
        Log.d(TAG, "is null: " + (toggleSwitch == null) + ", resourceId: " + resourceId);
        view.setOnClickListener(this);
    }

    public void setName(String name) {
        toggleSwitch.setText(name);
    }

    public void setIcons(int enabledIconResourceId, int disabledIconResourceId) {

        this.enabledIconResourceId = enabledIconResourceId;
        this.disabledIconResourceId = disabledIconResourceId;

        Context context = toggleSwitch.getContext();

        // Set default to enabled.
        toggleSwitch.setCompoundDrawablesWithIntrinsicBounds(
                context.getDrawable(this.enabledIconResourceId), null, null, null);
    }

    public void setSharedPrefId(int sharedPrefId, boolean defaultState) {
        sharedPrefResourceId = sharedPrefId;

        if (toggleSwitch != null) {
            Context context = toggleSwitch.getContext();
            String sharedPreferenceString = context.getString(sharedPrefResourceId);
            Boolean currentState = sharedPreferences.getBoolean(sharedPreferenceString, sharedPreferences.getBoolean(sharedPreferenceString, defaultState));

            updateIcon(context, currentState);
        }
    }

    private void updateIcon(Context context, Boolean currentState) {
        int currentIconResourceId;

        if (currentState) {
            currentIconResourceId = enabledIconResourceId;
        } else {
            currentIconResourceId = disabledIconResourceId;
        }

        toggleSwitch.setChecked(currentState);
        toggleSwitch.setCompoundDrawablesWithIntrinsicBounds(
                context.getDrawable(currentIconResourceId), null, null, null);
    }

    @Override
    public void onClick(View view) {
        int position = getAdapterPosition();
        Log.d(TAG, "Complication onClick() position: " + position);

        Context context = view.getContext();
        String sharedPreferenceString = context.getString(sharedPrefResourceId);

        // Since user clicked on a switch, new state should be opposite of current state.
        Boolean newState = !sharedPreferences.getBoolean(sharedPreferenceString, true);

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(sharedPreferenceString, newState);
        editor.apply();

        updateIcon(context, newState);
    }
}
