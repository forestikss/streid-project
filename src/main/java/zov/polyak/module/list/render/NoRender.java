package zov.polyak.module.list.render;

import com.google.common.eventbus.Subscribe;
import zov.polyak.event.list.EventTick;
import zov.polyak.module.Module;
import zov.polyak.module.ModuleCategory;
import zov.polyak.module.ModuleInformation;
import zov.polyak.module.settings.BooleanSetting;
import zov.polyak.module.settings.ModeListSetting;

@ModuleInformation(moduleName = "Removals", moduleCategory = ModuleCategory.RENDER)
public class NoRender extends Module {

    public final ModeListSetting elements = new ModeListSetting("Убрать элементы",
            new BooleanSetting("Огонь",true),
            new BooleanSetting("Размытие в воде",true),
            new BooleanSetting("Зрение в блоках",true),
            new BooleanSetting("Камераклип",true),
            new BooleanSetting("Тряска камеры",true)
    );

    @Subscribe
    public void onUpdate(EventTick e) {
        if (elements.isEnabled("Тряска камеры")) {
            mc.options.getDamageTiltStrength().setValue(0.0);
        } else {
            mc.options.getDamageTiltStrength().setValue(0.5);
        }
    }

    @Override
    public void onDisable() {
        mc.options.getDamageTiltStrength().setValue(0.5);
        super.onDisable();
    }
}