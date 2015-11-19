import co.paralleluniverse.comsat.bench.http.client.Env;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;

import java.util.concurrent.TimeUnit;

public class ThreadApacheEnv implements Env<HttpGet, AutoCloseableApacheHttpClientRequestExecutor<HttpGet>> {
  @Override
  public AutoCloseableApacheHttpClientRequestExecutor<HttpGet> newRequestExecutor(int ioParallelism, int maxConnections, int timeout) throws Exception {
    return new AutoCloseableApacheHttpClientRequestExecutor<> (
      HttpClients.custom()
        .setMaxConnTotal(maxConnections)
        .setMaxConnPerRoute(maxConnections)
        .setConnectionTimeToLive(timeout, TimeUnit.MILLISECONDS)
        .setDefaultRequestConfig(AutoCloseableApacheHttpClientRequestExecutor.defaultRequestConfig(timeout))
        .disableAutomaticRetries()
        .build(),
      AutoCloseableApacheHttpClientRequestExecutor.DEFAULT_VALIDATOR
    );
  }

  @Override
  public HttpGet newRequest(String uri) throws Exception {
    return new HttpGet(uri);
  }
}
