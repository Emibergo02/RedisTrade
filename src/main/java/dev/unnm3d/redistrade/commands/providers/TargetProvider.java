package dev.unnm3d.redistrade.commands.providers;

import com.jonahseguin.drink.argument.CommandArg;
import com.jonahseguin.drink.exception.CommandExitMessage;
import com.jonahseguin.drink.parametric.DrinkProvider;
import dev.unnm3d.redistrade.commands.PlayerListManager;
import lombok.RequiredArgsConstructor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.util.List;

@RequiredArgsConstructor
public class TargetProvider extends DrinkProvider<PlayerListManager.Target> {

    private final PlayerListManager playerListManager;

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
    public PlayerListManager.Target provide(@NotNull CommandArg arg, @NotNull List<? extends Annotation> annotations) throws CommandExitMessage {
        return new PlayerListManager.Target(arg.get());
    }

    @Override
    public String argumentDescription() {
        return "playerName";
    }


    @Override
    public List<String> getSuggestions(CommandSender sender, @NotNull String prefix) {
        return playerListManager.getPlayerList(null)
          .stream()
          .filter(s -> s.toLowerCase().startsWith(prefix.toLowerCase()) && !sender.getName().equalsIgnoreCase(s))
          .toList();
    }

}