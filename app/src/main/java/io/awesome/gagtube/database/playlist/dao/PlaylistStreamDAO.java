package io.awesome.gagtube.database.playlist.dao;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Query;
import androidx.room.Transaction;
import io.awesome.gagtube.database.BasicDAO;
import io.awesome.gagtube.database.playlist.PlaylistMetadataEntry;
import io.awesome.gagtube.database.playlist.PlaylistStreamEntry;
import io.awesome.gagtube.database.playlist.model.PlaylistEntity;
import io.awesome.gagtube.database.playlist.model.PlaylistStreamEntity;
import io.awesome.gagtube.database.stream.model.StreamEntity;
import io.reactivex.Flowable;

@Dao
public abstract class PlaylistStreamDAO implements BasicDAO<PlaylistStreamEntity> {
    @Override
    @Query("SELECT * FROM " + PlaylistStreamEntity.PLAYLIST_STREAM_JOIN_TABLE)
    public abstract Flowable<List<PlaylistStreamEntity>> getAll();

    @Override
    @Query("DELETE FROM " + PlaylistStreamEntity.PLAYLIST_STREAM_JOIN_TABLE)
    public abstract int deleteAll();

    @Override
    public Flowable<List<PlaylistStreamEntity>> listByService(int serviceId) {
        throw new UnsupportedOperationException();
    }

    @Query("DELETE FROM " + PlaylistStreamEntity.PLAYLIST_STREAM_JOIN_TABLE +
            " WHERE " + PlaylistStreamEntity.JOIN_PLAYLIST_ID + " = :playlistId")
    public abstract void deleteBatch(final long playlistId);

    @Query("SELECT COALESCE(MAX(" + PlaylistStreamEntity.JOIN_INDEX + "), -1)" +
            " FROM " + PlaylistStreamEntity.PLAYLIST_STREAM_JOIN_TABLE +
            " WHERE " + PlaylistStreamEntity.JOIN_PLAYLIST_ID + " = :playlistId")
    public abstract Flowable<Integer> getMaximumIndexOf(final long playlistId);

    @Transaction
    @Query("SELECT * FROM " + StreamEntity.STREAM_TABLE + " INNER JOIN " +
            // get ids of streams of the given playlist
            "(SELECT " + PlaylistStreamEntity.JOIN_STREAM_ID + "," + PlaylistStreamEntity.JOIN_INDEX +
            " FROM " + PlaylistStreamEntity.PLAYLIST_STREAM_JOIN_TABLE +
            " WHERE " + PlaylistStreamEntity.JOIN_PLAYLIST_ID + " = :playlistId)" +

            // then merge with the stream metadata
            " ON " + StreamEntity.STREAM_ID + " = " + PlaylistStreamEntity.JOIN_STREAM_ID +
            " ORDER BY " + PlaylistStreamEntity.JOIN_INDEX + " ASC")
    public abstract Flowable<List<PlaylistStreamEntry>> getOrderedStreamsOf(long playlistId);

    @Transaction
    @Query("SELECT " + PlaylistEntity.PLAYLIST_ID + ", " + PlaylistEntity.PLAYLIST_NAME + ", " +
            PlaylistEntity.PLAYLIST_THUMBNAIL_URL + ", " +
            "COUNT(" + PlaylistStreamEntity.JOIN_PLAYLIST_ID + ") AS " + PlaylistMetadataEntry.PLAYLIST_STREAM_COUNT +

            " FROM " + PlaylistEntity.PLAYLIST_TABLE +
            " LEFT JOIN " + PlaylistStreamEntity.PLAYLIST_STREAM_JOIN_TABLE +
            " ON " + PlaylistEntity.PLAYLIST_ID + " = " + PlaylistStreamEntity.JOIN_PLAYLIST_ID +
            " GROUP BY " + PlaylistEntity.PLAYLIST_ID +
            " ORDER BY " + PlaylistEntity.PLAYLIST_NAME + " COLLATE NOCASE ASC")
    public abstract Flowable<List<PlaylistMetadataEntry>> getPlaylistMetadata();
}
