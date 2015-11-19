import co.paralleluniverse.comsat.bench.http.client.ClientBase;
import co.paralleluniverse.comsat.bench.http.client.Env;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;

public class ThreadJerseyEnv implements Env<Invocation.Builder, AutoCloseableJerseyHttpClientRequestExecutor> {
  private Client client;

  @Override
  public AutoCloseableJerseyHttpClientRequestExecutor newRequestExecutor(int ioParallelism, int ignored, int timeout) throws Exception {
    final ClientConfig cc =
      new ClientConfig()
//        .property(ClientProperties.ASYNC_THREADPOOL_SIZE, ioParallelism) // Will limit threads even in sync case...
        .property(ClientProperties.CONNECT_TIMEOUT, timeout)
        .property(ClientProperties.READ_TIMEOUT, timeout);

    cc.connectorProvider(ClientBase.jerseyConnProvider(cc));

    this.client = ClientBuilder.newClient(cc);

    return new AutoCloseableJerseyHttpClientRequestExecutor(client, AutoCloseableJerseyHttpClientRequestExecutor.REQUEST_VALIDATOR);
  }

  @Override
  public Invocation.Builder newRequest(String url) throws Exception {
    return client.target(url).request();
  }
}
