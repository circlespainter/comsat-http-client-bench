import co.paralleluniverse.comsat.bench.http.client.AutoCloseableRequestExecutor;
import co.paralleluniverse.comsat.bench.http.client.ClientBase;
import co.paralleluniverse.fibers.SuspendExecution;
import com.pinterest.jbender.executors.Validator;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.nio.reactor.IOReactorException;
import java.io.IOException;

public class AutoCloseableApacheHttpClientRequestExecutor<X extends HttpRequestBase> extends AutoCloseableRequestExecutor<X, CloseableHttpResponse> {

  private final Validator<CloseableHttpResponse> validator;
  private final CloseableHttpClient client;

  public static RequestConfig defaultRequestConfig(int timeoutMS) {
    return RequestConfig
      .custom()
      .setLocalAddress(null)
      .setSocketTimeout(timeoutMS)
      .setConnectTimeout(timeoutMS)
      .setConnectionRequestTimeout(timeoutMS)
      .build();
  }

  public static final Validator<CloseableHttpResponse> DEFAULT_VALIDATOR = r -> {
    final int sc = r.getStatusLine().getStatusCode();
    if (sc != 200 && sc != 204)
      throw new AssertionError("Request didn't complete successfully: " + r.getStatusLine().toString());
  };

  public AutoCloseableApacheHttpClientRequestExecutor(CloseableHttpClient client, Validator<CloseableHttpResponse> resValidator) throws IOReactorException {
    this.client = client;
    this.validator = resValidator;
  }

  @Override
  public CloseableHttpResponse execute0(long nanoTime, HttpRequestBase request) throws InterruptedException, SuspendExecution {
    final CloseableHttpResponse ret;
    try {
      ret = this.client.execute(request);
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }

    ClientBase.validate(validator, ret);

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
