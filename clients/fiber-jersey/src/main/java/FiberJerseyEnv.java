import co.paralleluniverse.fibers.ws.rs.client.AsyncClientBuilder;
import org.glassfish.jersey.client.ClientProperties;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;

public class FiberJerseyEnv implements Env<Invocation.Builder, AutoCloseableFiberJerseyHttpClientRequestExecutor> {
  private Client client;

  @Override
  public AutoCloseableFiberJerseyHttpClientRequestExecutor newRequestExecutor(int ioParallelism, int ignored, int timeout) throws Exception {
    this.client = AsyncClientBuilder.newClient()
//      .property(ClientProperties.ASYNC_THREADPOOL_SIZE, ioParallelism)
      .property(ClientProperties.CONNECT_TIMEOUT, timeout)
      .property(ClientProperties.READ_TIMEOUT, timeout);
    return new AutoCloseableFiberJerseyHttpClientRequestExecutor(r -> {
      final int sc = r.getStatus();
      if (sc != 200 && sc != 204)
        throw new AssertionError("Request didn't complete successfully: " + sc);
    });
  }

  @Override
  public Invocation.Builder newRequest(String uri) throws Exception {
    return client.target(uri).request();
  }
}
