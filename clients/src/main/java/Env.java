public interface Env<R, E extends AutoCloseableRequestExecutor<R, ?>> {
  E newRequestExecutor(int ioParallelism, int maxConnections, int timeoutMS) throws Exception;
  R newRequest(String address) throws Exception;
}
