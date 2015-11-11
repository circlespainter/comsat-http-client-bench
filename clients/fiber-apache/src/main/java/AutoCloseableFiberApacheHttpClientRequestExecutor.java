import co.paralleluniverse.fibers.RuntimeExecutionException;
import co.paralleluniverse.fibers.SuspendExecution;
import com.pinterest.jbender.executors.Validator;
import com.pinterest.jbender.executors.http.FiberApacheHttpClientRequestExecutor;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.nio.reactor.IOReactorException;

import java.io.IOException;

public class AutoCloseableFiberApacheHttpClientRequestExecutor<X extends HttpRequestBase> extends FiberApacheHttpClientRequestExecutor<X> implements AutoCloseableRequestExecutor<X, CloseableHttpResponse> {
  private final RequestConfig requestConfig;

  public AutoCloseableFiberApacheHttpClientRequestExecutor(Validator<CloseableHttpResponse> resValidator, int ioParallelism, int maxConnections, int timeout) throws IOReactorException {
    super(resValidator, maxConnections, timeout, ioParallelism);
    this.requestConfig =
      RequestConfig.custom()
        .setSocketTimeout(timeout)
        .setConnectTimeout(timeout)
        .setConnectionRequestTimeout(timeout)
        .build();
  }

  @Override
  public CloseableHttpResponse execute(long nanoTime, HttpRequestBase request) throws SuspendExecution, InterruptedException {
    request.setConfig(requestConfig);
    final CloseableHttpResponse res = super.execute(nanoTime, request);
    try {
      res.close();
    } catch (final IOException e) {
      throw new RuntimeExecutionException(e);
    }
    return res;
  }
}
