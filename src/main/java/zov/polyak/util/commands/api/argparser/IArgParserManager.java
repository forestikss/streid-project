

package zov.polyak.util.commands.api.argparser;

import zov.polyak.util.commands.api.argument.ICommandArgument;
import zov.polyak.util.commands.api.exception.CommandInvalidTypeException;
import zov.polyak.util.commands.api.registry.Registry;

public interface  IArgParserManager {

    
    <T> IArgParser.Stateless<T> getParserStateless(Class<T> type);

    
    <T, S> IArgParser.Stated<T, S> getParserStated(Class<T> type, Class<S> stateKlass);

    
    <T> T parseStateless(Class<T> type, ICommandArgument arg) throws CommandInvalidTypeException;

    
    <T, S> T parseStated(Class<T> type, Class<S> stateKlass, ICommandArgument arg, S state) throws CommandInvalidTypeException;

    Registry<IArgParser> getRegistry();
}
