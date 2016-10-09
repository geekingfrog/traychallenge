package com.geekingfrog.traytest

import akka.actor.ActorSystem
import akka.actor.Props
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.pattern.ask
import akka.http.scaladsl.server.ValidationRejection
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import scala.io.StdIn
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport

import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.{Await, Future, ExecutionContext}

import com.geekingfrog.traytest._
import com.geekingfrog.traytest.db.{WorkflowTable, WorkflowExecutionTable}
import com.geekingfrog.traytest.protocol.{workflowProtocol => WorkflowProtocol}
import com.geekingfrog.traytest.protocol.{workflowExecutionProtocol => WorkflowExecutionProtocol}

object WebServer extends JsonSupport {
  def main(args: Array[String]): Unit = {

    implicit val system = ActorSystem("worflow-manager")
    implicit val materializer = ActorMaterializer()
    // required for the future flatMap/onComplete in the end
    implicit val executionContext = system.dispatcher

    // required when using actor.ask
    implicit val timeout = Timeout(1 seconds)

    val workflowTable = system.actorOf(Props[WorkflowTable], "worflowTableActor")
    val workflowExecutionTable = system.actorOf(Props[WorkflowExecutionTable], "worflowExecutionTableActor")

    import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport

    val route =
      pathPrefix("workflows") {
        pathEndOrSingleSlash {
          post {

            entity(as[CreateWorkflow]) { createWorkflow =>
              complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, s"creating workflow with some steps ${createWorkflow.number_of_steps}"))
              createWorkflow.number_of_steps match {
                case n if n <= 0 => reject(ValidationRejection(s"Number of steps should be >0, but got ${n}"))
                case n => {
                  val future = workflowTable ? WorkflowProtocol.Create(n)
                  val result = Await.result(future, timeout.duration).asInstanceOf[Int]
                  complete { WorkflowCreated(result) }
                }
              }
            }

          }
        } ~
        pathPrefix(IntNumber / "executions") { workflowId =>
          pathEndOrSingleSlash {
            post {
              complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, s"creating new execution workflow for $workflowId"))
            }
          } ~
          pathPrefix(IntNumber) { executionId =>
            put {
              complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, s"Executing $executionId for workflow $workflowId"))
            } ~
            get {
              complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, s"query status for workflow $workflowId exec $executionId"))
            }
          }
        }
      }

      // path("workflows" / IntNumber) { stuff =>
      //   complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, s"yooo"))
      // } ~
      //
      // path("hello") {
      //   get {
      //     implicit val timeout = Timeout(1 seconds)
      //     workflowTable ! WorkflowProtocol.Create(10)
      //     val future = workflowExecutionTable ? WorkflowExecutionProtocol.Create(0)
      //     val result = Await.result(future, timeout.duration).asInstanceOf[Option[WorkflowExecution]]
      //     complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, s"got stuff: $result"))
      //     // val future = workflowTable ? WorkflowProtocol.Create(213)
      //     // val result = Await.result(future, timeout.duration)
      //     // complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "result: " + result))
      //   }
      // }

    val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)

    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
      StdIn.readLine() // let it run until user presses return
      bindingFuture
        .flatMap(_.unbind()) // trigger unbinding from the port
        .onComplete(_ => system.terminate()) // and shutdown when done
  }
}
