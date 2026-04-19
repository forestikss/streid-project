package zov.polyak.module.list.movement;

import com.google.common.eventbus.Subscribe;
import net.minecraft.entity.player.PlayerEntity;
import zov.polyak.Polyak;
import zov.polyak.event.list.EventTick;
import zov.polyak.module.Module;
import zov.polyak.module.ModuleCategory;
import zov.polyak.module.ModuleInformation;
import zov.polyak.module.list.player.FreeCamera;

@ModuleInformation(moduleName = "Sprint", moduleDesc = "Автоматический спринт", moduleCategory = ModuleCategory.MOVEMENT)
public class Sprint extends Module {
    @Subscribe
    public void onUpdate(EventTick event) {
        if (mc.player == null) return;
        mc.options.sprintKey.setPressed(false);

        PlayerEntity fakePlayer = Polyak.getInstance().getModuleStorage().get(FreeCamera.class).fakePlayer;

        mc.player.setSprinting(fakePlayer == null && ((!mc.player.isTouchingWater() || mc.player.isSubmergedInWater()) && !mc.player.isGliding() && mc.player.isWalking() && mc.player.canSprint() && !mc.player.isUsingItem() && !mc.player.isBlind() && (!mc.player.hasVehicle() || (mc.player.getVehicle().canSprintAsVehicle() && mc.player.getVehicle().isLogicalSideForUpdatingMovement()) && !mc.player.isGliding() && (!mc.player.shouldSlowDown() || mc.player.isSubmergedInWater())) && mc.player.input.hasForwardMovement() && (!mc.player.horizontalCollision && !mc.player.collidedSoftly)));
    }
}