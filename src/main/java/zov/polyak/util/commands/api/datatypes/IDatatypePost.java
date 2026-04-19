package zov.polyak.util.commands.api.datatypes;

import zov.polyak.util.commands.api.exception.CommandException;

public interface IDatatypePost<T, O> extends IDatatype {
    T apply(IDatatypeContext datatypeContext, O original) throws CommandException;
}
