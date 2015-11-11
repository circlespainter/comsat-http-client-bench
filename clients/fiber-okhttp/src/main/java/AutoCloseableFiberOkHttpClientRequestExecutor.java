import co.paralleluniverse.fibers.RuntimeExecutionException;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.okhttp.FiberOkHttpClient;
import com.pinterest.jbender.executors.Validator;
import com.squareup.okhttp.Dispatcher;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

public class AutoCloseableFiberOkHttpClientRequestExecutor implements AutoCloseableRequestExecutor<Request, Response> {
  protected final Validator<Response> validator;
  protected final OkHttpClient client;

  public AutoCloseableFiberOkHttpClientRequestExecutor(OkHttpClient client, Validator<Response> resValidator, int ioParallelism, int maxConnections, int timeout) {
    this.validator = resValidator;
    this.client = client;
    client.setDispatcher(new Dispatcher(new ForkJoinPool(ioParallelism)));
    client.getDispatcher().setMaxRequests(maxConnections);
    client.getDispatcher().setMaxRequestsPerHost(maxConnections);
    client.setConnectTimeout(timeout, TimeUnit.MILLISECONDS);
    client.setReadTimeout(timeout, TimeUnit.MILLISECONDS);
    client.setWriteTimeout(timeout, TimeUnit.MILLISECONDS);
  }

  public AutoCloseableFiberOkHttpClientRequestExecutor(Validator<Response> resValidator, int ioParallelism, int maxReqs, int timeout) {
    this(new FiberOkHttpClient(), resValidator, ioParallelism, maxReqs, timeout);
  }

  public Response execute(long nanoTime, Request request) throws InterruptedException, SuspendExecution {
    Response ret;
    try {
      ret = client.newCall(request).execute();
    } catch (final IOException e) {
      throw new RuntimeExecutionException(e);
    }

    if(this.validator != null) {
      this.validator.validate(ret);
    }

    return ret;
  }

  public void close() throws IOException {
    // TODO check there's nothing to do
  }
}
