import com.pinterest.jbender.executors.Validator;
import com.pinterest.jbender.executors.http.FiberApacheHttpClientRequestExecutor;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.nio.reactor.IOReactorException;

public class AutoCloseableFiberApacheHttpClientRequestExecutor<X extends HttpRequestBase> extends FiberApacheHttpClientRequestExecutor<X> implements AutoCloseableRequestExecutor<X, CloseableHttpResponse> {
  public AutoCloseableFiberApacheHttpClientRequestExecutor(Validator<CloseableHttpResponse> resValidator, int maxConnections, int timeout, int parallelism) throws IOReactorException {
    super(resValidator, maxConnections, timeout, parallelism);
  }

  public AutoCloseableFiberApacheHttpClientRequestExecutor(Validator<CloseableHttpResponse> resValidator, int maxConnections, int timeout) throws IOReactorException {
    super(resValidator, maxConnections, timeout);
  }

  public AutoCloseableFiberApacheHttpClientRequestExecutor(Validator<CloseableHttpResponse> resValidator, int maxConnections) throws IOReactorException {
    super(resValidator, maxConnections);
  }

  public AutoCloseableFiberApacheHttpClientRequestExecutor(int maxConnections) throws IOReactorException {
    super(maxConnections);
  }
}
