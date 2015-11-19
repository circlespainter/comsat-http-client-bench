import co.paralleluniverse.comsat.bench.http.client.Env;
import com.squareup.okhttp.Request;

public class FiberOkHttpEnv implements Env<Request, AutoCloseableOkHttpClientRequestExecutor> {
  @Override
  public AutoCloseableOkHttpClientRequestExecutor newRequestExecutor(int ignored_ioParallelism, int maxConnections, int timeout) throws Exception {
    return new AutoCloseableOkHttpClientRequestExecutor(AutoCloseableOkHttpClientRequestExecutor.DEFAULT_VALIDATOR, maxConnections, timeout);
  }

  @Override
  public Request newRequest(String uri) throws Exception {
    return new Request.Builder().get().url(uri).build();
  }
}
