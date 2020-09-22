package io.awesome.gagtube.database.playlist.dao;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Query;
import io.awesome.gagtube.database.BasicDAO;
import io.awesome.gagtube.database.playlist.model.PlaylistEntity;
import io.reactivex.Flowable;

@Dao
public abstract class PlaylistDAO implements BasicDAO<PlaylistEntity> {
    @Override
    @Query("SELECT * FROM " + PlaylistEntity.PLAYLIST_TABLE)
    public abstract Flowable<List<PlaylistEntity>> getAll();

    @Override
    @Query("DELETE FROM " + PlaylistEntity.PLAYLIST_TABLE)
    public abstract int deleteAll();

    @Override
    public Flowable<List<PlaylistEntity>> listByService(int serviceId) {
        throw new UnsupportedOperationException();
    }

    @Query("SELECT * FROM " + PlaylistEntity.PLAYLIST_TABLE + " WHERE " + PlaylistEntity.PLAYLIST_ID + " = :playlistId")
    public abstract Flowable<List<PlaylistEntity>> getPlaylist(final long playlistId);

    @Query("DELETE FROM " + PlaylistEntity.PLAYLIST_TABLE + " WHERE " + PlaylistEntity.PLAYLIST_ID + " = :playlistId")
    public abstract int deletePlaylist(final long playlistId);
}
