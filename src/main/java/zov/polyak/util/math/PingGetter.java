package zov.polyak.util.math;

import com.google.common.eventbus.Subscribe;
import lombok.Getter;
import net.minecraft.network.packet.s2c.common.CommonPingS2CPacket;
import zov.polyak.Polyak;
import zov.polyak.event.list.EventPacket;
import zov.polyak.event.list.EventTick;
import zov.polyak.util.IMinecraft;

@Getter
public class PingGetter implements IMinecraft {
    public PingGetter() {
        Polyak.getInstance().getEventBus().register(this);
    }

    private final StopWatch stopWatch = new StopWatch();
    private boolean lagged;
    private int ping;

    @Subscribe
    private void onUpdate(EventTick e) {
        ping = (int) stopWatch.getTime();
        if (stopWatch.getTime() > 1000) lagged = true;
    }

    @Subscribe
    private void onPacket(EventPacket e) {
        if (e.getPacket() instanceof CommonPingS2CPacket) {
            stopWatch.reset();
        }
    }
}