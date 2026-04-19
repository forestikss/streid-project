package zov.polyak.event.list;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.entity.player.PlayerEntity;
import zov.polyak.event.Event;

@Getter
@AllArgsConstructor
public class EventPopTotem extends Event {
    private final PlayerEntity player;
}