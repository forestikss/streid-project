package zov.polyak.event.list;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import zov.polyak.event.Event;

@Getter
@AllArgsConstructor
public class EventHUD extends Event {
    private final DrawContext drawContext;
    private final RenderTickCounter renderTickCounter;
}