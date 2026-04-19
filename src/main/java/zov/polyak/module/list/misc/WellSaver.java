package zov.polyak.module.list.misc;

import com.google.common.eventbus.Subscribe;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import zov.polyak.event.list.EventTick;
import zov.polyak.module.Module;
import zov.polyak.module.ModuleCategory;
import zov.polyak.module.ModuleInformation;
import zov.polyak.module.settings.Setting;
import zov.polyak.module.settings.SliderSetting;
import zov.polyak.util.base.Instance;
import zov.polyak.util.math.StopWatch;

import java.util.concurrent.ThreadLocalRandom;

@ModuleInformation(moduleName = "Well Saver", moduleDesc = "Быстрый сейв инвентаря через хаб", moduleCategory = ModuleCategory.MISC)
public class WellSaver extends Module {

    private final SliderSetting grief = new SliderSetting("Гриф", 1, 1, 3, 1);

    private final StopWatch tickTimer = new StopWatch();
    private final StopWatch stateTimer = new StopWatch();
    private State state;

    @Override
    public void onEnable() {
        super.onEnable();
        state = State.WAITING_FOR_JOIN;
        tickTimer.reset();
        stateTimer.reset();
        if (mc.player == null || mc.getNetworkHandler() == null) return;

        String[] messages = {
                "Лан бань, я с клиентом",
                "Ну ок, бань если хочешь",
                "Лан откинь, я с софтом",
                "Да, я с читом, делай что должен",
                "Ну че, оформляй бан"
        };

        mc.player.networkHandler.sendChatMessage(messages[ThreadLocalRandom.current().nextInt(messages.length)]);
        mc.player.networkHandler.sendChatCommand("hub");
    }

    @Subscribe
    private void onTick(EventTick e) {
        if (mc.player == null || mc.world == null || mc.getNetworkHandler() == null || mc.interactionManager == null) return;
        if (!tickTimer.every(100)) return;

        WellJoiner wellJoiner = Instance.get(WellJoiner.class);

        switch (state) {
            case WAITING_FOR_JOIN -> {
                if (wellJoiner != null && !wellJoiner.isEnabled()) {
                    applyWellJoinerGrief(wellJoiner, grief.getIntValue());
                    wellJoiner.setEnabled(true);
                }
                state = State.JOINING_GRIEF;
            }
            case JOINING_GRIEF -> {
                if (wellJoiner != null && !wellJoiner.isEnabled()) {
                    mc.getNetworkHandler().sendChatCommand("warp demaz");
                    state = State.WAITING_WARP;
                    tickTimer.reset();
                    stateTimer.reset();
                }
            }
            case WAITING_WARP -> {
                if (stateTimer.getTime() >= 250) {
                    dropInventory();
                    setEnabled(false);
                }
            }
        }
    }

    private void applyWellJoinerGrief(WellJoiner wellJoiner, int griefId) {
        for (Setting setting : wellJoiner.getSettings()) {
            if (setting instanceof SliderSetting slider && "Гриф".equals(slider.getName())) {
                slider.setValue(griefId);
                return;
            }
        }
    }

    private void dropInventory() {
        int syncId = mc.player.currentScreenHandler.syncId;
        for (int slotId = 9; slotId <= 45; slotId++) {
            Slot slot = mc.player.currentScreenHandler.getSlot(slotId);
            if (slot == null || !slot.hasStack()) continue;
            mc.interactionManager.clickSlot(syncId, slotId, 1, SlotActionType.THROW, mc.player);
        }
    }

    private enum State {
        WAITING_FOR_JOIN,
        JOINING_GRIEF,
        WAITING_WARP
    }
}
