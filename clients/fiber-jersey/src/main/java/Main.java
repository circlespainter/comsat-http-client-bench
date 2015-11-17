import co.paralleluniverse.fibers.SuspendExecution;
import joptsimple.OptionSet;

import javax.ws.rs.client.Invocation;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main extends ClientBase<Invocation.Builder, String, AutoCloseableJerseyHttpClientRequestExecutor, FiberJerseyEnv> {
  @Override
  protected FiberJerseyEnv setupEnv(OptionSet options) {
    return new FiberJerseyEnv();
  }

  public static void main(String[] args) throws InterruptedException, ExecutionException, SuspendExecution, IOException {
    new Main().run(args, DEFAULT_FIBERS_SF);
  }
}
