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
            final int now = server.getTickCount();

            for (TickTask t; (t = INBOUND.poll()) != null; ) {
                SCHEDULED.add(t);
            }

            while (!SCHEDULED.isEmpty() && SCHEDULED.peek().getTick() <= now) {
                TickTask t = SCHEDULED.poll();
                try {
                    t.run();
                } catch (Throwable ex) {
                    //dont let one task break the tick loop
                    SleepTight.LOGGER.error("TickTask failed", ex);
                }
            }
        });
    }

    public static void schedule(TickTask task) {
        INBOUND.add(task);
    }

    private static final ConcurrentLinkedQueue<TickTask> INBOUND = new java.util.concurrent.ConcurrentLinkedQueue<>();
    private static final PriorityQueue<TickTask> SCHEDULED =
            new PriorityQueue<>(Comparator.comparingInt(TickTask::getTick));

}
