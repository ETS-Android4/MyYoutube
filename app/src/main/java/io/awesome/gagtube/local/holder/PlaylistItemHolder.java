package io.awesome.gagtube.local.holder;

import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.DateFormat;

import io.awesome.gagtube.R;
import io.awesome.gagtube.database.LocalItem;
import io.awesome.gagtube.local.LocalItemBuilder;

public abstract class PlaylistItemHolder extends LocalItemHolder {
    
    public final ImageView itemThumbnailView;
    public final TextView itemStreamCountView;
    public final TextView itemTitleView;
    public final TextView itemUploaderView;
    public final ImageButton itemMoreActions;

    private PlaylistItemHolder(LocalItemBuilder infoItemBuilder, int layoutId, ViewGroup parent) {
        
        super(infoItemBuilder, layoutId, parent);

        itemThumbnailView = itemView.findViewById(R.id.itemThumbnailView);
        itemTitleView = itemView.findViewById(R.id.itemTitleView);
        itemStreamCountView = itemView.findViewById(R.id.itemStreamCountView);
        itemUploaderView = itemView.findViewById(R.id.itemUploaderView);
        itemMoreActions = itemView.findViewById(R.id.btn_action);
    }

    PlaylistItemHolder(LocalItemBuilder infoItemBuilder, ViewGroup parent) {
        
        this(infoItemBuilder, R.layout.list_playlist_item, parent);
    }

    @Override
    public void updateFromItem(final LocalItem localItem, final DateFormat dateFormat) {
        
        itemView.setOnClickListener(view -> {
        
            if (itemBuilder.getOnItemSelectedListener() != null) {
                itemBuilder.getOnItemSelectedListener().selected(localItem);
            }
        });
    
        itemMoreActions.setOnClickListener(view -> {
        
            if (itemBuilder.getOnItemSelectedListener() != null) {
                itemBuilder.getOnItemSelectedListener().more(localItem, view);
            }
        });
    }
}
