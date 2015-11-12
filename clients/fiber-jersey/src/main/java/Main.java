import co.paralleluniverse.fibers.SuspendExecution;
import joptsimple.OptionSet;

import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class Main extends ClientBase<Invocation.Builder, Response, AutoCloseableJerseyHttpClientRequestExecutor, FiberJerseyEnv> {
  @Override
  protected FiberJerseyEnv setupEnv(OptionSet options) {
    return new FiberJerseyEnv();
  }

  public static void main(String[] args) throws InterruptedException, ExecutionException, SuspendExecution, IOException {
    final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("org.eclipse.jetty");
    if (!(logger instanceof ch.qos.logback.classic.Logger)) {
      return;
    }
    ch.qos.logback.classic.Logger logbackLogger = (ch.qos.logback.classic.Logger) logger;
    logbackLogger.setLevel(ch.qos.logback.classic.Level.WARN);

    new Main().run(args, DEFAULT_FIBERS_SF);
  }
}
