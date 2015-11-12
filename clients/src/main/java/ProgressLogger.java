import com.pinterest.jbender.events.TimingEvent;
import com.pinterest.jbender.events.recording.Recorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class ProgressLogger<Res> implements Recorder<Res> {
  private static final Logger log = LoggerFactory.getLogger(ProgressLogger.class);

  private final long total;

  private long succeeded = 0;
  private long errored = 0;
  private long notifiedRoundedPercent = 0;

  public ProgressLogger(final int total) {
    this.total = total;
  }

  @Override
  public void record(final TimingEvent<Res> timingEvent) {
    if (timingEvent.isSuccess)
      succeeded++;
    else
      errored++;

    final long finishedRoundedPercent = (long) Math.floor(((double) (succeeded + errored)) / ((double) total) * 100.0);
    if (finishedRoundedPercent > notifiedRoundedPercent) {
      notifiedRoundedPercent = finishedRoundedPercent;
      final double succeededPercent = ((double) (succeeded)) / ((double) total) * 100.0;
      final double erroredPercent = ((double) (errored)) / ((double) total) * 100.0;

      log.info((succeeded + errored) + "/" + finishedRoundedPercent + "% (" + succeeded + "/" + succeededPercent + "% OK + " + errored + "/" + erroredPercent + "% KO) / " + total);
    }
  }
}
