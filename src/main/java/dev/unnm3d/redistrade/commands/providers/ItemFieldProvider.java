package dev.unnm3d.redistrade.commands.providers;

import com.jonahseguin.drink.argument.CommandArg;
import com.jonahseguin.drink.exception.CommandExitMessage;
import com.jonahseguin.drink.parametric.CommandParameter;
import com.jonahseguin.drink.parametric.DrinkProvider;
import dev.unnm3d.redistrade.configs.GuiSettings;
import org.bukkit.command.CommandSender;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ItemFieldProvider extends DrinkProvider<Field> {

    @Override
    public boolean doesConsumeArgument() {
        return true;
    }

    @Override
    public boolean isAsync() {
        return true;
    }

    @Nullable
    @Override
    public Field provide(@Nonnull CommandArg arg, @Nonnull List<? extends Annotation> annotations) throws CommandExitMessage {
        final Optional<Field> optionalField = Arrays.stream(GuiSettings.class.getDeclaredFields())
          .filter(f -> f.getName().equals(arg.get()))
          .findFirst();
        if (optionalField.isEmpty()) {
            throw new CommandExitMessage("Field not found");
        }
        return optionalField.get();
    }

    @Override
    public String argumentDescription() {
        return "Field inside GuiSettings";
    }

    @Override
    public List<String> getSuggestions(CommandSender sender, @Nonnull String prefix, Map<CommandParameter, String> parameters, List<Annotation> annotations) {
        return Arrays.stream(GuiSettings.class.getDeclaredFields())
          .filter(f -> f.getType().equals(GuiSettings.SimpleSerializableItem.class))
          .map(Field::getName)
          .filter(n -> n.startsWith(prefix))
          .toList();
    }
}