package org.telegram.repostcleanerbot.tdlib.request;

import org.telegram.repostcleanerbot.tdlib.EventManager;

public abstract class EventProvider {

    protected EventManager eventManager;

    protected EventProvider(EventManager eventManager) {
        this.eventManager = eventManager;
    }
}
