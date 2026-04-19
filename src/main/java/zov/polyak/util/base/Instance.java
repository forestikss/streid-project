package zov.polyak.util.base;

import lombok.experimental.UtilityClass;
import zov.polyak.Polyak;
import zov.polyak.module.Module;
import zov.polyak.module.ModuleCategory;
import zov.polyak.util.rotation.Component;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

@UtilityClass
public class Instance {
    private final ConcurrentMap<Class<? extends Module>, Module> instances = new ConcurrentHashMap<>();
    private final ConcurrentMap<Class<? extends Component>, Component> componentInstances = new ConcurrentHashMap<>();

    public <T extends Module> T get(Class<T> clazz) {
        return clazz.cast(instances.computeIfAbsent(clazz, instance -> Polyak.getInstance().getModuleStorage().get(instance)));
    }

    public <T extends Component> T getComponent(Class<T> clazz) {
        return clazz.cast(componentInstances.computeIfAbsent(clazz, instance -> Polyak.getInstance().getComponentManager().get(instance)));
    }

    public <T extends Module> Supplier<T> getSupplier(Class<T> clazz) {
        return () -> clazz.cast(instances.computeIfAbsent(clazz, instance -> Polyak.getInstance().getModuleStorage().get(instance)));
    }

    public <T extends Module> T get(final String module) {
        return Polyak.getInstance().getModuleStorage().get(module);
    }

    public List<Module> get(final ModuleCategory category) {
        return Polyak.getInstance().getModuleStorage().get(category);
    }
}
