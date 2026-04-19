package zov.polyak.util.commands.api.exception;

import zov.polyak.util.QuickLogger;
import zov.polyak.util.commands.api.ICommand;
import zov.polyak.util.commands.api.argument.ICommandArgument;

import java.util.List;

public class CommandNotFoundException extends CommandException implements QuickLogger {

    public final String command;

    public CommandNotFoundException(String command) {
        super(String.format("Команда не найдена: %s", command));
        this.command = command;
    }

    @Override
    public void handle(ICommand command, List<ICommandArgument> args) {
       logDirect(getMessage());
    }
}
