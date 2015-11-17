import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;

public class ThreadOkHttpEnv implements Env<Request, AutoCloseableOkHttpClientRequestExecutor> {

  @Override
  public AutoCloseableOkHttpClientRequestExecutor newRequestExecutor(int ignored_ioParallelism, int maxConnections, int timeout) throws Exception {
    return new AutoCloseableOkHttpClientRequestExecutor(new OkHttpClient(), AutoCloseableOkHttpClientRequestExecutor.DEFAULT_VALIDATOR, maxConnections, timeout);
  }

  @Override
  public Request newRequest(String uri) throws Exception {
    return new Request.Builder().get().url(uri).build();
  }
}
