import com.pinterest.jbender.events.TimingEvent;
import com.pinterest.jbender.events.recording.Recorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicReference;

final class ProgressLogger<Res, Exec extends AutoCloseableRequestExecutor<?, Res>> implements Recorder<Res> {
  private static final Logger log = LoggerFactory.getLogger(ProgressLogger.class);

  private long succeeded = 0;
  private long errored = 0;

  private long notified = 0;
  private long notifiedNanos = System.nanoTime();

  private AtomicReference<Utils.SysStats> stats = new AtomicReference<>(null);

  public ProgressLogger(Exec requestExecutor, int total, int cmsi, int cmpi) {
    log.info("Starting progress report");

    final Timer ts = new Timer(true);
    final Timer tp = new Timer(true);

    ts.schedule(
      new TimerTask() {
        @Override
        public void run() {
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
          final long end = System.nanoTime();

          final long finished = succeeded + errored;
          final long finishedRoundedPercent = (long) Math.floor(((double) (succeeded + errored)) / ((double) total) * 100.0D);

          // Notify progress
          final long newFinished = finished - notified;
          final long newTimeNanos = end - notifiedNanos;
          final double rps = Math.round(newFinished / (newTimeNanos / 1_000_000_000.0D) * 100.D) / 100.D;
          notified = finished;
          notifiedNanos = end;

          final double succeededPercent = Math.round(((double) (succeeded)) / ((double) total) * 100.0D * 100.D) / 100.D;
          final double erroredPercent = Math.round(((double) (errored)) / ((double) total) * 100.0D * 100.D) / 100.D;

          log.info((succeeded + errored) + "/" + finishedRoundedPercent + "% (" + succeeded + "/" + succeededPercent + "% OK + " + errored + "/" + erroredPercent + "% KO) / " + total + " (" + newFinished + " reqs in " + newTimeNanos + " nanos = " + rps + " rps, concurrency = " + requestExecutor.getCurrentConcurrency() + ", max = " + requestExecutor.getMaxConcurrency() + ")");

          final Utils.SysStats s = stats.get();
          log.info("MEM = " + s.mem + " MB (max = " + s.maxMem +  " MB, avg = " + s.avgMem + " MB), CPU = " + s.cpu + " (max = " + s.maxCpu + ", avg = " + s.avgCpu + ")\n");
        }
      },
      cmpi,
      cmpi
    );
  }

  @Override
  public synchronized void record(final TimingEvent<Res> timingEvent) {
    if (timingEvent.isSuccess)
      succeeded++;
    else
      errored++;
  }
}
