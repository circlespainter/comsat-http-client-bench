package co.paralleluniverse.comsat.bench.http.client;

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
import com.pinterest.jbender.executors.Validator;
import com.pinterest.jbender.intervals.ConstantIntervalGenerator;
import com.pinterest.jbender.intervals.ExponentialIntervalGenerator;
import com.pinterest.jbender.intervals.IntervalGenerator;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import org.HdrHistogram.Histogram;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.glassfish.jersey.apache.connector.ApacheClientProperties;
import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.HttpUrlConnectorProvider;
import org.glassfish.jersey.client.spi.ConnectorProvider;
import org.glassfish.jersey.grizzly.connector.GrizzlyConnectorProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Arrays.*;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.concurrent.*;

import static com.pinterest.jbender.events.recording.Recorder.record;

public abstract class ClientBase<Req, Res, Exec extends AutoCloseableRequestExecutor<Req, Res>, E extends Env<Req, Exec>> {
  private static final ExecutorService e = Executors.newCachedThreadPool();
  public static final StrandFactory DEFAULT_FIBERS_SF = DefaultFiberScheduler.getInstance();
  public static final StrandFactory CACHED_THREAD_SF = target -> new ExecutorServiceStrand(e, target);

  private static final String H = "h";
  private static final String L = "l";
  private static final String P = "p";
  private static final String CMSY = "cmsy";
  private static final String SMSY = "smsy";

  private E env;

  protected abstract E setupEnv(OptionSet options);

  final public void run(final String[] args, final StrandFactory sf) throws SuspendExecution, InterruptedException, ExecutionException, IOException {
    final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    if (logger instanceof ch.qos.logback.classic.Logger) {
      ch.qos.logback.classic.Logger logbackLogger = (ch.qos.logback.classic.Logger) logger;
      logbackLogger.setLevel(ch.qos.logback.classic.Level.INFO);
    }

    final OptionParser parser = new OptionParser();
    final OptionSpec<Integer> r = parser.acceptsAll(asList("r", "rate")).withOptionalArg().ofType(Integer.class).describedAs("The desired throughput, in requests per second");
    final OptionSpec<Integer> v = parser.acceptsAll(asList("v", "interval")).withOptionalArg().ofType(Integer.class).describedAs("The interval between requests, in nanoseconds");
    final OptionSpec<Integer> n = parser.acceptsAll(asList("n", "maxConcurrency")).withOptionalArg().ofType(Integer.class).describedAs("Maximum concurrency level");
    final OptionSpec<Integer> w = parser.acceptsAll(asList("w", "warmup")).withOptionalArg().ofType(Integer.class).describedAs("The number of requests used to warm up the load tester").defaultsTo(1_000);
    final OptionSpec<Integer> c = parser.acceptsAll(asList("c", "count")).withRequiredArg().ofType(Integer.class).describedAs("Requests count").defaultsTo(11_000);
    parser.acceptsAll(asList(P, "preGenerateRequests"));

    final OptionSpec<String> u = parser.acceptsAll(asList("u", "url")).withRequiredArg().ofType(String.class).describedAs("URI").defaultsTo("http://localhost:9000");
    final OptionSpec<String> z = parser.acceptsAll(asList("z", "monitorURL")).withRequiredArg().ofType(String.class).describedAs("Monitor control base URI").defaultsTo("http://localhost:9000/monitor");

    final OptionSpec<Long> x = parser.acceptsAll(asList("x", "hdrHistHighest")).withRequiredArg().ofType(Long.class).describedAs("HDR Histogram highest trackable value").defaultsTo(3_600_000L * 1_000_000_000L);
    final OptionSpec<Integer> d = parser.acceptsAll(asList("d", "hdrHistDigits")).withRequiredArg().ofType(Integer.class).describedAs("HDR Histogram number of significant value digits").defaultsTo(3);
    final OptionSpec<Double> s = parser.acceptsAll(asList("s", "hdrHistScalingRatio")).withRequiredArg().ofType(Double.class).describedAs("HDR Histogram output value unit scaling ratio").defaultsTo(1_000_000.0d);

    final OptionSpec<Integer> i = parser.acceptsAll(asList("i", "ioParallelism")).withRequiredArg().ofType(Integer.class).describedAs("Number of OS threads performing actual I/O").defaultsTo(Runtime.getRuntime().availableProcessors());
    final OptionSpec<Integer> m = parser.acceptsAll(asList("m", "maxConnections")).withRequiredArg().ofType(Integer.class).describedAs("Maximum number of concurrent connections").defaultsTo(Integer.MAX_VALUE);
    final OptionSpec<Integer> t = parser.acceptsAll(asList("t", "timeout")).withRequiredArg().ofType(Integer.class).describedAs("Connection timeout (ms)").defaultsTo(3_600_000);

    parser.accepts(CMSY);
    final OptionSpec<Integer> cmsi = parser.accepts("cmsi").withRequiredArg().ofType(Integer.class).describedAs("Client monitoring sample interval (ms)").defaultsTo(100);
    final OptionSpec<Integer> cmpi = parser.accepts("cmpi").withRequiredArg().ofType(Integer.class).describedAs("Client monitoring print interval (ms)").defaultsTo(100);
    parser.accepts(SMSY);
    final OptionSpec<Integer> smsi = parser.accepts("smsi").withRequiredArg().ofType(Integer.class).describedAs("Server monitoring sample interval (ms)").defaultsTo(100);
    final OptionSpec<Integer> smpi = parser.accepts("smpi").withRequiredArg().ofType(Integer.class).describedAs("Server monitoring print interval (ms)").defaultsTo(100);

    final OptionSpec<Integer> rbs = parser.accepts("rbs").withRequiredArg().ofType(Integer.class).describedAs("Buffer size for generated requests").defaultsTo(-1);
    final OptionSpec<Integer> ebs = parser.accepts("ebs").withRequiredArg().ofType(Integer.class).describedAs("Buffer size for request completion events").defaultsTo(-1);

    parser.acceptsAll(asList(L, "logging"));

    parser.acceptsAll(asList(H, "?", "help"), "Show help").forHelp();

    final OptionSet options = parser.parse(args);

    int status = 0;

    if (!options.has(v) && !options.has(r) && !options.has(n)) {
      status = -1;
      System.out.println("ERROR: one of '-v', '-r', '-n' must be provided.\n");
    }

    if (status != 0 || options.has(H)) {
      parser.printHelpOn(System.err);
      System.exit(status);
    }

    final int reqs = options.valueOf(c);
    final int rbsV = options.valueOf(rbs) < 0 ? reqs : options.valueOf(rbs);
    final int ebsV = options.valueOf(ebs) < 0 ? reqs : options.valueOf(ebs);

    System.err.println (
      "\n=============== JBENDER SETTINGS ==============\n" +
        "\t* URL (-u): GET " + options.valueOf(u) + "\n" +
        "\t* Monitor control base URL (-z): " + options.valueOf(z) + "\n" +
        "\t" +
          (!options.has(v) ?
            (!options.has(r) ?
              "* Maximum concurrency level (-n): " + options.valueOf(n) :
              "* Desired throughput for exponential interval generator (rps, -r): " + options.valueOf(r)
            ) :
            "* Constant interval between requests (ns, -v): " + options.valueOf(v)
          ) +
        "\n" +
        "\t* Maximum open connections (-m): " + options.valueOf(m) + "\n" +
        "\t* IO Parallelism (async only, -i): " + options.valueOf(i) + "\n" +
        "\t* Request timeout (-t): " + options.valueOf(t) + " ms\n" +
        "\t* Requests count (-c): " + options.valueOf(c) + "\n" +
        "\t\t- Warmup requests (-w): " + options.valueOf(w) + "\n" +
        "\t* HDR histogram settings:\n" +
        "\t\t- Maximum (-x): " + options.valueOf(x) + "\n" +
        "\t\t- Digits (-d): " + options.valueOf(d) + "\n" +
        "\t\t- Scaling ratio (-s): " + options.valueOf(s) + "\n" +
        "\t* Monitoring settings (-1 = N/A):\n" +
        "\t\t- Client system monitoring (-cmsy): " + options.has(CMSY) + "\n" +
        "\t\t- Client sample interval (-cmsi): " + options.valueOf(cmsi) + " ms\n" +
        "\t\t- Client print interval (-cmpi): " + options.valueOf(cmpi) + " ms\n" +
        "\t\t- Server system monitoring (-smsy): " + options.has(SMSY) + "\n" +
        "\t\t- Server sample interval (-smsi): " + options.valueOf(smsi) + " ms\n" +
        "\t\t- Server print interval (-smpi): " + options.valueOf(smpi) + " ms\n" +
        "\t* Buffer size settings:\n" +
        "\t\t- Generated requests (-rbs, equals count if pre-generating): " + rbsV + "\n" +
        "\t\t- Request completion events (-ebs): " + ebsV + "\n" +
        "\t* Pre-generate requests (-p): " + options.has(P) + "\n" +
        "\t* Logging (-l): " + options.has("l") + "\n"
    );

    env = setupEnv(options);
    try (final Exec requestExecutor =
        env.newRequestExecutor(options.valueOf(i), options.valueOf(m), options.valueOf(t))) {

      final int warms = options.valueOf(w);
      final int recordedReqs = reqs - warms;
      final Channel<Req> requestCh = Channels.newChannel(options.has(P) ? reqs : rbsV);
      final Channel<TimingEvent<Res>> eventCh = Channels.newChannel(ebsV);

      final String uri = options.valueOf(u);

      // Requests generator
      final Fiber<Void> reqGen = new Fiber<Void>("req-gen", () -> {
        for (int j = 0; j < reqs; ++j) {
          final Req req;
          try {
            req = env.newRequest(uri);
            requestCh.send(req);
          } catch (final Exception e) {
            LOG.error("Got exception when constructing request: " + e.getMessage());
          }
        }

        requestCh.close();
      }).start();

      if (options.has(P)) {
        reqGen.join();
      }

      final Histogram histogram = new Histogram(options.valueOf(x), options.valueOf(d));

      // Event recording, both HistHDR and logging
      final ProgressLogger<Res, Exec> resExecProgressLogger = new ProgressLogger<>(requestExecutor, recordedReqs, options.has(CMSY), options.valueOf(cmsi), options.valueOf(cmpi));
      final HdrHistogramRecorder hdrHistogramRecorder = new HdrHistogramRecorder(histogram, 1);
      final Fiber recorder;
      if (options.has("l")) {
        recorder = record(eventCh, hdrHistogramRecorder, new LoggingRecorder(LOG), resExecProgressLogger);
      } else {
        recorder = record(eventCh, hdrHistogramRecorder, resExecProgressLogger);
      }

      // Reset and start server monitoring
      simpleBlockingGET(options.valueOf(z) + "/reset");
      simpleBlockingGET(options.valueOf(z) + "/start?sampleIntervalMS=" + options.valueOf(smsi) + "&printIntervalMS=" + options.valueOf(smpi) + "&sysMon=" + options.has(SMSY));

      // Main
      new Fiber<Void>("jbender", () -> {
        if (options.has(n)) {
          System.err.println("=============== CONCURRENCY TEST ==============");
          JBender.loadTestConcurrency(options.valueOf(n), warms, requestCh, requestExecutor, eventCh, sf);
        } else {
          System.err.println("================== RATE TEST ==================");
          IntervalGenerator intervalGen = null;

          if (options.has(r))
            intervalGen = new ExponentialIntervalGenerator(options.valueOf(r));
          else if (options.has(v))
            intervalGen = new ConstantIntervalGenerator(options.valueOf(v));

          JBender.loadTestThroughput(intervalGen, warms, requestCh, requestExecutor, eventCh, sf);
        }
      }).start().join();

      // Stop server monitoring
      simpleBlockingGET(options.valueOf(z) + "/stop");

      recorder.join();

      histogram.outputPercentileDistribution(System.err, options.valueOf(s));

      e.shutdown();
    } catch (final Throwable e) {
      LOG.error("Got exception: " + e.getMessage());
      LOG.error(Arrays.toString(e.getStackTrace()));
    }
  }

  private static void simpleBlockingGET(String httpUrl) throws IOException {
    HttpURLConnection _c = null;
    try {
      _c = (HttpURLConnection) new URL(httpUrl).openConnection();
    } finally {
      if (_c != null) {
        _c.getInputStream().close();
        _c.disconnect();
      }
      LOG.info("Performed GET " + httpUrl);
    }
  }

  private static final Logger LOG = LoggerFactory.getLogger(ClientBase.class);

  public static ConnectorProvider jerseyConnProvider(ClientConfig cc) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
    final String prov = System.getProperty("jersey.provider", "jetty");

    if ("jetty".equals(prov.toLowerCase())) {
      LOG.info("Using Custom Jersey Jetty");
      return new CustomJettyConnectorProvider();
    }

    if ("apache".equals(prov.toLowerCase())) {
      final PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
      connectionManager.setMaxTotal(100000);
      connectionManager.setDefaultMaxPerRoute(100000);
      cc.property(ApacheClientProperties.CONNECTION_MANAGER, connectionManager);

      LOG.info("Using re-configured Jersey Apache");
      return new ApacheConnectorProvider();
    }

    if ("grizzly".equals(prov.toLowerCase())) {
      LOG.info("Using re-configured Jersey Grizzly");

      return new GrizzlyConnectorProvider((client, config, configBuilder) -> configBuilder
        .setMaximumConnectionsPerHost(100000)
        .setMaximumConnectionsTotal(100000));
    }

    if ("jdk".equals(prov.toLowerCase())) {
      System.setProperty("http.maxConnections", "100000");
      LOG.info("Using re-configured Jersey JDK");
      return new HttpUrlConnectorProvider();
    }

    return (ConnectorProvider) Class.forName(prov).newInstance();
  }

  public static <X> void validate(Validator<X> validator, X v) {
    if (v != null)
      validator.validate(v);
  }
}
