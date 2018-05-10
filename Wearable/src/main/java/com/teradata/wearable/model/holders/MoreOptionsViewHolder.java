package com.teradata.wearable.model.holders;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;

import com.teradata.wearable.R;

/**
 * Displays icon to indicate there are more options below the fold.
 * Created by jason on 3/17/18
 */
public class MoreOptionsViewHolder extends RecyclerView.ViewHolder {

    private ImageView mMoreOptionsImageView;

    public MoreOptionsViewHolder(View view) {
        super(view);
        mMoreOptionsImageView = view.findViewById(R.id.more_options_image_view);
    }

    public void setIcon(int resourceId) {
        Context context = mMoreOptionsImageView.getContext();
        mMoreOptionsImageView.setImageDrawable(context.getDrawable(resourceId));
    }
}