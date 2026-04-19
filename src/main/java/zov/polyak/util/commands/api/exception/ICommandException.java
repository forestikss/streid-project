package zov.polyak.util.commands.api.exception;

import net.minecraft.util.Formatting;
import zov.polyak.util.QuickLogger;
import zov.polyak.util.commands.api.ICommand;
import zov.polyak.util.commands.api.argument.ICommandArgument;

import java.util.List;

public interface ICommandException extends QuickLogger {

    String getMessage();

    default void handle(ICommand command, List<ICommandArgument> args) {
        logDirect(
                this.getMessage(),
                Formatting.RED
        );
    }
}
