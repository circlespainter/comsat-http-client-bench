import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.channels.Channel;
import co.paralleluniverse.strands.channels.Channels;
import com.pinterest.jbender.executors.Validator;

import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Response;

public class AutoCloseableThreadJerseyHttpClientRequestExecutor extends AutoCloseableFiberJerseyHttpClientRequestExecutor {

  public AutoCloseableThreadJerseyHttpClientRequestExecutor(Validator<Response> resValidator) {
    super(resValidator);
  }

  public Response execute(long nanoTime, Invocation.Builder request) throws InterruptedException, SuspendExecution {
    final Response ret;
    final Channel<Response> c = Channels.newChannel(0);
    new Thread(() -> {
      try {
        c.send(request.get());
      } catch (final InterruptedException e) {
        throw new RuntimeException(e);
      } catch (final SuspendExecution e) {
        throw new AssertionError(e);
      }
    }).start();
    ret = c.receive();

    if (this.validator != null) {
      this.validator.validate(ret);
    }

    ret.close();

    return ret;
  }
}
