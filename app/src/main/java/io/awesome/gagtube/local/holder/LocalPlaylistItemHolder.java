package io.awesome.gagtube.local.holder;

import android.view.View;
import android.view.ViewGroup;

import java.text.DateFormat;

import io.awesome.gagtube.App;
import io.awesome.gagtube.R;
import io.awesome.gagtube.database.LocalItem;
import io.awesome.gagtube.database.playlist.PlaylistMetadataEntry;
import io.awesome.gagtube.local.LocalItemBuilder;
import io.awesome.gagtube.util.GlideUtils;
import io.awesome.gagtube.util.Localization;

public class LocalPlaylistItemHolder extends PlaylistItemHolder {
	
	public LocalPlaylistItemHolder(LocalItemBuilder infoItemBuilder, ViewGroup parent) {
		
		super(infoItemBuilder, parent);
	}
	
	@Override
	public void updateFromItem(final LocalItem localItem, final DateFormat dateFormat) {
		
		super.updateFromItem(localItem, dateFormat);
		
		if (!(localItem instanceof PlaylistMetadataEntry)) return;
		final PlaylistMetadataEntry item = (PlaylistMetadataEntry) localItem;
		
		itemTitleView.setText(item.name);
		itemStreamCountView.setText(Localization.localizeStreamCount(itemStreamCountView.getContext(), item.streamCount));
		itemUploaderView.setText(R.string.me);
		
		GlideUtils.loadThumbnail(App.applicationContext, itemThumbnailView, item.thumbnailUrl);
		itemMoreActions.setVisibility(itemBuilder.isShowOptionMenu() ? View.VISIBLE : View.GONE);
	}
}
