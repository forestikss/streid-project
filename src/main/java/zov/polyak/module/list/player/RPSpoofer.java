package zov.polyak.module.list.player;

import com.google.common.eventbus.Subscribe;
import net.minecraft.network.packet.c2s.common.ResourcePackStatusC2SPacket;
import net.minecraft.network.packet.s2c.common.ResourcePackSendS2CPacket;
import zov.polyak.event.list.EventPacket;
import zov.polyak.module.Module;
import zov.polyak.module.ModuleCategory;
import zov.polyak.module.ModuleInformation;
import zov.polyak.util.packet.NetworkUtils;

@ModuleInformation(moduleName = "RP Spoofer", moduleCategory = ModuleCategory.MISC)
public class RPSpoofer extends Module {

    @Subscribe
    private void onPacket(EventPacket e) {
        if (e.getPacket() instanceof ResourcePackSendS2CPacket) {
            NetworkUtils.sendPacket(new ResourcePackStatusC2SPacket(mc.player.getUuid(), ResourcePackStatusC2SPacket.Status.ACCEPTED));
            NetworkUtils.sendPacket(new ResourcePackStatusC2SPacket(mc.player.getUuid(), ResourcePackStatusC2SPacket.Status.DOWNLOADED));
            NetworkUtils.sendPacket(new ResourcePackStatusC2SPacket(mc.player.getUuid(), ResourcePackStatusC2SPacket.Status.SUCCESSFULLY_LOADED));
            e.cancelEvent();
        }
    }
}