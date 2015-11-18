import com.pinterest.jbender.events.TimingEvent;
import com.pinterest.jbender.events.recording.Recorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

final class ProgressLogger<Res, Exec extends AutoCloseableRequestExecutor<?, Res>> implements Recorder<Res> {
  private static final Logger log = LoggerFactory.getLogger(ProgressLogger.class);

  private AtomicLong succ = new AtomicLong(0);
  private AtomicLong err = new AtomicLong(0);

  private long notified = 0;
  private long notifiedNanos = System.nanoTime();

  private long avgDurationNanos = Long.MAX_VALUE;

  private AtomicReference<Utils.SysStats> stats = new AtomicReference<>(null);

  public ProgressLogger(Exec requestExecutor, int total, boolean sysMon, int cmsi, int cmpi) {
    log.info("Starting progress report");

    final Timer ts = new Timer(true);
    final Timer tp = new Timer(true);

    if (sysMon)
      ts.schedule (
        new TimerTask() {
          @Override
          public void run() {
            final long succeeded = succ.get();
            final long errored = err.get();

            if (succeeded + errored == total)
              ts.cancel();
            else
              stats.set(Utils.sampleSys());
          }
        },
        0L,
        cmsi
      );

    tp.schedule (
      new TimerTask() {
        @Override
        public void run() {
          final long succeeded = succ.get();
          final long errored = err.get();

          if (succeeded + errored == total) {
            tp.cancel();
          } else {
            final long end = System.nanoTime();

            final long finished = succeeded + errored;
            final long finishedRoundedPercent = (long) Math.floor(((double) (succeeded + errored)) / ((double) total) * 100.0D);

            // Notify progress
            final long newFinished = finished - notified;
            final long newTimeNanos = end - notifiedNanos;
            notified = finished;
            notifiedNanos = end;

            final double succeededPercent = Math.round(((double) (succeeded)) / ((double) total) * 100.0D * 100.D) / 100.D;
            final double erroredPercent = Math.round(((double) (errored)) / ((double) total) * 100.0D * 100.D) / 100.D;

            log.info((succeeded + errored) + "/" + finishedRoundedPercent + "% (" + succeeded + "/" + succeededPercent + "% OK + " + errored + "/" + erroredPercent + "% KO) / " + total + " (" + newFinished + " reqs in " + newTimeNanos + " nanos, " + (1.0D / (avgDurationNanos / 1_000_000_000.0D)) + " rps, concurrency = " + requestExecutor.getCurrentConcurrency() + ", max = " + requestExecutor.getMaxConcurrency() + ")");

            if (sysMon) {
              final Utils.SysStats s = stats.get();
              log.info("MEM = " + s.mem + " MB (max = " + s.maxMem + " MB, avg = " + s.avgMem + " MB), CPU = " + s.cpu + " (max = " + s.maxCpu + ", avg = " + s.avgCpu + ")\n");
            }
          }
        }
      },
      cmpi,
      cmpi
    );
  }

  @Override
  public void record(final TimingEvent<Res> timingEvent) {
    final long succeeded = succ.get();
    final long errored = err.get();

    avgDurationNanos = (avgDurationNanos * (succeeded + errored) + timingEvent.durationNanos) / (succeeded + errored + 1);

    if (timingEvent.isSuccess)
      succ.incrementAndGet();
    else
      err.incrementAndGet();
  }
}
