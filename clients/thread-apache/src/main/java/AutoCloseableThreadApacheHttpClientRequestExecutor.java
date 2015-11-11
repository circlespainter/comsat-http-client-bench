import co.paralleluniverse.common.util.Exceptions;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.channels.Channel;
import co.paralleluniverse.strands.channels.Channels;
import com.pinterest.jbender.executors.RequestExecutor;
import com.pinterest.jbender.executors.Validator;
import com.pinterest.jbender.executors.http.FiberApacheHttpClientRequestExecutor;
import org.apache.http.ConnectionReuseStrategy;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.nio.reactor.IOReactorException;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class AutoCloseableThreadApacheHttpClientRequestExecutor<X extends HttpRequestBase> implements AutoCloseableRequestExecutor<X, CloseableHttpResponse>{
  private final Validator<CloseableHttpResponse> validator;
  private final CloseableHttpClient client;

  public AutoCloseableThreadApacheHttpClientRequestExecutor(Validator<CloseableHttpResponse> resValidator, int maxConnections, int timeout, int parallelism) throws IOReactorException {
    this.client = HttpClients.custom().setMaxConnTotal(maxConnections).setConnectionTimeToLive(timeout, TimeUnit.MILLISECONDS).build();
    this.validator = resValidator;
  }

  public AutoCloseableThreadApacheHttpClientRequestExecutor(Validator<CloseableHttpResponse> resValidator, int maxConnections, int timeout) throws IOReactorException {
    this(resValidator, maxConnections, timeout, Runtime.getRuntime().availableProcessors());
  }

  public AutoCloseableThreadApacheHttpClientRequestExecutor(Validator<CloseableHttpResponse> resValidator, int maxConnections) throws IOReactorException {
    this(resValidator, maxConnections, 0);
  }

  public AutoCloseableThreadApacheHttpClientRequestExecutor(int maxConnections) throws IOReactorException {
    this(null, maxConnections, 0);
  }

  public CloseableHttpResponse execute(long nanoTime, HttpRequestBase request) throws InterruptedException, SuspendExecution {
    CloseableHttpResponse ret;
    final Channel<CloseableHttpResponse> c = Channels.newChannel(0);
    new Thread(() -> {
      try {
        c.send(this.client.execute(request));
      } catch (final IOException | InterruptedException e) {
        throw new RuntimeException(e);
      } catch (final SuspendExecution e) {
        throw new AssertionError(e);
      }
    }).start();
    ret = c.receive();

    if(this.validator != null) {
      this.validator.validate(ret);
    }

    return ret;
  }

  public void close() throws IOException {
    this.client.close();
  }
}
