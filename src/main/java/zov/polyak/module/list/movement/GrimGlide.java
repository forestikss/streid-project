package zov.polyak.module.list.movement;

import com.google.common.eventbus.Subscribe;
import net.minecraft.util.math.Vec3d;
import zov.polyak.event.list.EventPlayerUpdate;
import zov.polyak.module.Module;
import zov.polyak.module.ModuleCategory;
import zov.polyak.module.ModuleInformation;
import zov.polyak.util.math.StopWatch;

@ModuleInformation(moduleName = "GrimGlide", moduleDesc = "ну типо в попе ковыряет и ты летишь", moduleCategory = ModuleCategory.MOVEMENT)
public class GrimGlide extends Module {

    private final StopWatch ticks = new StopWatch();

    @Subscribe
    private void onMotion(EventPlayerUpdate event) {
        if (mc.player == null || mc.world == null) return;
        if (!mc.player.isGliding()) return;

        Vec3d pos = mc.player.getPos();
        float yaw = mc.player.getYaw();
        double forward = 0.085;
        double dx = -Math.sin(Math.toRadians(yaw)) * forward;
        double dz = Math.cos(Math.toRadians(yaw)) * forward;

        mc.player.setVelocity(dx * 1.25, mc.player.getVelocity().y - 0.01, dz * 1.25);

        if (ticks.isReached(45)) {
            mc.player.setPos(
                    pos.getX() + dx,
                    pos.getY(),
                    pos.getZ() + dz
            );
            ticks.reset();
        }

        mc.player.setVelocity(dx * 1.25, mc.player.getVelocity().y + 0.015, dz * 1.25);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        ticks.reset();
    }
}
