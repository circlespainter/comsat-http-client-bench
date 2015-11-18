import co.paralleluniverse.fibers.SuspendExecution;
import com.pinterest.jbender.executors.Validator;
import org.glassfish.jersey.client.ClientConfig;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class AutoCloseableJerseyHttpClientRequestExecutor extends AutoCloseableRequestExecutor<Invocation.Builder, Response> {
  public static final Validator<Response> REQUEST_VALIDATOR = (Response r) -> {
    if (r.getStatus() != 200 && r.getStatus() != 204)
      throw new AssertionError("Request didn't complete successfully: " + r.getStatus());
  };
  private final Validator<Response> validator;
  private final Client client;

  public AutoCloseableJerseyHttpClientRequestExecutor(Client client, Validator<Response> resValidator) {
    this.client = client;
    this.validator = resValidator;
  }

  @Override
  public Response execute0(long nanoTime, Invocation.Builder request) throws InterruptedException, SuspendExecution {
    final Response ret = request.get(Response.class);

    ClientBase.validate(validator, ret);

    ret.close();

    return ret;
  }

  public void close() throws IOException {
    client.close();
  }
}
