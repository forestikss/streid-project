package zov.polyak.event.list;

import lombok.AllArgsConstructor;
import lombok.Getter;
import zov.polyak.event.Event;

@Getter
@AllArgsConstructor
public class ChatEvent extends Event {
    private final String message;
}