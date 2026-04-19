package zov.polyak.module.list.movement;

import com.google.common.eventbus.Subscribe;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import zov.polyak.event.list.EventCloseInv;
import zov.polyak.event.list.EventPacket;
import zov.polyak.event.list.EventPlayerUpdate;
import zov.polyak.module.Module;
import zov.polyak.module.ModuleCategory;
import zov.polyak.module.ModuleInformation;
import zov.polyak.module.settings.BooleanSetting;
import zov.polyak.module.settings.SliderSetting;
import zov.polyak.util.player.move.MoveUtil;
import zov.polyak.util.packet.NetworkUtils;
import zov.polyak.util.player.other.SlownessManager;

import java.util.ArrayList;
import java.util.List;

@ModuleInformation(moduleName = "Gui Move", moduleDesc = "Позволяет взаимодействовать с инвентарем при движении", moduleCategory = ModuleCategory.MOVEMENT)
public class GuiMove extends Module {

    private final BooleanSetting universal = new BooleanSetting("Универсальный", false);
    private final SliderSetting slownessDuration = new SliderSetting("Длительность замедления", 50, 1, 400, 1).setVisible(universal::getValue);
    private final List<Packet<?>> packets = new ArrayList<>();
    private boolean wasSprinting = false;

    @Override
    public void onDisable() {
        super.onDisable();
        packets.clear();
        wasSprinting = false;
    }

    @Subscribe
    public void onPacket(EventPacket e) {
        if (!universal.getValue()) return;
        if (mc.currentScreen == null || mc.currentScreen instanceof ChatScreen) return;

        final Packet<?> packet = e.getPacket();

        if (packet instanceof ClickSlotC2SPacket
                && MoveUtil.hasPlayerMovement()
                && mc.currentScreen instanceof InventoryScreen) {
            packets.add(packet);
            e.cancelEvent();
        } else if (packet instanceof CloseHandledScreenC2SPacket
                && MoveUtil.hasPlayerMovement()
                && mc.player.isSprinting()) {
            wasSprinting = true;
            packets.add(packet);
            e.cancelEvent();
        }
    }

    @Subscribe
    public void onCloseInv(EventCloseInv e) {
        if (!universal.getValue()) return;
        if (mc.currentScreen == null || mc.currentScreen instanceof ChatScreen) return;

        if (!packets.isEmpty()) {
            e.cancelEvent();

            if (wasSprinting) {
                mc.player.setSprinting(false);
            }

            SlownessManager.addTask(new SlownessManager.SlowTask(slownessDuration.getIntValue(), 0, () -> {
                packets.forEach(NetworkUtils::sendSilentPacket);
                packets.clear();
                NetworkUtils.sendSilentPacket(new CloseHandledScreenC2SPacket(mc.player.currentScreenHandler.syncId));

                if (wasSprinting) {
                    Sprint sprint = zov.polyak.util.base.Instance.get(Sprint.class);
                    if (sprint != null && sprint.isEnabled()) {
                        mc.player.setSprinting(true);
                    }
                    wasSprinting = false;
                }
            }));
        }
    }

    @Subscribe
    public void onUpdate(EventPlayerUpdate e) {
        if (mc.player == null) return;
        if (mc.currentScreen == null || mc.currentScreen instanceof ChatScreen) return;

        for (KeyBinding key : new KeyBinding[]{
                mc.options.forwardKey, mc.options.backKey,
                mc.options.leftKey, mc.options.rightKey,
                mc.options.jumpKey
        }) {
            key.setPressed(InputUtil.isKeyPressed(
                    mc.getWindow().getHandle(),
                    InputUtil.fromTranslationKey(key.getBoundKeyTranslationKey()).getCode()
            ));
        }
    }
}
