import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.channels.Channel;
import co.paralleluniverse.strands.channels.Channels;
import com.pinterest.jbender.executors.Validator;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.nio.reactor.IOReactorException;
import java.io.IOException;

public class AutoCloseableThreadApacheHttpClientRequestExecutor<X extends HttpRequestBase> implements AutoCloseableRequestExecutor<X, CloseableHttpResponse> {
  private final Validator<CloseableHttpResponse> validator;
  private final CloseableHttpClient client;
  private final RequestConfig requestConfig;

  public AutoCloseableThreadApacheHttpClientRequestExecutor(Validator<CloseableHttpResponse> resValidator, int maxConnections, int timeout) throws IOReactorException {
    this.client = HttpClients.custom()
      .setMaxConnTotal(maxConnections)
      .setMaxConnPerRoute(maxConnections)
      .build();
    this.requestConfig =
      RequestConfig.custom()
        .setSocketTimeout(timeout)
        .setConnectTimeout(timeout)
        .setConnectionRequestTimeout(timeout)
        .build();
    this.validator = resValidator;
  }

  public CloseableHttpResponse execute(long nanoTime, HttpRequestBase request) throws InterruptedException, SuspendExecution {
    request.setConfig(requestConfig);
    final CloseableHttpResponse ret;
    try {
      ret = this.client.execute(request);
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }

    if(this.validator != null) {
      this.validator.validate(ret);
    }

    try {
      ret.close();
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }

    return ret;
  }

  public void close() throws IOException {
    this.client.close();
  }
}
