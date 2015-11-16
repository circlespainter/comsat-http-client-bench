import co.paralleluniverse.fibers.SuspendExecution;
import joptsimple.OptionSet;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class Main extends ClientBase<HttpGet, CloseableHttpResponse, AutoCloseableApacheHttpClientRequestExecutor<HttpGet>, ThreadApacheEnv> {

  @Override
  protected ThreadApacheEnv setupEnv(OptionSet options) {
    return new ThreadApacheEnv();
  }

  public static void main(String[] args) throws InterruptedException, ExecutionException, SuspendExecution, IOException {
    new Main().run(args, CACHED_THREAD_SF);
  }
}
