package zov.polyak.module.list.combat;

import com.google.common.eventbus.Subscribe;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import zov.polyak.event.list.EventPlayerSync;
import zov.polyak.event.list.EventPlayerUpdate;
import zov.polyak.module.Module;
import zov.polyak.module.ModuleCategory;
import zov.polyak.module.ModuleInformation;
import zov.polyak.module.settings.BooleanSetting;
import zov.polyak.module.settings.ModeListSetting;
import zov.polyak.util.math.StopWatch;

@ModuleInformation(moduleName = "Auto Potion", moduleDesc = "Автоматически кидает выбранные бафы", moduleCategory = ModuleCategory.COMBAT)
public class AutoPotion extends Module {

    private final ModeListSetting throwSettings = new ModeListSetting("Кидать",
            new BooleanSetting("Зелье силы", true),
            new BooleanSetting("Зелье скорости", true),
            new BooleanSetting("Зелье огнестойкости", true)
    );
    private final BooleanSetting autoDisable = new BooleanSetting("Выключать после использования", false);

    private final StopWatch timer = new StopWatch();
    private float savedPitch;
    private boolean throwing = false;

    @Subscribe
    public void onSync(EventPlayerSync e) {
        if (mc.player == null) return;
        if (!canThrow()) {
            throwing = false;
            return;
        }
        savedPitch = mc.player.getPitch();
        mc.player.setPitch(90.0f);
        throwing = true;
    }

    @Subscribe
    public void onUpdate(EventPlayerUpdate e) {
        if (mc.player == null || mc.world == null) return;
        if (!throwing) return;
        if (!timer.isReached(500)) return;

        for (PotionType type : PotionType.values()) {
            if (!type.isSettingEnabled()) continue;
            if (mc.player.hasStatusEffect(type.getEffect())) continue;

            int slot = findPotionSlot(type.getEffect());
            if (slot == -1) continue;

            throwPotion(slot);
            timer.reset();

            if (autoDisable.getValue()) {
                setEnabled(false);
            }
            return;
        }
    }

    private void throwPotion(int slot) {
        int previousSlot = mc.player.getInventory().selectedSlot;

        if (slot < 9) {
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(previousSlot));
        } else {
            mc.interactionManager.clickSlot(0, slot, previousSlot, SlotActionType.SWAP, mc.player);
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(previousSlot));
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            mc.interactionManager.clickSlot(0, slot, previousSlot, SlotActionType.SWAP, mc.player);
        }
    }

    private int findPotionSlot(RegistryEntry<StatusEffect> effect) {
        for (int i = 0; i < 45; i++) {
            var stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() != Items.SPLASH_POTION) continue;
            var contents = stack.get(DataComponentTypes.POTION_CONTENTS);
            if (contents == null) continue;
            for (var effectInstance : contents.getEffects()) {
                if (effectInstance.getEffectType().equals(effect)) return i;
            }
        }
        return -1;
    }

    private boolean canThrow() {
        if (mc.player == null || mc.world == null) return false;
        boolean onGround = mc.player.isOnGround() ||
                mc.world.getBlockState(BlockPos.ofFloored(mc.player.getX(), mc.player.getY() - 0.3, mc.player.getZ())).isSolid();
        if (!onGround) return false;
        if (mc.player.isClimbing()) return false;
        if (mc.player.hasVehicle()) return false;
        if (mc.player.getAbilities().flying) return false;
        if (mc.player.isTouchingWater() || mc.player.isInLava()) return false;

        for (PotionType type : PotionType.values()) {
            if (type.isSettingEnabled()
                    && !mc.player.hasStatusEffect(type.getEffect())
                    && findPotionSlot(type.getEffect()) != -1) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onDisable() {
        throwing = false;
        super.onDisable();
    }

    @Getter
    @RequiredArgsConstructor
    private enum PotionType {
        STRENGTH(StatusEffects.STRENGTH, "Зелье силы"),
        SPEED(StatusEffects.SPEED, "Зелье скорости"),
        FIRE_RESISTANCE(StatusEffects.FIRE_RESISTANCE, "Зелье огнестойкости");

        private final RegistryEntry<StatusEffect> effect;
        private final String settingName;

        public boolean isSettingEnabled() {
            return AutoPotion.getStaticInstance().throwSettings.isEnabled(settingName);
        }
    }

    private static AutoPotion staticInstance;

    public AutoPotion() {
        staticInstance = this;
    }

    public static AutoPotion getStaticInstance() {
        return staticInstance;
    }
}
