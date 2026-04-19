package zov.polyak.util.commands.api.exception;

import zov.polyak.util.QuickLogger;
import zov.polyak.util.commands.api.ICommand;
import zov.polyak.util.commands.api.argument.ICommandArgument;

import java.util.List;

public class CommandUnhandledException extends RuntimeException implements ICommandException, QuickLogger {

    public CommandUnhandledException(String message) {
        super(message);
    }

    public CommandUnhandledException(Throwable cause) {
        super(cause);
    }

    @Override
    public void handle(ICommand command, List<ICommandArgument> args) {
    }
}
