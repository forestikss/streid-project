package zov.polyak.util.commands.defaults;

import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import zov.polyak.Polyak;
import zov.polyak.util.commands.api.Command;
import zov.polyak.util.commands.api.ICommand;
import zov.polyak.util.commands.api.argument.IArgConsumer;
import zov.polyak.util.commands.api.exception.CommandException;
import zov.polyak.util.commands.api.exception.CommandNotFoundException;
import zov.polyak.util.commands.api.helpers.Paginator;
import zov.polyak.util.commands.api.helpers.TabCompleteHelper;
import zov.polyak.util.commands.manager.CommandRepository;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static zov.polyak.util.commands.api.IBaritoneChatControl.FORCE_COMMAND_PREFIX;

public class HelpCommand extends Command {
    Polyak onetap;
    protected HelpCommand(Polyak onetap) {
        super("help");
        this.onetap = onetap;
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        args.requireMax(1);

        CommandRepository commandRepository = onetap.getCommandRepository();
        if (!args.hasAny() || args.is(Integer.class)) {
            Paginator.paginate(
                    args, new Paginator<>(
                            commandRepository.getRegistry().descendingStream()
                                    .filter(command -> !command.hiddenFromHelp())
                                    .collect(Collectors.toList())
                    ),
                    () -> logDirect("Доступные команды в чите:"),
                    command -> {
                        String names = String.join("/", command.getNames());
                        String name = command.getNames().get(0);
                        MutableText shortDescComponent = Text.literal(Formatting.DARK_GRAY + " - " + Formatting.GRAY + command.getShortDesc());
                        shortDescComponent.setStyle(shortDescComponent.getStyle().withColor(Formatting.GRAY));
                        MutableText namesComponent = Text.literal(names);
                        namesComponent.setStyle(namesComponent.getStyle().withColor(Formatting.WHITE));
                        MutableText hoverComponent = Text.literal("");
                        hoverComponent.setStyle(hoverComponent.getStyle().withColor(Formatting.GRAY));
                        hoverComponent.append(namesComponent);
                        hoverComponent.append("\n" + command.getShortDesc());
                        hoverComponent.append("\n\nНажмите, чтобы просмотреть полную справку о команде");
                        String clickCommand = FORCE_COMMAND_PREFIX + String.format("%s %s", label, command.getNames().get(0));
                        MutableText component = Text.literal(name);
                        component.setStyle(component.getStyle().withColor(Formatting.GRAY));
                        component.append(shortDescComponent);
                        component.setStyle(component.getStyle()
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverComponent))
                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, clickCommand)));
                        return component;
                    },
                    FORCE_COMMAND_PREFIX + label
            );
        } else {
            String commandName = args.getString().toLowerCase();
            ICommand command = commandRepository.getCommand(commandName);
            if (command == null) {
                throw new CommandNotFoundException(commandName);
            }
            logDirect("");
            command.getLongDesc().forEach(this::logDirect);
            logDirect("");
            MutableText returnComponent = Text.literal("Нажмите что бы вернуться обратно в меню");
            returnComponent.setStyle(returnComponent.getStyle().withClickEvent(new ClickEvent(
                    ClickEvent.Action.RUN_COMMAND,
                    FORCE_COMMAND_PREFIX + label
            )));
            logDirect(returnComponent);
        }
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
        if (args.hasExactlyOne()) {
            return new TabCompleteHelper()
                    .addCommands(Polyak.getInstance().getCommandRepository())
                    .filterPrefix(args.getString())
                    .stream();
        }
        return Stream.empty();
    }

    @Override
    public String getShortDesc() {
        return "Просмотр всех доступных команд";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "> help - Перечисляет все команды и их краткие описания.",
                "> help <command> - Отображение справочной информации по конкретной команде."
        );
    }
}