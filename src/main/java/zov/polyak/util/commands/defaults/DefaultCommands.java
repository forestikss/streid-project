

package zov.polyak.util.commands.defaults;

import zov.polyak.Polyak;
import zov.polyak.util.commands.api.ICommand;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class DefaultCommands {

    public static List<ICommand> createAll() {
        List<ICommand> commands = new ArrayList<>(Arrays.asList(
                new CfgCommand(),
                new RotationCommand(),
                new HelpCommand(Polyak.getInstance()),
                new MacroCommand(Polyak.getInstance()),
                new BindCommand(Polyak.getInstance()),
                new FriendCommand(Polyak.getInstance()),
                new StaffCommand(Polyak.getInstance()),
                new VClipCommand(),
                new PartyCommand(),
                new GpsCommand(),
                new AICommand(),
                new BotCommand()
        ));
        return Collections.unmodifiableList(commands);
    }
}