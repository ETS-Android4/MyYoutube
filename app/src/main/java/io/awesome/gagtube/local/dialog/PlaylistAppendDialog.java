package io.awesome.gagtube.local.dialog;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import org.schabi.newpipe.extractor.stream.StreamInfo;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.awesome.gagtube.R;
import io.awesome.gagtube.database.GAGTubeDatabase;
import io.awesome.gagtube.database.LocalItem;
import io.awesome.gagtube.database.playlist.PlaylistMetadataEntry;
import io.awesome.gagtube.database.stream.model.StreamEntity;
import io.awesome.gagtube.local.LocalItemListAdapter;
import io.awesome.gagtube.local.playlist.LocalPlaylistManager;
import io.awesome.gagtube.player.playqueue.PlayQueueItem;
import io.awesome.gagtube.util.OnClickGesture;
import io.reactivex.Maybe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;

public final class PlaylistAppendDialog extends PlaylistDialog {
	private static final String TAG = PlaylistAppendDialog.class.getCanonicalName();
	
	private View view;
	@BindView(R.id.playlist_list) RecyclerView playlistRecyclerView;
	private LocalItemListAdapter playlistAdapter;
	
	private CompositeDisposable playlistDisposables = new CompositeDisposable();
	
	public static PlaylistAppendDialog fromStreamInfo(final StreamInfo info) {
		
		PlaylistAppendDialog dialog = new PlaylistAppendDialog();
		dialog.setInfo(Collections.singletonList(new StreamEntity(info)));
		return dialog;
	}
	
	public static PlaylistAppendDialog fromStreamInfoItems(final List<StreamInfoItem> items) {
		
		PlaylistAppendDialog dialog = new PlaylistAppendDialog();
		List<StreamEntity> entities = new ArrayList<>(items.size());
		
		for (final StreamInfoItem item : items) {
			entities.add(new StreamEntity(item));
		}
		dialog.setInfo(entities);
		return dialog;
	}
	
	public static PlaylistAppendDialog fromPlayQueueItems(final List<PlayQueueItem> items) {
		
		PlaylistAppendDialog dialog = new PlaylistAppendDialog();
		List<StreamEntity> entities = new ArrayList<>(items.size());
		
		for (final PlayQueueItem item : items) {
			entities.add(new StreamEntity(item));
		}
		dialog.setInfo(entities);
		return dialog;
	}
	
	// LifeCycle - Creation
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		
		view = inflater.inflate(R.layout.dialog_playlists, container);
		ButterKnife.bind(this, view);
		
		return view;
	}
	
	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		
		super.onViewCreated(view, savedInstanceState);
		
		final LocalPlaylistManager playlistManager = new LocalPlaylistManager(GAGTubeDatabase.getInstance(view.getContext()));
		
		playlistAdapter = new LocalItemListAdapter(getActivity(), false);
		playlistAdapter.setSelectedListener(new OnClickGesture<LocalItem>() {
			
			@Override
			public void selected(LocalItem selectedItem) {
				
				if (!(selectedItem instanceof PlaylistMetadataEntry) || getStreams() == null) return;
				
				onPlaylistSelected(playlistManager, (PlaylistMetadataEntry) selectedItem, getStreams());
			}
		});
		
		playlistRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
		playlistRecyclerView.setAdapter(playlistAdapter);
		
		playlistDisposables.add(playlistManager.getPlaylists()
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(this::onPlaylistsReceived));
	}
	
	@OnClick(R.id.newPlaylist)
	void onCreateNewPlaylist() {
		
		openCreatePlaylistDialog();
	}
	
	// LifeCycle - Destruction
	@Override
	public void onDestroyView() {
		
		super.onDestroyView();
		
		playlistDisposables.dispose();
		if (playlistAdapter != null) playlistAdapter.unsetSelectedListener();
		
		playlistDisposables.clear();
		playlistRecyclerView = null;
		playlistAdapter = null;
	}
	
	// Helper
	public void openCreatePlaylistDialog() {
		
		if (getFragmentManager() == null) return;
		
		PlaylistCreationDialog.newInstance(getStreams()).show(getFragmentManager(), TAG);
		getDialog().dismiss();
	}
	
	private void onPlaylistsReceived(@NonNull final List<PlaylistMetadataEntry> playlists) {
		
		if (playlists.isEmpty()) {
			openCreatePlaylistDialog();
			return;
		}
		
		if (playlistAdapter != null && playlistRecyclerView != null) {
			playlistAdapter.clearStreamItemList();
			playlistAdapter.addItems(playlists);
			playlistRecyclerView.setVisibility(View.VISIBLE);
		}
	}
	
	@SuppressLint("CheckResult")
	private void onPlaylistSelected(@NonNull LocalPlaylistManager manager, @NonNull PlaylistMetadataEntry playlist, @NonNull List<StreamEntity> streams) {
		
		if (getStreams() == null) return;
		
		final Toast successToast = Toast.makeText(view.getContext(), R.string.playlist_add_stream_success, Toast.LENGTH_SHORT);
		
		// append to playlist
		playlistDisposables.add(Maybe.zip(manager.appendToPlaylist(playlist.uid, streams),
				  // set playlist thumbnail using the last video thumbnail
				  manager.changePlaylistThumbnail(playlist.uid, streams.get(0).getThumbnailUrl()), (longs, integer) -> true)
				// apply schedulers
				.observeOn(AndroidSchedulers.mainThread()).subscribe(
				// onNext
				ignored -> successToast.show()));
		
		// dismiss dialog
		getDialog().dismiss();
	}
}