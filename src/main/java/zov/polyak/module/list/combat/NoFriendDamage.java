package zov.polyak.module.list.combat;

import com.google.common.eventbus.Subscribe;
import zov.polyak.event.list.EventAttack;
import zov.polyak.module.Module;
import zov.polyak.module.ModuleCategory;
import zov.polyak.module.ModuleInformation;
import zov.polyak.util.friend.Friend;
import zov.polyak.util.friend.FriendRepository;

@ModuleInformation(moduleName = "No Friend Damage", moduleCategory = ModuleCategory.COMBAT)
public class NoFriendDamage extends Module {

    @Subscribe
    private void onAttack(EventAttack e) {
        for (Friend friend : FriendRepository.getFriends()) {
            if (e.getEntity() == mc.player) continue;
            if (!e.getEntity().getNameForScoreboard().equals(friend.name())) continue;
            e.cancelEvent();
        }
    }
}