import com.pinterest.jbender.executors.RequestExecutor;

public interface AutoCloseableRequestExecutor<Req, Res> extends RequestExecutor<Req, Res>, AutoCloseable {}
