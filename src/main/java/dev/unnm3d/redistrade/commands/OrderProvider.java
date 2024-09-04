package dev.unnm3d.redistrade.commands;

import com.jonahseguin.drink.argument.CommandArg;
import com.jonahseguin.drink.exception.CommandExitMessage;
import com.jonahseguin.drink.parametric.DrinkProvider;
import dev.unnm3d.redistrade.RedisTrade;
import dev.unnm3d.redistrade.objects.Order;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class OrderProvider extends DrinkProvider<Order> {
    public OrderProvider(RedisTrade redisTrade) {
    }

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
    public Order provide(@NotNull CommandArg arg, @NotNull List<? extends Annotation> annotations) throws CommandExitMessage {
        //String to Order
        return null;
    }

    @Override
    public String argumentDescription() {
        return "";
    }

    @Override
    public CompletableFuture<List<String>> getSuggestionsAsync(@NotNull String prefix) {
        //Return a list of order strings
        return null;
    }
}
