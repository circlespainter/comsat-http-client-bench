import com.pinterest.jbender.events.TimingEvent;
import com.pinterest.jbender.events.recording.Recorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

final class ProgressLogger<Res> implements Recorder<Res> {
  private static final Logger log = LoggerFactory.getLogger(ProgressLogger.class);

  private final long total;

  private long succeeded = 0;
  private long errored = 0;

  private long notified = 0;
  private long notifiedRoundedPercent = 0;
  private long notifiedNanos = System.nanoTime();

  public ProgressLogger(final int total) {
    this.total = total;
    log.info("Starting progress report");
  }

  @Override
  public void record(final TimingEvent<Res> timingEvent) {
    final long end = System.nanoTime();

    if (timingEvent.isSuccess)
      succeeded++;
    else
      errored++;

    final long finished = succeeded + errored;
    final long finishedRoundedPercent = (long) Math.floor(((double) (succeeded + errored)) / ((double) total) * 100.0D);

    if (finishedRoundedPercent > notifiedRoundedPercent) {
      // Notify progress
      final long newFinished = finished - notified;
      final long newTimeNanos = end - notifiedNanos;
      final double rps = newFinished / (newTimeNanos / 1_000_000_000.0D);
      notified = finished;
      notifiedRoundedPercent = finishedRoundedPercent;
      notifiedNanos = end;

      final double succeededPercent = ((double) (succeeded)) / ((double) total) * 100.0D;
      final double erroredPercent = ((double) (errored)) / ((double) total) * 100.0D;

      log.info((succeeded + errored) + "/" + finishedRoundedPercent + "% (" + succeeded + "/" + succeededPercent + "% OK + " + errored + "/" + erroredPercent + "% KO) / " + total + " (" + newFinished + " reqs in " + newTimeNanos + " nanos = " + rps + " rps)");
    }
  }
}
