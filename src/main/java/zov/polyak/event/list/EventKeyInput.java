package zov.polyak.event.list;

import lombok.AllArgsConstructor;
import lombok.Getter;
import zov.polyak.event.Event;

@Getter
@AllArgsConstructor
public class EventKeyInput extends Event {
    private final int key, action;
}