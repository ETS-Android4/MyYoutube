package io.awesome.gultube.local.dialog;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import butterknife.BindView;
import butterknife.ButterKnife;
import io.awesome.gultube.R;

import io.awesome.gultube.database.GAGTubeDatabase;
import io.awesome.gultube.database.stream.model.StreamEntity;
import io.awesome.gultube.local.playlist.LocalPlaylistManager;
import io.reactivex.android.schedulers.AndroidSchedulers;

public final class PlaylistCreationDialog extends PlaylistDialog {

	@BindView(R.id.playlist_name) TextInputEditText editText;
	private Activity activity;
	
	public static PlaylistCreationDialog newInstance(final List<StreamEntity> streams) {
		
		PlaylistCreationDialog dialog = new PlaylistCreationDialog();
		dialog.setInfo(streams);
		return dialog;
	}
	
	public static PlaylistCreationDialog newInstance() {
		return new PlaylistCreationDialog();
	}
	
	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		activity = getActivity();
	}
	
	@NonNull
	@Override
	public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
		
		View dialogView = View.inflate(getContext(), R.layout.dialog_playlist_name, null);
		ButterKnife.bind(this, dialogView);
		

		@SuppressLint("CheckResult") final MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(getContext())
				.setTitle(R.string.create_new_playlist)
				.setView(dialogView)
				.setCancelable(true)
				.setNegativeButton(R.string.cancel, null)
				.setPositiveButton(R.string.create, (dialogInterface, i) -> {
					
					final String name = editText.getText().toString();
					final LocalPlaylistManager playlistManager = new LocalPlaylistManager(GAGTubeDatabase.getInstance(dialogView.getContext()));
					final Toast successToast = Toast.makeText(getActivity(), R.string.playlist_creation_success, Toast.LENGTH_SHORT);
					
					// create playlist
					if (getStreams() != null) {
						playlistManager.createPlaylist(name, getStreams()).observeOn(AndroidSchedulers.mainThread()).subscribe(longs -> successToast.show());
					}
					else {
						playlistManager.createPlaylist(name).observeOn(AndroidSchedulers.mainThread()).subscribe(longs -> successToast.show());
					}
				});
		
		return dialogBuilder.create();
	}

}
