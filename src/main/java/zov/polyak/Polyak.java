package zov.polyak;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import lombok.Getter;
import net.fabricmc.api.ModInitializer;
import net.minecraft.client.MinecraftClient;
import zov.polyak.event.list.EventKeyInput;
import zov.polyak.module.Module;
import zov.polyak.module.ModuleStorage;
import zov.polyak.util.commands.CommandDispatcher;
import zov.polyak.util.commands.manager.CommandRepository;
import zov.polyak.util.config.ConfigManager;
import zov.polyak.util.draggable.DragManager;
import zov.polyak.util.friend.FriendRepository;
import zov.polyak.util.macro.MacroRepository;
import zov.polyak.util.math.TPSGetter;
import zov.polyak.util.player.combat.IdealHitUtils;
import zov.polyak.util.player.other.ServerManager;
import zov.polyak.util.rotation.ComponentManager;
import zov.polyak.util.script.ScriptManager;
import zov.polyak.util.staff.StaffManager;

import java.io.File;

public class Polyak implements ModInitializer {

    private static Polyak instance;

    @Getter
    private final EventBus eventBus;

    @Getter
    private final ModuleStorage moduleStorage;
    @Getter
    private final ComponentManager componentManager;
    @Getter
    private final DragManager dragManager;
    @Getter
    private final CommandRepository commandRepository;
    @Getter
    private final MacroRepository macroRepository;
    @Getter
    private final ConfigManager configManager;
    @Getter
    private final CommandDispatcher commandDispatcher;
    @Getter
    private final StaffManager staffManager;
    @Getter
    private final ServerManager serverManager;
    @Getter
    private final TPSGetter tpsGetter;
    @Getter
    private final IdealHitUtils idealHitUtils;
    @Getter
    private final ScriptManager scriptManager;

    public Polyak() {
        instance = this;

        eventBus = new EventBus();
        eventBus.register(this);

        moduleStorage = new ModuleStorage();
        componentManager = new ComponentManager();
        dragManager = new DragManager();
        macroRepository = new MacroRepository();
        configManager = new ConfigManager();
        staffManager = new StaffManager();
        staffManager.load();
        commandRepository = new CommandRepository();
        commandDispatcher = new CommandDispatcher();
        serverManager = new ServerManager();
        tpsGetter = new TPSGetter();
        idealHitUtils = new IdealHitUtils();
        scriptManager = new ScriptManager();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            ConfigManager.save("autocfg");
            getDragManager().saveDraggables();
            getMacroRepository().save();
            FriendRepository.save();
            staffManager.save();
        }));
        File dir = new File("polyakzov/configs/");
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    public static Polyak getInstance() {
        return instance == null ? new Polyak() : instance;
    }

    @Override
    public void onInitialize() {
        getModuleStorage().injectRegisterModules();
        componentManager.init();
        dragManager.load();
        macroRepository.load();
        FriendRepository.load();
        configManager.load("autocfg");
    }

    @Subscribe
    private void onModuleKeyPressed(EventKeyInput event) {
        for (Module module : getModuleStorage().getModules()) {
            if (event.getAction() == 1 && MinecraftClient.getInstance().currentScreen == null) {
                if (module.getKey() == event.getKey()) {
                    module.toggle();
                }
            }
        }
    }
}