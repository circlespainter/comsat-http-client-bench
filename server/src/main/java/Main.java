import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.Suspendable;
import co.paralleluniverse.fibers.dropwizard.FiberApplication;
import io.dropwizard.Configuration;
import io.dropwizard.setup.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class Main extends FiberApplication<Configuration> {
  private static Logger log = LoggerFactory.getLogger(Main.class);

  private static AtomicLong concurrency = new AtomicLong(0);
  private static AtomicLong maxConcurrency = new AtomicLong(0);
  private static AtomicLong reqs = new AtomicLong(0);

  private static Timer ts;
  private static Timer tp;

  public static void main(String[] args) throws Exception {
    new Main().run(args);
  }

  @Override
  public void initialize(Bootstrap<Configuration> bootstrap) {}

  @Override
  public void fiberRun(Configuration cfg, Environment env) throws ClassNotFoundException {
    env.jersey().register(new HelloWorldResource());
    env.jersey().register(new MonitorResource());
  }

  @Path("/")
  @Produces(MediaType.TEXT_PLAIN)
  public static class HelloWorldResource {
    @GET @Suspendable public String sayHello(@QueryParam("sleepMS") Long sleepMS) {
      concurrency.incrementAndGet();
      maxConcurrency.updateAndGet((current) -> {
        if (concurrency.get() > current)
          return concurrency.get();
        else
          return current;
      });
      try {
        Fiber.sleep(sleepMS != null ? sleepMS : 1_000L);
        return "Hello!";
      } catch (final Throwable t) {
        throw new AssertionError(t);
      } finally {
        concurrency.decrementAndGet();
        reqs.incrementAndGet();
      }
    }
  }

  @Path("/monitor")
  @Produces(MediaType.TEXT_PLAIN)
  public static class MonitorResource {
    private static AtomicReference<Utils.SysStats> stats = new AtomicReference<>();
    @Path("start") @GET public boolean start(@QueryParam("sampleIntervalMS") Long sampleIntervalMS, @QueryParam("printIntervalMS") Long printIntervalMS, @QueryParam("sysMon") Boolean sysMon) {
      tp = new Timer(true);
      ts = new Timer(true);
      final long actualSampleIntervalMS = sampleIntervalMS != null ? sampleIntervalMS : 100L;
      final boolean actuallySysMon = sysMon == null || sysMon;
      if (actualSampleIntervalMS > 0 && actuallySysMon)
        ts.schedule (
          new TimerTask() {
            @Override
            public synchronized void run() {
              stats.set(Utils.sampleSys());
            }
          },
          0,
          actualSampleIntervalMS
        );

      final long actualPrintIntervalMS = printIntervalMS != null ? printIntervalMS : 100L;
      if (actualPrintIntervalMS > 0)
        tp.schedule (
          new TimerTask() {
            @Override
            public synchronized void run() {
              Utils.SysStats s = stats.get();
              if (s != null || !actuallySysMon)
                log.info (
                  "Concurrency = " + concurrency.get() + " (max = " + maxConcurrency.get() + "), " +
                  "reqs = " + reqs.get() +
                  (actuallySysMon ?
                    ", MEM = " + s.mem + " MB (max = " + s.maxMem +  " MB, avg = " + s.avgMem + " MB)" +
                    ", CPU = " + s.cpu + " (max = " + s.maxCpu + ", avg = " + s.avgCpu + ")"
                    : "")
                );
              }
            },
          actualPrintIntervalMS,
          actualPrintIntervalMS
        );
      log.info (
        "Monitoring start request: sample interval = " + (actualSampleIntervalMS > 0 ? actualSampleIntervalMS + " ms" : "N/A") +
        ", print interval = " + (actualPrintIntervalMS > 0 ? actualPrintIntervalMS + " ms" : "N/A") +
        ", monitor RAM/CPU = " + actuallySysMon);
      return true;
    }

    @Path("stop") @GET public boolean stop() {
      if (tp != null) tp.cancel();
      if (tp != null) ts.cancel();
      log.info("Monitoring stopped");
      return true;
    }

    @Path("reset") @GET public boolean reset() {
      if (tp != null) tp.cancel();
      if (tp != null) ts.cancel();
      concurrency.set(0);
      maxConcurrency.set(0);
      reqs.set(0);
      stats.set(null);
      Utils.resetSampleSys();
      log.info("Monitoring reset");
      return true;
    }
  }
}
