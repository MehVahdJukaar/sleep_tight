package net.mehvahdjukaar.sleep_tight.fabric;

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

            // Drain inbound (lock-free, safe for multi-producer)
            for (TickTask t; (t = INBOUND.poll()) != null; ) {
                SCHEDULED.add(t); // server thread only
            }

            // Run due tasks in tick order
            while (!SCHEDULED.isEmpty() && SCHEDULED.peek().getTick() <= now) {
                TickTask t = SCHEDULED.poll();
                try {
                    t.run();
                } catch (Throwable ex) {
                    // don't let one task break the tick loop
                    SleepTight.LOGGER.error("TickTask failed", ex);
                }
            }
        });
    }

    public static void schedule(TickTask task) {
        INBOUND.add(task);
    }

    // Tasks arriving from any thread
    private static final ConcurrentLinkedQueue<TickTask> INBOUND = new ConcurrentLinkedQueue<>();
    // Only the server thread touches this
    private static final PriorityQueue<TickTask> SCHEDULED =
            new PriorityQueue<>(Comparator.comparingInt(TickTask::getTick));

}
