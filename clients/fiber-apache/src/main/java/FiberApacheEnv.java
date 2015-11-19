import co.paralleluniverse.comsat.bench.http.client.Env;
import co.paralleluniverse.fibers.httpclient.FiberHttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.impl.nio.reactor.IOReactorConfig;

public class FiberApacheEnv implements Env<HttpGet, AutoCloseableApacheHttpClientRequestExecutor<HttpGet>> {
  @Override
  public AutoCloseableApacheHttpClientRequestExecutor<HttpGet> newRequestExecutor(int ioParallelism, int maxConnections, int timeout) throws Exception {
    final DefaultConnectingIOReactor ioReactor = new DefaultConnectingIOReactor(IOReactorConfig.custom().setConnectTimeout(timeout).setIoThreadCount(ioParallelism).setSoTimeout(timeout).build());
    final PoolingNHttpClientConnectionManager mgr = new PoolingNHttpClientConnectionManager(ioReactor);
    mgr.setDefaultMaxPerRoute(maxConnections);
    mgr.setMaxTotal(maxConnections);

    CloseableHttpAsyncClient ahc =
      HttpAsyncClientBuilder
        .create()
        .setConnectionManager(mgr)
        .setDefaultRequestConfig(AutoCloseableApacheHttpClientRequestExecutor.defaultRequestConfig(timeout))
        .build();

    return new AutoCloseableApacheHttpClientRequestExecutor<> (
      new FiberHttpClient(ahc),
      AutoCloseableApacheHttpClientRequestExecutor.DEFAULT_VALIDATOR
    );
  }

  @Override
  public HttpGet newRequest(String uri) throws Exception {
    return new HttpGet(uri);
  }
}
