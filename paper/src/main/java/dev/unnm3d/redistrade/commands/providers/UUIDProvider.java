package dev.unnm3d.redistrade.commands.providers;


import com.jonahseguin.drink.argument.CommandArg;
import com.jonahseguin.drink.exception.CommandExitMessage;
import com.jonahseguin.drink.parametric.DrinkProvider;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.UUID;

public class UUIDProvider extends DrinkProvider<UUID> {

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
    public UUID provide(@Nonnull CommandArg arg, @Nonnull List<? extends Annotation> annotations) throws CommandExitMessage {

        try {
            return UUID.fromString(arg.get());
        } catch (IllegalArgumentException e) {
            throw new CommandExitMessage("Invalid UUID format. Expected format: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx");
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String argumentDescription() {
        return "Trade UUID";
    }
}
