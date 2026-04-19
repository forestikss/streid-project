package zov.polyak.util.commands.api;

import zov.polyak.util.commands.api.argparser.IArgParserManager;

public interface ICommandSystem {
    IArgParserManager getParserManager();
}
