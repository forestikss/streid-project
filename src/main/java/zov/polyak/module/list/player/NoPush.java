package zov.polyak.module.list.player;

import zov.polyak.module.Module;
import zov.polyak.module.ModuleCategory;
import zov.polyak.module.ModuleInformation;
import zov.polyak.module.settings.BooleanSetting;
import zov.polyak.module.settings.ModeListSetting;

@ModuleInformation(moduleName = "No Push", moduleCategory = ModuleCategory.PLAYER)
public class NoPush extends Module {
    public final ModeListSetting objects = new ModeListSetting("Обьекты",
            new BooleanSetting("Игроки", true),
            new BooleanSetting("Блоки", true)
    );
}