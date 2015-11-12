import co.paralleluniverse.fibers.SuspendExecution;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import joptsimple.OptionSet;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class Main extends ClientBase<Request, Response, AutoCloseableOkHttpClientRequestExecutor, FiberOkHttpEnv> {
  @Override
  protected FiberOkHttpEnv setupEnv(OptionSet options) {
    return new FiberOkHttpEnv();
  }

  public static void main(String[] args) throws InterruptedException, ExecutionException, SuspendExecution, IOException {
    new Main().run(args, DEFAULT_FIBERS_SF);
  }
}
