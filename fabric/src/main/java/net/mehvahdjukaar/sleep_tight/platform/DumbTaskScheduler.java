package net.mehvahdjukaar.sleep_tight.platform;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.mehvahdjukaar.sleep_tight.SleepTight;
import net.minecraft.server.TickTask;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class DumbTaskScheduler {

    public static void init(){
        //because mc tick task is also dumb
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            int now = server.getTickCount();
            while (!INBOUND.isEmpty()) {
                SCHEDULED.add(INBOUND.poll());
            }

            while (!SCHEDULED.isEmpty() && SCHEDULED.peek().getTick() <= now) {
                TickTask t = SCHEDULED.poll();
                try {
                    t.run();
                } catch (Exception e) {
                    SleepTight.LOGGER.error("Failed to run scheduled task", e);
                }
            }
        });
    }

    public static void schedule(TickTask task) {
        INBOUND.add(task);
    }

    private static final ConcurrentLinkedQueue<TickTask> INBOUND = new ConcurrentLinkedQueue<>();
    private static final PriorityQueue<TickTask> SCHEDULED =
            new PriorityQueue<>(Comparator.comparingInt(TickTask::getTick));

}
