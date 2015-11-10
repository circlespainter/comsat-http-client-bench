public interface Env<R, E> {
  E newRequestExecutor(int ioParallelism, int maxConnections, int timeoutMS) throws Exception;
  R newRequest(String address) throws Exception;
}
