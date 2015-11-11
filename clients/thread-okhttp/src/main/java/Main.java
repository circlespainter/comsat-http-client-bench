import co.paralleluniverse.fibers.SuspendExecution;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import joptsimple.OptionSet;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class Main extends ClientBase<Request, Response, AutoCloseableThreadOkHttpClientRequestExecutor, ThreadOkHttpEnv> {
  @Override
  protected ThreadOkHttpEnv setupEnv(OptionSet options) {
    return new ThreadOkHttpEnv();
  }

  public static void main(String[] args) throws InterruptedException, ExecutionException, SuspendExecution, IOException {
    new Main().run(args);
  }
}
