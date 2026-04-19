package zov.polyak.module.list.player;

import com.google.common.eventbus.Subscribe;
import net.minecraft.item.Items;
import zov.polyak.event.list.EventPlayerUpdate;
import zov.polyak.module.Module;
import zov.polyak.module.ModuleCategory;
import zov.polyak.module.ModuleInformation;

@ModuleInformation(moduleName = "Fast Exp", moduleCategory = ModuleCategory.PLAYER)
public class FastExp extends Module {

    @Subscribe
    private void onPlayerUpdate(EventPlayerUpdate e) {
        if (!(mc.player.getMainHandStack().getItem() == Items.EXPERIENCE_BOTTLE || mc.player.getOffHandStack().getItem() == Items.EXPERIENCE_BOTTLE)) return;

        mc.itemUseCooldown = 0;
    }
}