package zov.polyak.event.list;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import zov.polyak.event.Event;

@Getter
@Setter
@AllArgsConstructor
public class EventChangeSprint extends Event {
    private boolean sprinting;
}