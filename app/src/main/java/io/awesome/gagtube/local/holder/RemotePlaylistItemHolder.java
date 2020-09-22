package io.awesome.gagtube.local.holder;

import android.text.TextUtils;
import android.view.ViewGroup;

import org.schabi.newpipe.extractor.NewPipe;

import java.text.DateFormat;

import io.awesome.gagtube.App;
import io.awesome.gagtube.database.LocalItem;
import io.awesome.gagtube.database.playlist.model.PlaylistRemoteEntity;
import io.awesome.gagtube.local.LocalItemBuilder;
import io.awesome.gagtube.util.GlideUtils;
import io.awesome.gagtube.util.Localization;

public class RemotePlaylistItemHolder extends PlaylistItemHolder {
	
	public RemotePlaylistItemHolder(LocalItemBuilder infoItemBuilder, ViewGroup parent) {
		
		super(infoItemBuilder, parent);
	}
	
	@Override
	public void updateFromItem(final LocalItem localItem, final DateFormat dateFormat) {
		
		super.updateFromItem(localItem, dateFormat);
		
		if (!(localItem instanceof PlaylistRemoteEntity)) return;
		final PlaylistRemoteEntity item = (PlaylistRemoteEntity) localItem;
		
		itemTitleView.setText(item.getName());
		itemStreamCountView.setText(Localization.localizeStreamCount(itemStreamCountView.getContext(), item.getStreamCount()));
		if (!TextUtils.isEmpty(item.getUploader())) {
			itemUploaderView.setText(item.getUploader());
		}
		else {
			itemUploaderView.setText(NewPipe.getNameOfService(item.getServiceId()));
		}
		
		GlideUtils.loadThumbnail(App.applicationContext, itemThumbnailView, item.getThumbnailUrl());
	}
}
