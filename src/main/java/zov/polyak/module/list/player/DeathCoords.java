package zov.polyak.module.list.player;

import com.google.common.eventbus.Subscribe;
import zov.polyak.event.list.EventTick;
import zov.polyak.module.Module;
import zov.polyak.module.ModuleCategory;
import zov.polyak.module.ModuleInformation;

@ModuleInformation(moduleName = "Death Coords", moduleCategory = ModuleCategory.PLAYER)
public class DeathCoords extends Module {

    private boolean send;

    @Subscribe
    private void onUpdate(EventTick e) {
        if (mc.player == null) return;
        if (mc.player.isDead()) {
            if (!send) {
                logDirect(String.format("Вы умерли на XYZ: %.0f %.0f %.0f", mc.player.getX(), mc.player.getY(), mc.player.getZ()).replace(",","."));
                send = true;
            }
        } else send = false;
    }
}