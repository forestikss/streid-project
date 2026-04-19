package zov.polyak.util.commands;

import zov.polyak.util.commands.api.ICommandSystem;
import zov.polyak.util.commands.api.argparser.IArgParserManager;
import zov.polyak.util.commands.argparser.ArgParserManager;

public enum CommandSystem implements ICommandSystem {
    INSTANCE;

    @Override
    public IArgParserManager getParserManager() {
        return ArgParserManager.INSTANCE;
    }
}
