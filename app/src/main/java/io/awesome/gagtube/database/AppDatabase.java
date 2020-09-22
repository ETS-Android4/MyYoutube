package io.awesome.gagtube.database;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import io.awesome.gagtube.database.history.dao.SearchHistoryDAO;
import io.awesome.gagtube.database.history.dao.StreamHistoryDAO;
import io.awesome.gagtube.database.history.model.SearchHistoryEntry;
import io.awesome.gagtube.database.history.model.StreamHistoryEntity;
import io.awesome.gagtube.database.stream.dao.StreamDAO;
import io.awesome.gagtube.database.stream.dao.StreamStateDAO;
import io.awesome.gagtube.database.stream.model.StreamEntity;
import io.awesome.gagtube.database.stream.model.StreamStateEntity;
import io.awesome.gagtube.database.playlist.dao.PlaylistDAO;
import io.awesome.gagtube.database.playlist.dao.PlaylistRemoteDAO;
import io.awesome.gagtube.database.playlist.dao.PlaylistStreamDAO;
import io.awesome.gagtube.database.playlist.model.PlaylistEntity;
import io.awesome.gagtube.database.playlist.model.PlaylistRemoteEntity;
import io.awesome.gagtube.database.playlist.model.PlaylistStreamEntity;
import io.awesome.gagtube.database.subscription.SubscriptionDAO;
import io.awesome.gagtube.database.subscription.SubscriptionEntity;

@TypeConverters({Converters.class})
@Database(
        entities = {
                SubscriptionEntity.class, SearchHistoryEntry.class,
                StreamEntity.class, StreamHistoryEntity.class, StreamStateEntity.class,
                PlaylistEntity.class, PlaylistStreamEntity.class, PlaylistRemoteEntity.class
        },
        version = 1,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    public static final String DATABASE_NAME = "gagtube.db";

    public abstract SubscriptionDAO subscriptionDAO();

    public abstract SearchHistoryDAO searchHistoryDAO();

    public abstract StreamDAO streamDAO();

    public abstract StreamHistoryDAO streamHistoryDAO();

    public abstract StreamStateDAO streamStateDAO();

    public abstract PlaylistDAO playlistDAO();

    public abstract PlaylistStreamDAO playlistStreamDAO();

    public abstract PlaylistRemoteDAO playlistRemoteDAO();
}
