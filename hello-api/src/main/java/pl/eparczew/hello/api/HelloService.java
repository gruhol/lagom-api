package pl.eparczew.hello.api;

import akka.Done;
import akka.NotUsed;
import com.lightbend.lagom.javadsl.api.Descriptor;
import com.lightbend.lagom.javadsl.api.Service;
import com.lightbend.lagom.javadsl.api.ServiceCall;
import com.lightbend.lagom.javadsl.api.broker.Topic;
import com.lightbend.lagom.javadsl.api.broker.kafka.KafkaProperties;
import com.lightbend.lagom.javadsl.api.transport.Method;

import static com.lightbend.lagom.javadsl.api.Service.*;

/**
 * The Hello service interface.
 * <p>
 * This describes everything that Lagom needs to know about how to serve and
 * consume the Hello.
 */
public interface HelloService extends Service {

  /**
   * Example: curl http://localhost:9000/api/hello/Alice
   */
  ServiceCall<NotUsed, String> hello(String id);


  /**
   * Example: curl -H "Content-Type: application/json" -X POST -d '{"message":
   * "Hi"}' http://localhost:9000/api/hello/Alice
   */
  ServiceCall<GreetingMessage, Done> useGreeting(String id);


  /**
   * This gets published to Kafka.
   */

  ServiceCall<FilesToCompare, String> compare();

  @Override
  default Descriptor descriptor() {
    // @formatter:off
    return named("hello").withCalls(
        pathCall("/api/hello/:id",  this::hello),
        pathCall("/api/hello/:id", this::useGreeting),
        //pathCall("/compare", this::compare)
        restCall(Method.POST, "/compare", this::compare)
        ).withAutoAcl(true);
    // @formatter:on
  }
}
