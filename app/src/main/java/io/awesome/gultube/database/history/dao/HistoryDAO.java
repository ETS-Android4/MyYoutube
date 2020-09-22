package io.awesome.gultube.database.history.dao;

import io.awesome.gultube.database.BasicDAO;

public interface HistoryDAO<T> extends BasicDAO<T> {
    T getLatestEntry();
}
