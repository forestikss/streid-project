package zov.polyak.module.list.combat;

import com.google.common.eventbus.Subscribe;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import zov.polyak.event.list.EventPacket;
import zov.polyak.module.Module;
import zov.polyak.module.ModuleCategory;
import zov.polyak.module.ModuleInformation;

@ModuleInformation(moduleName = "Velocity", moduleCategory = ModuleCategory.COMBAT)
public class Velocity extends Module {
    @Subscribe
    private void onPacket(EventPacket e) {
        if (e.getPacket() instanceof EntityVelocityUpdateS2CPacket packet) {
            if (packet.getEntityId() != mc.player.getId()) return;

            e.cancelEvent();
        }
    }
}