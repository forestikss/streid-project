package zov.polyak.module.list.movement;

import com.google.common.eventbus.Subscribe;
import zov.polyak.Polyak;
import zov.polyak.event.list.EventNoSlow;
import zov.polyak.module.Module;
import zov.polyak.module.ModuleCategory;
import zov.polyak.module.ModuleInformation;
import zov.polyak.module.list.combat.KillAura;
import zov.polyak.module.settings.ModeSetting;
import zov.polyak.util.player.simulate.SimulatedPlayer;

@ModuleInformation(moduleName = "No Slow", moduleCategory = ModuleCategory.MOVEMENT)
public class NoSlow extends Module {

    private final ModeSetting mode = new ModeSetting("Мод", "Vanilla", "Vanilla", "Grim");

    @Subscribe
    private void onNoSlow(EventNoSlow e) {
        switch (mode.getValue()) {
            case "Vanilla" -> {
                if (!(Polyak.getInstance().getModuleStorage().get(KillAura.class).getTarget() != null && SimulatedPlayer.simulateLocalPlayer(1).fallDistance > 0)) mc.player.setSprinting(true);
                e.cancelEvent();
            }
            case "Grim" -> {
                mc.player.setSprinting(mc.player.getItemUseTime() > 4 && !(Polyak.getInstance().getModuleStorage().get(KillAura.class).getTarget() != null && SimulatedPlayer.simulateLocalPlayer(1).fallDistance > 0) && Polyak.getInstance().getServerManager().getSprintingChangeTicks() > 0);
                if (mc.player.getItemUseTime() % 2 == 0) e.cancelEvent();
            }
        }
    }
}