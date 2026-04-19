package zov.polyak.module.list.movement;

import com.google.common.eventbus.Subscribe;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
import zov.polyak.event.list.EventPlayerUpdate;
import zov.polyak.module.Module;
import zov.polyak.module.ModuleCategory;
import zov.polyak.module.ModuleInformation;
import zov.polyak.module.list.combat.KillAura;
import zov.polyak.util.base.Instance;
import zov.polyak.util.player.combat.PredictUtils;

@ModuleInformation(moduleName = "Elytra Motion", moduleCategory = ModuleCategory.MOVEMENT)
public class ElytraMotion extends Module {
    private boolean waitTarget;

    @Subscribe
    private void onPlayerTick(EventPlayerUpdate e) {
        if (mc.player == null) return;

        LivingEntity target = Instance.get(KillAura.class).getTarget();

        if (target == null) {
            if (!waitTarget) {
                mc.player.setNoGravity(false);
                waitTarget = true;
            }
            return;
        } else waitTarget = false;

        double dist = mc.player.getEyePos().distanceTo(target.getBoundingBox().getCenter());
        boolean shouldChase = target.isGliding() && target.getVelocity().length() * 20 >= 13;


        float pon;
        if(target.isGliding()) pon = 1.5f;
        else{
            pon = 3f;
        }
        if (mc.player.isGliding() && dist < pon && !shouldChase) {
            mc.player.setVelocity(0,0,0);
            mc.player.setNoGravity(true);
        } else {
            mc.player.setNoGravity(false);
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (mc.player == null) return;
        waitTarget = false;
        mc.player.setNoGravity(false);
    }
}