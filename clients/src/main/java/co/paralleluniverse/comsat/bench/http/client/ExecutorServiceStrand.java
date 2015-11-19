package co.paralleluniverse.comsat.bench.http.client;

import co.paralleluniverse.strands.Strand;
import co.paralleluniverse.strands.SuspendableCallable;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class ExecutorServiceStrand extends Strand {
  private static final AtomicLong idGen = new AtomicLong();

  private final SuspendableCallable<?> target;
  private final Long id = idGen.getAndIncrement();
  private final ExecutorService es;

  private Object result = null;
  private String name = null;
  private Future<?> underlying = null;

  public ExecutorServiceStrand(ExecutorService es, SuspendableCallable<?> sc) {
    this(null, es, sc);
  }

  public ExecutorServiceStrand(String name, ExecutorService es, SuspendableCallable<?> sc) {
    this.target = sc;
    this.es = es;
    this.name = name;
  }

  @Override
  public boolean isFiber() {
    return false;
  }

  @Override
  public Object getUnderlying() {
    return underlying;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public Strand setName(String name) {
    this.name = name;
    return this;
  }

  @Override
  public boolean isAlive() {
    return !underlying.isDone() && !underlying.isCancelled();
  }

  @Override
  public boolean isTerminated() {
    return !isAlive();
  }

  @Override
  public Strand start() {
    this.underlying = es.submit((Callable) target::run);
    return this;
  }

  @Override
  public void join() throws ExecutionException, InterruptedException {
    result = underlying.get();
  }

  @Override
  public void join(long timeout, TimeUnit unit) throws ExecutionException, InterruptedException, TimeoutException {
    result = underlying.get(timeout, unit);
  }

  @Override
  public Object get() throws ExecutionException, InterruptedException {
    join();
    return result;
  }

  @Override
  public Object get(long timeout, TimeUnit unit) throws ExecutionException, InterruptedException, TimeoutException {
    join(timeout, unit);
    return result;
  }

  @Override
  public void interrupt() {
    underlying.cancel(true);
  }

  @Override
  public boolean isInterrupted() {
    if (underlying == null)
      return false;
    return underlying.isCancelled();
  }

  @Override
  public InterruptedException getInterruptStack() {
    return null;
  }

  @Override
  public void unpark() {
    // Nothing to be done
  }

  @Override
  public void unpark(Object unblocker) {
    // Nothing to be done
  }

  @Override
  public Object getBlocker() {
    return null;
  }

  @Override
  public State getState() {
    if (isAlive())
      return State.RUNNING;
    if (result != null || underlying != null)
      return State.TERMINATED;
    else
      return State.NEW;
  }

  @Override
  public StackTraceElement[] getStackTrace() {
    return new StackTraceElement[0];
  }

  @Override
  public long getId() {
    return 0;
  }

  @Override
  public void setUncaughtExceptionHandler(UncaughtExceptionHandler eh) {
    throw new UnsupportedOperationException();
  }

  @Override
  public UncaughtExceptionHandler getUncaughtExceptionHandler() {
    throw new UnsupportedOperationException();
  }
}
