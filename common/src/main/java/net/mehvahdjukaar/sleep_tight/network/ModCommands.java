package net.mehvahdjukaar.sleep_tight.network;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.mehvahdjukaar.moonlight.api.platform.RegHelper;
import net.mehvahdjukaar.sleep_tight.SleepTight;
import net.mehvahdjukaar.sleep_tight.SleepTightPlatformStuff;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class ModCommands {
    public static void init() {
        RegHelper.addCommandRegistration(ModCommands::register);
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal(SleepTight.MOD_ID).requires((p) -> p.hasPermission(2))
                        .then(Commands.literal("sleep_cooldown")
                                .then(GetInsomnia.register(dispatcher))
                                .then(SetInsomnia.register(dispatcher))
                        )
                        .then(Commands.literal("consecutive_nights")
                                .then(SetNights.register(dispatcher))
                                .then(GetNights.register(dispatcher))
                        )
                        .then(Commands.literal("home_bed_nights")
                                .then(GetHomeBedNights.register(dispatcher))
                                .then(SetHomeBedNights.register(dispatcher))
                        )
                        .then(Commands.literal("nightmare_chance")
                                .then(GetNightmareChance.register(dispatcher))

                        )
        );
    }

    private static class SetInsomnia implements Command<CommandSourceStack> {

        public static ArgumentBuilder<CommandSourceStack, ?> register(CommandDispatcher<CommandSourceStack> dispatcher) {
            return Commands.literal("set")
                    .then(Commands.argument("cooldown", IntegerArgumentType.integer(0))
                            .executes(new SetInsomnia()));
        }

        @Override
        public int run(CommandContext<CommandSourceStack> context) {
            if (context.getSource().getEntity() instanceof ServerPlayer serverPlayer) {
                var cap = SleepTightPlatformStuff.getPlayerSleepData(serverPlayer);
                int cooldown = IntegerArgumentType.getInteger(context, "cooldown");
                cap.addInsomnia(serverPlayer, cooldown);
                cap.syncToClient(serverPlayer);

                context.getSource().sendSuccess(Component.translatable("message.sleep_tight.command.set_insomnia"), false);
                return cooldown;
            }
            return 0;
        }
    }

    private static class GetInsomnia implements Command<CommandSourceStack> {

        public static ArgumentBuilder<CommandSourceStack, ?> register(CommandDispatcher<CommandSourceStack> dispatcher) {
            return Commands.literal("get").executes(new GetInsomnia());
        }

        @Override
        public int run(CommandContext<CommandSourceStack> context) {
            if (context.getSource().getEntity() instanceof ServerPlayer serverPlayer) {
                var cap = SleepTightPlatformStuff.getPlayerSleepData(serverPlayer);
                int timeLeft = (int) cap.getInsomniaTimeLeft(serverPlayer);
                context.getSource().sendSuccess(Component.translatable("message.sleep_tight.command.get_insomnia", timeLeft), false);
                return timeLeft;
            }
            return 0;
        }
    }

    private static class SetNights implements Command<CommandSourceStack> {

        public static ArgumentBuilder<CommandSourceStack, ?> register(CommandDispatcher<CommandSourceStack> dispatcher) {
            return Commands.literal("set")
                    .then(Commands.argument("consecutive_nights", IntegerArgumentType.integer(0))
                            .executes(new SetNights()));
        }

        @Override
        public int run(CommandContext<CommandSourceStack> context) {
            if (context.getSource().getEntity() instanceof ServerPlayer serverPlayer) {
                var cap = SleepTightPlatformStuff.getPlayerSleepData(serverPlayer);
                int nights = IntegerArgumentType.getInteger(context, "consecutive_nights");
                cap.setConsecutiveNightsSlept(nights);
                cap.syncToClient(serverPlayer);

                context.getSource().sendSuccess(Component.translatable("message.sleep_tight.command.set_nights"), false);
                return nights;
            }
            return 0;
        }
    }

    private static class GetNights implements Command<CommandSourceStack> {

        public static ArgumentBuilder<CommandSourceStack, ?> register(CommandDispatcher<CommandSourceStack> dispatcher) {
            return Commands.literal("get").executes(new GetNights());
        }

        @Override
        public int run(CommandContext<CommandSourceStack> context) {
            if (context.getSource().getEntity() instanceof ServerPlayer serverPlayer) {
                var cap = SleepTightPlatformStuff.getPlayerSleepData(serverPlayer);

                int timeLeft = cap.getConsecutiveNightsSlept();
                context.getSource().sendSuccess(Component.translatable("message.sleep_tight.command.get_nights", timeLeft), false);

                return timeLeft;
            }
            return 0;
        }
    }

    private static class SetHomeBedNights implements Command<CommandSourceStack> {

        public static ArgumentBuilder<CommandSourceStack, ?> register(CommandDispatcher<CommandSourceStack> dispatcher) {
            return Commands.literal("set")
                    .then(Commands.argument("nights", IntegerArgumentType.integer(0))
                            .executes(new SetHomeBedNights()));
        }

        @Override
        public int run(CommandContext<CommandSourceStack> context) {
            if (context.getSource().getEntity() instanceof ServerPlayer serverPlayer) {
                var cap = SleepTightPlatformStuff.getPlayerSleepData(serverPlayer);
                int nights = IntegerArgumentType.getInteger(context, "nights");
                cap.setNightsSleptInHomeBed(nights);
                cap.syncToClient(serverPlayer);

                context.getSource().sendSuccess(Component.translatable("message.sleep_tight.command.set_home_bed_nights"), false);
                return nights;
            }
            return 0;
        }
    }

    private static class GetHomeBedNights implements Command<CommandSourceStack> {

        public static ArgumentBuilder<CommandSourceStack, ?> register(CommandDispatcher<CommandSourceStack> dispatcher) {
            return Commands.literal("get").executes(new GetHomeBedNights());
        }

        @Override
        public int run(CommandContext<CommandSourceStack> context) {
            if (context.getSource().getEntity() instanceof ServerPlayer serverPlayer) {
                var cap = SleepTightPlatformStuff.getPlayerSleepData(serverPlayer);

                int timeLeft = cap.getNightsSleptInHomeBed();
                context.getSource().sendSuccess(Component.translatable("message.sleep_tight.command.get_home_bed_nights", timeLeft), false);

                return timeLeft;
            }
            return 0;
        }
    }

    private static class GetNightmareChance implements Command<CommandSourceStack> {

        public static ArgumentBuilder<CommandSourceStack, ?> register(CommandDispatcher<CommandSourceStack> dispatcher) {
            return Commands.literal("get").executes(new GetNightmareChance());
        }

        @Override
        public int run(CommandContext<CommandSourceStack> context) {
            if (context.getSource().getEntity() instanceof ServerPlayer serverPlayer) {
                var cap = SleepTightPlatformStuff.getPlayerSleepData(serverPlayer);

                double nightmareChance = cap.getNightmareChance(serverPlayer, serverPlayer.getOnPos());
                context.getSource().sendSuccess(Component.translatable("message.sleep_tight.command.nightmare_chance", String.format("%.3f", nightmareChance)), false);
            }
            return 0;
        }
    }
}
