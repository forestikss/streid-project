package zov.polyak.event.list;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.screen.slot.Slot;
import zov.polyak.event.Event;

@Getter
@AllArgsConstructor
public class EventHandledScreen extends Event {
    private final Slot slotHover;
}
