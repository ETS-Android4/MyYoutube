package io.awesome.gagtube.download.helper;

import androidx.annotation.NonNull;

public class FinishedMission extends Mission {

    public FinishedMission() {
    }

    public FinishedMission(@NonNull DownloadMission mission) {
        source = mission.source;
        length = mission.length;
        timestamp = mission.timestamp;
        kind = mission.kind;
        storage = mission.storage;
    }

}
