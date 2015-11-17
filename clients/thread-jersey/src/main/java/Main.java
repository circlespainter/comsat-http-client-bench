import co.paralleluniverse.fibers.SuspendExecution;
import joptsimple.OptionSet;

import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main extends ClientBase<Invocation.Builder, String, AutoCloseableJerseyHttpClientRequestExecutor, ThreadJerseyEnv> {
  @Override
  protected ThreadJerseyEnv setupEnv(OptionSet options) {
    return new ThreadJerseyEnv();
  }

  public static void main(String[] args) throws InterruptedException, ExecutionException, SuspendExecution, IOException {
    new Main().run(args, CACHED_THREAD_SF);
  }
}
