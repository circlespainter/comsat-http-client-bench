import co.paralleluniverse.fibers.SuspendExecution;
import com.pinterest.jbender.executors.Validator;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Response;
import java.io.IOException;

public class AutoCloseableJerseyHttpClientRequestExecutor implements AutoCloseableRequestExecutor<Invocation.Builder, Response> {
  protected final Validator<Response> validator;

  public AutoCloseableJerseyHttpClientRequestExecutor(Validator<Response> resValidator) {
    this.validator = resValidator;
  }

  public Response execute(long nanoTime, Invocation.Builder request) throws InterruptedException, SuspendExecution {
    final Response ret = request.get();

    if(this.validator != null) {
      this.validator.validate(ret);
    }

    ret.close();

    return ret;
  }

  public void close() throws IOException {
    // Stateless, nothing to do
  }
}
