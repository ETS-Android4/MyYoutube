package io.awesome.gagtube.player.playqueue.events;

public class ErrorEvent implements PlayQueueEvent {
    
    final private int errorIndex;
    final private int queueIndex;

    @Override
    public PlayQueueEventType type() {
        return PlayQueueEventType.ERROR;
    }

    public ErrorEvent(final int errorIndex, final int queueIndex) {
        
        this.errorIndex = errorIndex;
        this.queueIndex = queueIndex;
    }

    public int getErrorIndex() {
        return errorIndex;
    }

    public int getQueueIndex() {
        return queueIndex;
    }
}
