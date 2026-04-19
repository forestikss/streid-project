package zov.polyak.event.list;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.util.math.BlockPos;
import zov.polyak.event.Event;

@Getter
@AllArgsConstructor
public class EventObsidianPlace extends Event {
    private final BlockPos blockPos;
}