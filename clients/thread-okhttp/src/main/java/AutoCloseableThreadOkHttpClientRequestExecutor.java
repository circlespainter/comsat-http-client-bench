import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.channels.Channel;
import co.paralleluniverse.strands.channels.Channels;
import com.pinterest.jbender.executors.Validator;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;

public class AutoCloseableThreadOkHttpClientRequestExecutor extends AutoCloseableFiberOkHttpClientRequestExecutor {
  public AutoCloseableThreadOkHttpClientRequestExecutor(Validator<Response> resValidator, int ioParallelism, int maxConnections, int timeout) {
    super(new OkHttpClient(), resValidator, ioParallelism, maxConnections, timeout);
  }

  public Response execute(long nanoTime, Request request) throws InterruptedException, SuspendExecution {
    Response ret;
    final Channel<Response> c = Channels.newChannel(0);
    new Thread(() -> {
      try {
        c.send(client.newCall(request).execute());
      } catch (final IOException | InterruptedException e) {
        throw new RuntimeException(e);
      } catch (final SuspendExecution e) {
        throw new AssertionError(e);
      }
    });
    ret = c.receive();

    if(this.validator != null) {
      this.validator.validate(ret);
    }

    return ret;
  }
}
