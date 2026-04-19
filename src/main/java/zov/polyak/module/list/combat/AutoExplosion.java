package zov.polyak.module.list.combat;

import com.google.common.eventbus.Subscribe;
import lombok.Getter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import zov.polyak.event.list.EventEntitySpawn;
import zov.polyak.event.list.EventObsidianPlace;
import zov.polyak.event.list.EventRightClickBlock;
import zov.polyak.event.list.EventTick;
import zov.polyak.module.Module;
import zov.polyak.module.ModuleCategory;
import zov.polyak.module.ModuleInformation;
import zov.polyak.module.settings.BooleanSetting;
import zov.polyak.util.friend.FriendRepository;
import zov.polyak.util.math.BestPoint;
import zov.polyak.util.math.RotationUtil;
import zov.polyak.util.player.other.InventoryUtil;
import zov.polyak.util.player.other.SlownessManager;
import zov.polyak.util.rotation.Rotation;
import zov.polyak.util.rotation.RotationComponent;

import java.util.HashSet;
import java.util.Set;

@ModuleInformation(moduleName = "Auto Explosion", moduleCategory = ModuleCategory.COMBAT)
public class AutoExplosion extends Module {

    private final BooleanSetting saveSelf = new BooleanSetting("Не бабах себя", false);
    private final BooleanSetting saveFriend = new BooleanSetting("Не бабах друзей", false);
    private final BooleanSetting saveResources = new BooleanSetting("Не бахать ресы", false);

    private BlockPos obsidianPos;
    private Entity entityToAttack;
    private int prevSlot = -1;
    @Getter private int ticksToDisableRightClicks;

    private final Set<BlockPos> myCrystalPlaces = new HashSet<>();

    @Subscribe
    private void onObsidianPlace(EventObsidianPlace e) {
        var crystalSlot = InventoryUtil.searchItemHotbar(Items.END_CRYSTAL);

        if (crystalSlot == -1) return;

        if (obsidianPos == null && condition(e.getBlockPos())) {
            obsidianPos = e.getBlockPos();
            ticksToDisableRightClicks = 5;
        }
    }

    @Subscribe
    private void onTick(EventTick e) {
        if (ticksToDisableRightClicks > 0) ticksToDisableRightClicks--;
        if (entityToAttack != null) {
            if (mc.player.getEyePos().distanceTo(BestPoint.getNearestPoint(entityToAttack)) < 3) {
                var rotation = new Rotation(RotationUtil.calculate(BestPoint.getNearestPoint(entityToAttack)));

                RotationComponent.update(rotation, 360, 360, 360, 360, 0, 55, false);

                SlownessManager.addTimeTask(new SlownessManager.TimeTask(50, () -> {
                    if (entityToAttack == null) return;
                    mc.interactionManager.attackEntity(mc.player, entityToAttack);
                    mc.player.swingHand(Hand.MAIN_HAND);
                    entityToAttack = null;
                }, true));
            } else {
                entityToAttack = null;
            }
        }

        if (obsidianPos == null) return;

        var rotation = new Rotation(Vec3d.ofCenter(obsidianPos));

        RotationComponent.update(rotation, 360, 360, 360, 360, 0, 55, false);

        var slot = InventoryUtil.searchItemHotbar(Items.END_CRYSTAL);

        SlownessManager.addTimeTask(new SlownessManager.TimeTask(5, () -> {
            if (slot != -1 && mc.crosshairTarget instanceof BlockHitResult hitResult) {
                if (prevSlot == -1) prevSlot = mc.player.getInventory().selectedSlot;
                mc.player.getInventory().selectedSlot = slot;

                BlockPos placePos = hitResult.getBlockPos();
                myCrystalPlaces.add(placePos);

                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hitResult);
                SlownessManager.addTimeTask(new SlownessManager.TimeTask(5, () -> {
                    if (prevSlot != -1) mc.player.getInventory().selectedSlot = prevSlot;
                    prevSlot = -1;
                }, true));
                obsidianPos = null;
            }
        }, true));
    }

    @Subscribe
    private void onRightClick(EventRightClickBlock e) {
        if (mc.player.getMainHandStack().getItem() != Items.END_CRYSTAL) return;

        BlockPos placePos = e.getHitResult().getBlockPos().up();
        myCrystalPlaces.add(placePos);
    }

    @Subscribe
    private void onEntitySpawn(EventEntitySpawn e) {
        if (!(e.getEntity() instanceof EndCrystalEntity crystal)) return;

        BlockPos crystalPos = crystal.getBlockPos();

        if (!myCrystalPlaces.contains(crystalPos)) return;

        entityToAttack = crystal;
        myCrystalPlaces.remove(crystalPos);
    }

    private boolean condition(BlockPos blockPos) {
        if (saveSelf.getValue() && mc.player.getY() > blockPos.getY()) return false;
        for (var entity : mc.world.getEntities()) {
            if (entity == mc.player) continue;
            if (saveResources.getValue() && entity instanceof ItemEntity item) {
                var dx = Math.abs(item.getX() - blockPos.getX());
                var dy = Math.abs(item.getY() - blockPos.getY());
                var dz = Math.abs(item.getZ() - blockPos.getZ());

                if (dx < 4 && dy < 2 && dz < 4) return false;
            }
            if (!(entity instanceof PlayerEntity player) || !FriendRepository.isFriend(player.getNameForScoreboard()))
                continue;
            if (saveFriend.getValue() && player.getY() > blockPos.getY()) return false;
        }
        return true;
    }

    @Override
    public void onDisable() {
        obsidianPos = null;
        entityToAttack = null;
        ticksToDisableRightClicks = 0;
        super.onDisable();
    }
}