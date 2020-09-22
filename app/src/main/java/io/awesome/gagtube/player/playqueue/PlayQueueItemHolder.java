package io.awesome.gagtube.player.playqueue;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;
import io.awesome.gagtube.R;

public class PlayQueueItemHolder extends RecyclerView.ViewHolder {

    public final TextView itemVideoTitleView, itemDurationView, itemAdditionalDetailsView;
    public final ImageView itemSelected, itemThumbnailView, itemHandle;

    public final View itemRoot;

    public PlayQueueItemHolder(View rootView) {
        
        super(rootView);
        
        itemRoot = rootView.findViewById(R.id.itemRoot);
        itemVideoTitleView = rootView.findViewById(R.id.itemVideoTitleView);
        itemDurationView = rootView.findViewById(R.id.itemDurationView);
        itemAdditionalDetailsView = rootView.findViewById(R.id.itemAdditionalDetails);
        itemSelected = rootView.findViewById(R.id.itemSelected);
        itemThumbnailView = rootView.findViewById(R.id.itemThumbnailView);
        itemHandle = rootView.findViewById(R.id.itemHandle);
    }
}
