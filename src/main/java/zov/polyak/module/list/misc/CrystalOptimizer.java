package zov.polyak.module.list.misc;

import com.google.common.eventbus.Subscribe;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import zov.polyak.event.list.EventAttack;
import zov.polyak.module.Module;
import zov.polyak.module.ModuleCategory;
import zov.polyak.module.ModuleInformation;

@ModuleInformation(moduleName = "Crystal Optimizer", moduleCategory = ModuleCategory.MISC)
public class CrystalOptimizer extends Module {
    @Subscribe
    private void onAttack(EventAttack e) {
        if (e.getEntity() instanceof EndCrystalEntity entity) {
            entity.remove(Entity.RemovalReason.DISCARDED);
        }
    }
}