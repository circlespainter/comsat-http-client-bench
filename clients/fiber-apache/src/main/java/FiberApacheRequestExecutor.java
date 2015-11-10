import co.paralleluniverse.fibers.SuspendExecution;

public class FiberApacheRequestExecutor implements AutoCloseableRequestExecutor {
  @Override
  public void close() throws Exception {

  }

  @Override
  public Object execute(long l, Object o) throws SuspendExecution, InterruptedException {
    return null;
  }
}
