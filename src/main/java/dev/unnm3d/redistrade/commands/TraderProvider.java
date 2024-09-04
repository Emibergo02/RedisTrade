package dev.unnm3d.redistrade.commands;

import com.jonahseguin.drink.argument.CommandArg;
import com.jonahseguin.drink.exception.CommandExitMessage;
import com.jonahseguin.drink.parametric.DrinkProvider;
import dev.unnm3d.redistrade.RedisTrade;
import dev.unnm3d.redistrade.objects.Trader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.util.List;

public class TraderProvider extends DrinkProvider<Trader> {
    public TraderProvider(RedisTrade redisTrade) {
    }

    @Override
    public boolean doesConsumeArgument() {
        return false;
    }

    @Override
    public boolean isAsync() {
        return false;
    }

    @Override
    public @Nullable Trader provide(@NotNull CommandArg arg, @NotNull List<? extends Annotation> annotations) throws CommandExitMessage {
        return null;
    }

    @Override
    public String argumentDescription() {
        return "";
    }
}
