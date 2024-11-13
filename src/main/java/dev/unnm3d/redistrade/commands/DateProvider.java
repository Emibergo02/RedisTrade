package dev.unnm3d.redistrade.commands;

import com.jonahseguin.drink.argument.CommandArg;
import com.jonahseguin.drink.exception.CommandExitMessage;
import com.jonahseguin.drink.parametric.CommandParameter;
import com.jonahseguin.drink.parametric.DrinkProvider;
import org.bukkit.command.CommandSender;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.annotation.Annotation;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class DateProvider extends DrinkProvider<Date> {
    public final DateFormat format;
    public final TimeZone timeZone;
    public final String formatString;

    public DateProvider(String dateFormat, String timeZone) {
        this.format = new SimpleDateFormat(dateFormat);
        this.timeZone = TimeZone.getTimeZone(timeZone);
        this.format.setTimeZone(this.timeZone);
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
    public Date provide(@Nonnull CommandArg arg, @Nonnull List<? extends Annotation> annotations) throws CommandExitMessage {
        String s = arg.get();

        try {
            return format.parse(s);
        } catch (ParseException e) {
            throw new CommandExitMessage("Date must be in format: " + formatString);
        }
    }

    @Override
    public String argumentDescription() {
        return "date: " + formatString;
    }

    @Override
    public List<String> getSuggestions(CommandSender sender, @Nonnull String prefix, Map<CommandParameter, String> parameters, List<Annotation> annotations) {
        if(annotations.stream().anyMatch(a -> a instanceof StartDate)) {
            //Day before
            return Collections.singletonList(format.format(new Date(System.currentTimeMillis() - 86400000)));
        }
        return Collections.singletonList(format.format(new Date()));
    }
}