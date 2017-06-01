package de.randombyte.commanddelayer

import com.google.inject.Inject
import de.randombyte.kosp.config.serializers.duration.SimpleDurationTypeSerializer
import de.randombyte.kosp.extensions.toText
import org.slf4j.Logger
import org.spongepowered.api.Sponge
import org.spongepowered.api.command.CommandException
import org.spongepowered.api.command.CommandResult
import org.spongepowered.api.command.args.GenericArguments
import org.spongepowered.api.command.spec.CommandSpec
import org.spongepowered.api.event.Listener
import org.spongepowered.api.event.game.state.GameInitializationEvent
import org.spongepowered.api.plugin.Plugin
import org.spongepowered.api.scheduler.Task
import java.util.concurrent.TimeUnit

@Plugin(id = CommandDelayer.ID, name = CommandDelayer.NAME, version = CommandDelayer.VERSION,
        authors = arrayOf(CommandDelayer.AUTHOR))
class CommandDelayer @Inject constructor(val logger: Logger) {

    companion object {
        const val ID = "commanddelayer"
        const val NAME = "CommandDelayer"
        const val VERSION = "1.0"
        const val AUTHOR = "RandomByte"

        const val DELAY_ARGUMENT = "delay"
        const val COMMAND_ARGUMENT = "command"
    }

    @Listener
    fun onInit(event: GameInitializationEvent) {
        Sponge.getCommandManager().register(this, CommandSpec.builder()
                .permission("commanddelayer")
                .arguments(
                        GenericArguments.string(DELAY_ARGUMENT.toText()),
                        GenericArguments.remainingJoinedStrings(COMMAND_ARGUMENT.toText()))
                .executor { src, ctx ->
                    val delayDurationString = ctx.getOne<String>(DELAY_ARGUMENT).get()
                    val command = ctx.getOne<String>(COMMAND_ARGUMENT).get()

                    val legacyFormatSeconds = delayDurationString.toIntOrNull()
                    val delayMilliseconds = if (legacyFormatSeconds != null) {
                        legacyFormatSeconds * 1000L
                    } else {
                        SimpleDurationTypeSerializer.deserialize(delayDurationString).toMillis()
                    }

                    if (delayMilliseconds < 1) throw CommandException(("'$DELAY_ARGUMENT' must be positive!").toText())

                    Task.builder()
                            .delay(delayMilliseconds, TimeUnit.MILLISECONDS)
                            .execute { -> Sponge.getCommandManager().process(src, command) }
                            .submit(this)

                    return@executor CommandResult.success()
                }.build(), "delay")
    }
}