import co.paralleluniverse.fibers.DefaultFiberScheduler;
import co.paralleluniverse.fibers.SuspendExecution;
import joptsimple.OptionSet;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class Main extends ClientBase<HttpGet, CloseableHttpResponse, AutoCloseableFiberApacheHttpClientRequestExecutor<HttpGet>, FiberApacheEnv> {

  @Override
  protected FiberApacheEnv setupEnv(OptionSet options) {
    return new FiberApacheEnv();
  }

  public static void main(String[] args) throws InterruptedException, ExecutionException, SuspendExecution, IOException {
    new Main().run(args, DEFAULT_FIBERS_SF);
  }
}
