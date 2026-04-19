package zov.polyak.event;

import lombok.Data;
import zov.polyak.Polyak;

@Data
public class Event {
    private boolean cancelled;

    public void post() {
        Polyak.getInstance().getEventBus().post(this);
    }

    public void cancelEvent() {
        setCancelled(true);
    }
}