package zov.polyak.module.list.player;

import com.google.common.eventbus.Subscribe;
import zov.polyak.event.list.EventTick;
import zov.polyak.module.Module;
import zov.polyak.module.ModuleCategory;
import zov.polyak.module.ModuleInformation;
import zov.polyak.util.math.StopWatch;
import zov.polyak.util.player.other.SlownessManager;

@ModuleInformation(moduleName = "Teleport Back", moduleCategory = ModuleCategory.PLAYER)
public class TeleportBack extends Module {

    private final StopWatch stopWatch = new StopWatch();
    private boolean dead;

    @Subscribe
    private void onUpdate(EventTick e) {
        if (mc.player == null || mc.world == null) return;
        if (!mc.player.isAlive() && !dead && stopWatch.isReached(500)) {
            mc.player.networkHandler.sendChatCommand("sethome gavno");
            SlownessManager.addTimeTask(new SlownessManager.TimeTask(100, () -> {
                if (mc.player == null) return;
                mc.player.requestRespawn();
                mc.player.networkHandler.sendChatCommand("home gavno");
                dead = false;
            }, true));
            stopWatch.reset();
        }
    }
}