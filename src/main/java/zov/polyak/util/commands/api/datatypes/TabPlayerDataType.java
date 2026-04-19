package zov.polyak.util.commands.api.datatypes;

import net.minecraft.scoreboard.Team;
import zov.polyak.util.commands.api.exception.CommandException;
import zov.polyak.util.commands.api.helpers.TabCompleteHelper;

import java.util.Collection;
import java.util.stream.Stream;

public enum TabPlayerDataType implements IDatatypeFor<Team> {
    INSTANCE;

    @Override
    public Stream<String> tabComplete(IDatatypeContext ctx) throws CommandException {
        return new TabCompleteHelper()
                .append(getTeam().stream()
                        .map(Team::getPlayerList)
                        .map(Object::toString)
                        .map(s -> s.replaceAll("[\\[\\]]", "")))
                .filterPrefix(ctx.getConsumer().getString())
                .sortAlphabetically()
                .stream();
    }

    @Override
    public Team get(IDatatypeContext datatypeContext) throws CommandException {
        final String username = datatypeContext.getConsumer().getString();
        return getTeam().stream()
                .filter(s -> s.getName().equalsIgnoreCase(username))
                .findFirst().orElse(null);
    }

    public Collection<Team> getTeam() {
        return mc.world.getScoreboard().getTeams();
    }
}
