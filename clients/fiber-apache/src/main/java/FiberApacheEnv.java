import com.pinterest.jbender.executors.http.FiberApacheHttpClientRequestExecutor;
import org.apache.http.client.methods.HttpGet;

public class FiberApacheEnv implements Env<HttpGet, FiberApacheHttpClientRequestExecutor<HttpGet>> {
  @Override
  public FiberApacheHttpClientRequestExecutor<HttpGet> newRequestExecutor(int ioParallelism, int maxConnections, int timeout) throws Exception {
    return new FiberApacheHttpClientRequestExecutor<>(r -> {
      if (r.getStatusLine().getStatusCode() - 200 > 100)
        throw new AssertionError("Request didn't complete successfully: " + r.getStatusLine().toString());
    }, ioParallelism, maxConnections, timeout);
  }

  @Override
  public HttpGet newRequest(String uri) throws Exception {
    return new HttpGet(uri);
  }
}
