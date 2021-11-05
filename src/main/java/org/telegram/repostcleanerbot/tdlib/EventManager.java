package org.telegram.repostcleanerbot.tdlib;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class EventManager {
    private Map<Enum, List<Consumer>> allEventsHandlers = new HashMap<>();

    public <E extends Enum<E>, T> void addEventHandler(E event, Consumer<T> handler) {
        List<Consumer> eventHandlers = allEventsHandlers.get(event);
        if(eventHandlers == null) {
            eventHandlers = new ArrayList<>();
            allEventsHandlers.put(event, eventHandlers);
        }
        eventHandlers.add(handler);
    }

    public <E extends Enum<E>, T> void fireEvent(E event, T eventData) {
        List<Consumer> eventHandlers = allEventsHandlers.get(event);
        if(eventHandlers != null) {
            eventHandlers.forEach(c -> {
                try {
                    c.accept(eventData);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }
}
