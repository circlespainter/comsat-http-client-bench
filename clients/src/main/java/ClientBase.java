import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.channels.Channel;
import co.paralleluniverse.strands.channels.Channels;
import com.pinterest.jbender.JBender;
import com.pinterest.jbender.events.TimingEvent;
import com.pinterest.jbender.events.recording.HdrHistogramRecorder;
import com.pinterest.jbender.events.recording.LoggingRecorder;
import com.pinterest.jbender.intervals.ExponentialIntervalGenerator;
import com.pinterest.jbender.intervals.IntervalGenerator;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import org.HdrHistogram.Histogram;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static java.util.Arrays.*;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import static com.pinterest.jbender.events.recording.Recorder.record;

public class ClientBase<Req, Res, Exec extends AutoCloseableRequestExecutor<Req, Res>> {
  private final Env<Req, Exec> env;

  public ClientBase(Env<Req, Exec> env) {
    this.env = env;
  }

  final public void run(final String[] args) throws SuspendExecution, InterruptedException, ExecutionException, IOException {
    final OptionParser parser = new OptionParser();
    final OptionSpec<Integer> r = parser.acceptsAll(asList("r", "rate")).withRequiredArg().ofType(Integer.class).describedAs("The throughput, in requests per second").defaultsTo(100);
    final OptionSpec<Integer> w = parser.acceptsAll(asList("w", "warmup")).withOptionalArg().ofType(Integer.class).describedAs("The number of requests used to warm up the load tester").defaultsTo(0);
    final OptionSpec<Integer> c = parser.acceptsAll(asList("c", "count")).withRequiredArg().ofType(Integer.class).describedAs("Requests count").defaultsTo(1000);
    final OptionSpec<String> u = parser.acceptsAll(asList("u", "URI")).withRequiredArg().ofType(String.class).describedAs("URI").defaultsTo("http://localhost:9000");
    final OptionSpec<Long> x = parser.acceptsAll(asList("x", "hdrHistHighest")).withRequiredArg().ofType(Long.class).describedAs("HDR Histogram highest trackable value").defaultsTo(3600000000L);
    final OptionSpec<Integer> d = parser.acceptsAll(asList("d", "hdrHistDigits")).withRequiredArg().ofType(Integer.class).describedAs("HDR Histogram number of significant value digits").defaultsTo(3);
    final OptionSpec<Double> s = parser.acceptsAll(asList("s", "hdrHistScalingRation")).withRequiredArg().ofType(Double.class).describedAs("HDR Histogram output value unit scaling ratio").defaultsTo(1000.0d);
    final OptionSpec<Integer> i = parser.acceptsAll(asList("i", "ioParallelism")).withRequiredArg().ofType(Integer.class).describedAs("Number of OS threads performing actual I/O").defaultsTo(Runtime.getRuntime().availableProcessors());
    final OptionSpec<Integer> m = parser.acceptsAll(asList("m", "maxConnections")).withRequiredArg().ofType(Integer.class).describedAs("Maximum number of concurrent connections").defaultsTo(Integer.MAX_VALUE);
    final OptionSpec<Integer> t = parser.acceptsAll(asList("t", "timeout")).withRequiredArg().ofType(Integer.class).describedAs("Connection timeout (ms)").defaultsTo(Integer.MAX_VALUE);
    parser.acceptsAll(asList("g", "preGenerateRequests"));
    parser.acceptsAll(asList("l", "logging"));

    parser.acceptsAll(asList("h", "?", "help"), "Show help").forHelp();

    final OptionSet options = parser.parse(args);

    if (options.has("h")) {
      parser.printHelpOn(System.out);
    } else {
      final IntervalGenerator intervalGen = new ExponentialIntervalGenerator(options.valueOf(r));
      try (final Exec requestExecutor =
          env.newRequestExecutor(options.valueOf(i), options.valueOf(m), options.valueOf(t))) {

        final int reqs = options.valueOf(c);
        final Channel<Req> requestCh = Channels.newChannel(reqs);
        final Channel<TimingEvent<Res>> eventCh = Channels.newChannel(reqs);

        final String uri = options.valueOf(u);
        // Requests generator
        final Fiber<Void> reqGen = new Fiber<Void>("req-gen", () -> {
          for (int j = 0; j < reqs; ++j) {
            final Req req = env.newRequest(uri);
            requestCh.send(req);
          }

          requestCh.close();
        }).start();

        if (options.has("g")) {
          reqGen.join();
        }

        final Histogram histogram = new Histogram(options.valueOf(x), options.valueOf(d));

        // Event recording, both HistHDR and logging
        if (options.has("l")) {
          record(eventCh, new HdrHistogramRecorder(histogram, 1), new LoggingRecorder(LOG));
        } else {
          record(eventCh, new HdrHistogramRecorder(histogram, 1));
        }

        // Main
        new Fiber<Void>("jbender", () -> {
          JBender.loadTestThroughput(intervalGen, options.valueOf(w), requestCh, requestExecutor, eventCh);
        }).start().join();

        histogram.outputPercentileDistribution(System.out, options.valueOf(s));
      } catch (Exception e) {
        LOG.error("Got exception: " + e.getMessage());
      }
    }
  }

  private static final Logger LOG = LoggerFactory.getLogger(ClientBase.class);
}
