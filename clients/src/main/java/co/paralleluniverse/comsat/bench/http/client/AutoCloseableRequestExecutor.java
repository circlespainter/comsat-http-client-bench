package co.paralleluniverse.comsat.bench.http.client;

import co.paralleluniverse.fibers.SuspendExecution;
import com.pinterest.jbender.executors.RequestExecutor;

import java.util.concurrent.atomic.AtomicLong;

public abstract class AutoCloseableRequestExecutor<Req, Res> implements RequestExecutor<Req, Res>, AutoCloseable {
	private AtomicLong concurrency = new AtomicLong(0L);
	private AtomicLong maxConcurrency = new AtomicLong(0L);

	protected abstract Res execute0(long nanoTime, Req request) throws InterruptedException, SuspendExecution;

	protected long getCurrentConcurrency() {
		return concurrency.get();
	}

	protected long getMaxConcurrency() {
		return maxConcurrency.get();
	}

	@Override
	public Res execute(long nanoTime, Req request) throws InterruptedException, SuspendExecution {
		concurrency.incrementAndGet();
		maxConcurrency.updateAndGet((current) -> {
			if (concurrency.get() > current)
				return concurrency.get();
			else
				return current;
		});
		try {
			return execute0(nanoTime, request);
		} finally {
			concurrency.decrementAndGet();
		}
	}
}
