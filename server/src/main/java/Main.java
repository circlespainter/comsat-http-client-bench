import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.Suspendable;
import co.paralleluniverse.fibers.dropwizard.FiberApplication;
import io.dropwizard.Configuration;
import io.dropwizard.setup.*;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

public class Main extends FiberApplication<Configuration> {
  public static void main(String[] args) throws Exception {
    new Main().run(args);
  }

  @Override
  public void initialize(Bootstrap<Configuration> bootstrap) {}

  @Override
  public void fiberRun(Configuration cfg, Environment env) throws ClassNotFoundException {
    env.jersey().register(new HelloWorldResource());
  }

  @Path("/")
  @Produces(MediaType.TEXT_PLAIN)
  public static class HelloWorldResource {
    @GET @Suspendable public String sayHello(@QueryParam("sleepMS") Long sleepMS) {
      try {
        Fiber.sleep(sleepMS != null ? sleepMS : 1_000L);
        return "Hello!";
      } catch (final Throwable t) {
        throw new AssertionError(t);
      }
    }
  }
}
