package io.awesome.gultube.fragments.list;

import io.awesome.gultube.fragments.ViewContract;

public interface ListViewContract<I, N> extends ViewContract<I> {
    void showListFooter(boolean show);

    void handleNextItems(N result);
}
