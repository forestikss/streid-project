package zov.polyak.util.commands.api.datatypes;

import zov.polyak.util.IMinecraft;
import zov.polyak.util.commands.api.exception.CommandException;

import java.util.stream.Stream;

public interface IDatatype extends IMinecraft {
    Stream<String> tabComplete(IDatatypeContext ctx) throws CommandException;
}
