package zov.polyak.module.list.misc;

import zov.polyak.module.Module;
import zov.polyak.module.ModuleCategory;
import zov.polyak.module.ModuleInformation;
import zov.polyak.module.settings.BooleanSetting;
import zov.polyak.util.friend.Friend;
import zov.polyak.util.friend.FriendRepository;

@ModuleInformation(moduleName = "Streamer Mode", moduleCategory = ModuleCategory.MISC)
public class NameProtect extends Module {

    public final BooleanSetting hideFriends = new BooleanSetting("Скрыть друзей", false);

    public String getCustomName() {
        return isEnabled() ? "polyak" : mc.player.getNameForScoreboard();
    }

    public String getCustomName(String originalName) {
        if (!isEnabled() || mc.player == null) {
            return originalName;
        }

        String me = mc.player.getNameForScoreboard();
        if (originalName.contains(me)) {
            return originalName.replace(me, "polyak");
        }

        if (hideFriends.getValue()) {
            var friends = FriendRepository.getFriends();
            for (Friend friend : friends) {
                if (originalName.contains(friend.name())) {
                    return originalName.replace(friend.name(), "polyak");
                }
            }
        }

        return originalName;
    }
}