package io.awesome.gagtube.local.dialog;

import android.os.Bundle;

import com.google.api.services.youtube.model.Playlist;

import java.util.List;
import java.util.Queue;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import io.awesome.gagtube.database.stream.model.StreamEntity;
import io.awesome.gagtube.util.StateSaver;

public abstract class PlaylistDialog extends DialogFragment implements StateSaver.WriteRead {

    private List<StreamEntity> streamEntities;
    private List<Playlist> playlists;

    private StateSaver.SavedState savedState;

    protected void setInfo(final List<StreamEntity> entities) {
        this.streamEntities = entities;
    }

    protected List<StreamEntity> getStreams() {
        return streamEntities;
    }
    
    public List<Playlist> getPlaylists() {
        return playlists;
    }
    
    public void setPlaylists(List<Playlist> playlists) {
        this.playlists = playlists;
    }
    
    // LifeCycle
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        
        super.onCreate(savedInstanceState);
        
        savedState = StateSaver.tryToRestore(savedInstanceState, this);
    }

    @Override
    public void onDestroy() {
        
        super.onDestroy();
        
        StateSaver.onDestroy(savedState);
    }

    // State Saving
    @Override
    public String generateSuffix() {
        
        final int size = streamEntities == null ? 0 : streamEntities.size();
        return "." + size + ".list";
    }

    @Override
    public void writeTo(Queue<Object> objectsToSave) {
        objectsToSave.add(streamEntities);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void readFrom(@NonNull Queue<Object> savedObjects) {
        streamEntities = (List<StreamEntity>) savedObjects.poll();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        
        super.onSaveInstanceState(outState);
        
        if (getActivity() != null) {
            savedState = StateSaver.tryToSave(getActivity().isChangingConfigurations(), savedState, outState, this);
        }
    }
}
