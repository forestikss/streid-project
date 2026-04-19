package zov.polyak.module.list.movement;

import com.google.common.eventbus.Subscribe;
import zov.polyak.event.list.EventTick;
import zov.polyak.module.Module;
import zov.polyak.module.ModuleCategory;
import zov.polyak.module.ModuleInformation;

@ModuleInformation(moduleName = "No Jump Delay", moduleCategory = ModuleCategory.MOVEMENT)
public class NoJumpDelay extends Module {

    @Subscribe
    private void onUpdate(EventTick e) {
        if (mc.player == null || mc.world == null) return;

        mc.player.jumpingCooldown = 0;
    }
}