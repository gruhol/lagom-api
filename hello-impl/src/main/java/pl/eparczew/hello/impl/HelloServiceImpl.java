package pl.eparczew.hello.impl;

import akka.Done;
import akka.NotUsed;
import akka.cluster.sharding.typed.javadsl.ClusterSharding;
import akka.cluster.sharding.typed.javadsl.Entity;
import akka.cluster.sharding.typed.javadsl.EntityRef;
import akka.japi.Pair;
import com.lightbend.lagom.javadsl.api.ServiceCall;
import com.lightbend.lagom.javadsl.api.broker.Topic;
import com.lightbend.lagom.javadsl.api.transport.BadRequest;
import com.lightbend.lagom.javadsl.broker.TopicProducer;
import com.lightbend.lagom.javadsl.persistence.PersistentEntityRegistry;

import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import pl.eparczew.hello.api.FilesToCompare;
import pl.eparczew.hello.api.GreetingMessage;
import pl.eparczew.hello.api.HelloService;
import pl.eparczew.hello.impl.HelloCommand.*;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Implementation of the HelloService.
 */
public class HelloServiceImpl implements HelloService {

  private final PersistentEntityRegistry persistentEntityRegistry;

  private final Duration askTimeout = Duration.ofSeconds(5);
  private ClusterSharding clusterSharding;

  @Inject
  public HelloServiceImpl(PersistentEntityRegistry persistentEntityRegistry, ClusterSharding clusterSharding){
    this.clusterSharding=clusterSharding;
    // The persistent entity registry is only required to build an event stream for the TopicProducer
    this.persistentEntityRegistry=persistentEntityRegistry;

    // register the Aggregate as a sharded entity
    this.clusterSharding.init(
    Entity.of(
    HelloAggregate.ENTITY_TYPE_KEY,
    HelloAggregate::create
    )
    );
  }

  @Override
  public ServiceCall<NotUsed, String> hello(String id) {
    return request -> {

    // Look up the aggregete instance for the given ID.
    EntityRef<HelloCommand> ref = clusterSharding.entityRefFor(HelloAggregate.ENTITY_TYPE_KEY, id);
    // Ask the entity the Hello command.

    return ref.
      <HelloCommand.Greeting>ask(replyTo -> new Hello(id, replyTo), askTimeout)
      .thenApply(greeting -> greeting.message);    };
  }

  @Override
  public ServiceCall<GreetingMessage, Done> useGreeting(String id) {
    return request -> {

    // Look up the aggregete instance for the given ID.
    EntityRef<HelloCommand> ref = clusterSharding.entityRefFor(HelloAggregate.ENTITY_TYPE_KEY, id);
    // Tell the entity to use the greeting message specified.

    return ref.
      <HelloCommand.Confirmation>ask(replyTo -> new UseGreetingMessage(request.message, replyTo), askTimeout)
          .thenApply(confirmation -> {
              if (confirmation instanceof HelloCommand.Accepted) {
                return Done.getInstance();
              } else {
                throw new BadRequest(((HelloCommand.Rejected) confirmation).reason);
              }
          });
    };

  }

  @Override
  public ServiceCall<FilesToCompare, String> compare() {
    return request -> {
      String responseString;
      responseString = getHttpContent(request.originalfile, request.comparefile);
      return CompletableFuture.completedFuture(responseString);
    };

  }

  private String getHttpContent(String originalFile, String compareFile) {
    HttpResponse<String> response = Unirest.post("http://localhost:8100/compare")
            .header("Content-Type", "application/json")
            .body("{\"originalFile\":\""+ originalFile + "\",\"compareFile\":\"" + compareFile + "\"}")
            .asString();
    return response.getBody();
  }
}
