import co.paralleluniverse.fibers.SuspendExecution;
import com.pinterest.jbender.executors.Validator;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class AutoCloseableJerseyHttpClientRequestExecutor extends AutoCloseableRequestExecutor<Invocation.Builder, String> {
  protected final Validator<String> validator;
  protected final Client client;

  public AutoCloseableJerseyHttpClientRequestExecutor(Client client, Validator<String> resValidator) {
    this.client = client;
    this.validator = resValidator;
  }

  public String execute0(long nanoTime, Invocation.Builder request) throws InterruptedException, SuspendExecution {
    final String ret;
    try {
      ret = request.async().get(String.class).get();
    } catch (final ExecutionException e) {
      throw new RuntimeException(e);
    }

    if (this.validator != null) {
      this.validator.validate(ret);
    }

    return ret;
  }

  public void close() throws IOException {
    client.close();
  }
}
