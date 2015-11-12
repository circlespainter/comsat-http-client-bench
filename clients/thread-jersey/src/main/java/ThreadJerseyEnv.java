import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.spi.ConnectorProvider;
import org.glassfish.jersey.jetty.connector.JettyConnectorProvider;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Configuration;

public class ThreadJerseyEnv implements Env<Invocation.Builder, AutoCloseableJerseyHttpClientRequestExecutor> {
  private Client client;

  @Override
  public AutoCloseableJerseyHttpClientRequestExecutor newRequestExecutor(int ioParallelism, int ignored, int timeout) throws Exception {
    final Configuration config = new ClientConfig().connectorProvider((ConnectorProvider) Class.forName(System.getProperty("jersey.provider", JettyConnectorProvider.class.getName())).newInstance());

    this.client = ClientBuilder.newClient(config)
//      .property(ClientProperties.ASYNC_THREADPOOL_SIZE, ioParallelism)
      .property(ClientProperties.CONNECT_TIMEOUT, timeout)
      .property(ClientProperties.READ_TIMEOUT, timeout);

    return new AutoCloseableJerseyHttpClientRequestExecutor(r -> {
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
