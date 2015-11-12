import co.paralleluniverse.fibers.DefaultFiberScheduler;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.StrandFactory;
import co.paralleluniverse.strands.channels.Channel;
import co.paralleluniverse.strands.channels.Channels;
import com.pinterest.jbender.JBender;
import com.pinterest.jbender.events.TimingEvent;
import com.pinterest.jbender.events.recording.HdrHistogramRecorder;
import com.pinterest.jbender.events.recording.LoggingRecorder;
import com.pinterest.jbender.intervals.ConstantIntervalGenerator;
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
import java.util.concurrent.*;

import static com.pinterest.jbender.events.recording.Recorder.record;

public abstract class ClientBase<Req, Res, Exec extends AutoCloseableRequestExecutor<Req, Res>, E extends Env<Req, Exec>> {
  private static final String H = "h";
  private static final String L = "l";
  private static final String P = "p";

  private static final ExecutorService e = Executors.newCachedThreadPool();
  protected static final StrandFactory DEFAULT_FIBERS_SF = DefaultFiberScheduler.getInstance();
  protected static final StrandFactory CACHED_THREAD_SF = target -> new ExecutorServiceStrand(e, target);

  private E env;

  protected abstract E setupEnv(OptionSet options);

  final public void run(final String[] args, final StrandFactory sf) throws SuspendExecution, InterruptedException, ExecutionException, IOException {
    final OptionParser parser = new OptionParser();
    final OptionSpec<Integer> r = parser.acceptsAll(asList("r", "rate")).withOptionalArg().ofType(Integer.class).describedAs("The desired throughput, in requests per second");
    final OptionSpec<Integer> v = parser.acceptsAll(asList("v", "interval")).withOptionalArg().ofType(Integer.class).describedAs("The interval between requests, in milliseconds");
    final OptionSpec<Integer> n = parser.acceptsAll(asList("n", "maxConcurrency")).withOptionalArg().ofType(Integer.class).describedAs("Maximum concurrency level").defaultsTo(100);
    final OptionSpec<Integer> w = parser.acceptsAll(asList("w", "warmup")).withOptionalArg().ofType(Integer.class).describedAs("The number of requests used to warm up the load tester").defaultsTo(1_000);
    final OptionSpec<Integer> c = parser.acceptsAll(asList("c", "count")).withRequiredArg().ofType(Integer.class).describedAs("Requests count").defaultsTo(11_000);
    parser.acceptsAll(asList(P, "preGenerateRequests"));

    final OptionSpec<String> u = parser.acceptsAll(asList("u", "URI")).withRequiredArg().ofType(String.class).describedAs("URI").defaultsTo("http://localhost:9000");

    final OptionSpec<Long> x = parser.acceptsAll(asList("x", "hdrHistHighest")).withRequiredArg().ofType(Long.class).describedAs("HDR Histogram highest trackable value").defaultsTo(60_000L * 1_000_000_000L);
    final OptionSpec<Integer> d = parser.acceptsAll(asList("d", "hdrHistDigits")).withRequiredArg().ofType(Integer.class).describedAs("HDR Histogram number of significant value digits").defaultsTo(3);
    final OptionSpec<Double> s = parser.acceptsAll(asList("s", "hdrHistScalingRatio")).withRequiredArg().ofType(Double.class).describedAs("HDR Histogram output value unit scaling ratio").defaultsTo(1_000_000.0d);

    final OptionSpec<Integer> i = parser.acceptsAll(asList("i", "ioParallelism")).withRequiredArg().ofType(Integer.class).describedAs("Number of OS threads performing actual I/O").defaultsTo(Runtime.getRuntime().availableProcessors());
    final OptionSpec<Integer> m = parser.acceptsAll(asList("m", "maxConnections")).withRequiredArg().ofType(Integer.class).describedAs("Maximum number of concurrent connections").defaultsTo(Integer.MAX_VALUE);
    final OptionSpec<Integer> t = parser.acceptsAll(asList("t", "timeout")).withRequiredArg().ofType(Integer.class).describedAs("Connection timeout (ms)").defaultsTo(60_000);
    parser.acceptsAll(asList(L, "logging"));

    parser.acceptsAll(asList(H, "?", "help"), "Show help").forHelp();

    final OptionSet options = parser.parse(args);

    int status = 0;

    if (!options.has(v) && !options.has(r) && !options.has(n)) {
      status = -1;
      System.out.println("Commandline error: cne of '-v', '-r' or '-n' must be present");
    }

    if (status != 0 || options.has(H)) {
      parser.printHelpOn(System.err);
      System.exit(status);
    }

    System.err.println (
      "\n=/=> JBender settings:\n" +
        "\t* URL (-u): GET " + options.valueOf(u) + "\n" +
        "\t" +
          (options.has(v) ?
            (options.has(r) ?
              "* Desired throughput for exponential interval generator (rps, -r): " + options.valueOf(r) :
              "* Constant interval between requests (ms, -v): " + options.valueOf(v)
            ) :
            "* Maximum concurrency level (-n): " + options.valueOf(n)) +
        "\n" +
        "\t* IO Parallelism (async only, -i): " + options.valueOf(i) + "\n" +
        "\t* Requests count (-c): " + options.valueOf(c) + "\n" +
        "\t\t- Warmup requests (-w): " + options.valueOf(w) + "\n" +
        "\t* Request timeout (-t): " + options.valueOf(t) + " ms\n" +
        "\t* HDR histogram settings:\n" +
        "\t\t- Maximum (-x): " + options.valueOf(x) + "\n" +
        "\t\t- Digits (-d): " + options.valueOf(d) + "\n" +
        "\t\t- Scaling ratio (-s): " + options.valueOf(s) + "\n" +
        "\t* Pre-generate (-p): " + options.has("p") + "\n" +
        "\t* Logging (-l): " + options.has("l") + "\n"
    );

    env = setupEnv(options);
    try (final Exec requestExecutor =
        env.newRequestExecutor(options.valueOf(i), options.valueOf(m), options.valueOf(t))) {

      final int reqs = options.valueOf(c);
      final int warms = options.valueOf(w);
      final int recordedReqs = reqs - warms;
      final Channel<Req> requestCh = Channels.newChannel(reqs);
      final Channel<TimingEvent<Res>> eventCh = Channels.newChannel(reqs);

      final String uri = options.valueOf(u);
      // Requests generator
      final Fiber<Void> reqGen = new Fiber<Void>("req-gen", () -> {
        for (int j = 0; j < reqs; ++j) {
          final Req req;
          try {
            req = env.newRequest(uri);
            requestCh.send(req);
          } catch (final Exception e) {
            LOG.error("Got exception when constructing request: " + e.getMessage());            }
        }

        requestCh.close();
      }).start();

      if (options.has("p")) {
        reqGen.join();
      }

      final Histogram histogram = new Histogram(options.valueOf(x), options.valueOf(d));

      // Event recording, both HistHDR and logging
      if (options.has("l")) {
        record(eventCh, new HdrHistogramRecorder(histogram, 1), new LoggingRecorder(LOG), new ProgressLogger(recordedReqs));
      } else {
        record(eventCh, new HdrHistogramRecorder(histogram, 1), new ProgressLogger(recordedReqs));
      }

      // Main
      new Fiber<Void>("jbender", () -> {
        if (options.has(n)) {
          JBender.loadTestConcurrency(options.valueOf(n), warms, requestCh, requestExecutor, eventCh, sf);
        } else {
          IntervalGenerator intervalGen = null;

          if (options.has(r))
            intervalGen = new ExponentialIntervalGenerator(options.valueOf(r));
          else if (options.has(v))
            intervalGen = new ConstantIntervalGenerator(options.valueOf(v));

          JBender.loadTestThroughput(intervalGen, warms, requestCh, requestExecutor, eventCh, sf);
        }
      }).start().join();

      e.shutdown();

      System.err.println();
      histogram.outputPercentileDistribution(System.err, options.valueOf(s));
    } catch (final Exception e) {
      LOG.error("Got exception: " + e.getMessage());
    }
  }

  private static final Logger LOG = LoggerFactory.getLogger(ClientBase.class);
}
