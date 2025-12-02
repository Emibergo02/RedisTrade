package dev.unnm3d.redistrade.commands.providers;

import com.jonahseguin.drink.argument.CommandArg;
import com.jonahseguin.drink.exception.CommandExitMessage;
import com.jonahseguin.drink.parametric.CommandParameter;
import com.jonahseguin.drink.parametric.DrinkProvider;
import dev.unnm3d.redistrade.commands.StartDate;
import org.bukkit.command.CommandSender;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.annotation.Annotation;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class LocalDateProvider extends DrinkProvider<LocalDateTime> {
    public final DateTimeFormatter format;
    public final ZoneId timeZone;
    public final String formatString;

    public LocalDateProvider(String dateFormat, String timeZone) {
        this.timeZone = ZoneId.of(timeZone);
        this.format = DateTimeFormatter.ofPattern(dateFormat)
                .withZone(this.timeZone);
        this.formatString = dateFormat;
    }

    @Override
    public boolean doesConsumeArgument() {
        return true;
    }

    @Override
    public boolean isAsync() {
        return false;
    }

    @Nullable
    @Override
    public LocalDateTime provide(@Nonnull CommandArg arg, @Nonnull List<? extends Annotation> annotations) throws CommandExitMessage {
        if (arg.get() == null) return null;
        try {
            return LocalDateTime.parse(arg.get(), format);
        } catch (DateTimeParseException e) {
            throw new CommandExitMessage("Invalid date format. Expected: " + formatString);
        }
    }

    @Override
    public String argumentDescription() {
        return "date: " + formatString;
    }

    @Override
    public List<String> getSuggestions(CommandSender sender, @Nonnull String prefix, Map<CommandParameter, String> parameters, List<Annotation> annotations) {
        final LocalDateTime ldt = LocalDateTime.now(timeZone);
        if (annotations.stream().anyMatch(a -> a instanceof StartDate)) {
            //Day before
            return Collections.singletonList(format.format(ldt.minusDays(1)));
        }
        return Collections.singletonList(format.format(ldt));
    }
}