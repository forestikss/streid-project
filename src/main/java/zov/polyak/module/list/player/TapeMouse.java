package zov.polyak.module.list.player;

import com.google.common.eventbus.Subscribe;
import zov.polyak.event.list.EventTick;
import zov.polyak.module.Module;
import zov.polyak.module.ModuleCategory;
import zov.polyak.module.ModuleInformation;
import zov.polyak.module.settings.BooleanSetting;
import zov.polyak.module.settings.SliderSetting;
import zov.polyak.util.bot.BotSessionManager;

@ModuleInformation(moduleName = "Tape Mouse", moduleDesc = "Автоматические клики для фоновых ботов", moduleCategory = ModuleCategory.PLAYER)
public class TapeMouse extends Module {
    private final SliderSetting cps = new SliderSetting("CPS", 1, 0, 2, 0.05f);
    private final BooleanSetting rightClick = new BooleanSetting("Правая кнопка", false);
    private final BooleanSetting onlyBots = new BooleanSetting("Только боты", true);

    private long lastClick;

    @Subscribe
    private void onTick(EventTick eventTick) {
        long delay = (long) (1000.0f / cps.getValue());
        long now = System.currentTimeMillis();

        if (now - lastClick < delay) {
            return;
        }

        if (!onlyBots.getValue()) {
            clickForLocalPlayer();
        }

        if (!BotSessionManager.getConnections().isEmpty()) {
            BotSessionManager.pulseBots(rightClick.getValue());
        }

        lastClick = now;
    }

    private void clickForLocalPlayer() {
        if (mc.player == null || mc.currentScreen != null) return;

        if (rightClick.getValue()) {
            mc.doItemUse();
        } else {
            mc.doAttack();
        }
    }
}