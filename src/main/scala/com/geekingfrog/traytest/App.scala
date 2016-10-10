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

// required to bring implicit conversion to marshall entity back to client
import spray.json._

import com.geekingfrog.traytest._
import com.geekingfrog.traytest.db.{WorkflowTable, WorkflowExecutionTable}
import com.geekingfrog.traytest.protocol.{workflowProtocol => WorkflowProtocol}
import com.geekingfrog.traytest.protocol.{workflowExecutionProtocol => WorkflowExecutionProtocol}
import com.geekingfrog.traytest.protocol.tick.Tick


object WebServer extends JsonSupport {
  def main(args: Array[String]): Unit = {

    implicit val system = ActorSystem("worflow-manager")
    implicit val materializer = ActorMaterializer()

    // required for the future flatMap/onComplete in the end
    implicit val executionContext = system.dispatcher

    // required when using actor.ask
    implicit val timeout = Timeout(1 seconds)

    val workflowTable = system.actorOf(Props[WorkflowTable], "workflowTableActor")
    val workflowExecutionTable = system.actorOf(
      Props(classOf[WorkflowExecutionTable], workflowTable), "worflowExecutionTableActor"
    )

    import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport

    val cronActor = system.actorOf(Props(classOf[CronActor], workflowExecutionTable))
    system.scheduler.schedule(0 seconds, 1 minute, cronActor, Tick)

    val route =
      pathPrefix("workflows") {
        pathEndOrSingleSlash {
          post {

            entity(as[CreateWorkflow]) { createWorkflow =>
              createWorkflow.number_of_steps match {
                case n if n <= 0 => reject(ValidationRejection(s"Number of steps should be >0, but got ${n}"))
                case n => {
                  val future = workflowTable ? WorkflowProtocol.Create(n)
                  val result = Await.result(future, timeout.duration).asInstanceOf[Int]
                  val payload = WorkflowCreated(workflow_id=result.toString)
                  complete(HttpResponse(201, entity=payload.toJson.toString))
                }
              }
            }

          }
        } ~
        pathPrefix(IntNumber / "executions") { workflowId =>
          pathEndOrSingleSlash {
            post {
              val future = workflowExecutionTable ? WorkflowExecutionProtocol.Create(workflowId)
              val workflow = Await.result(future, timeout.duration).asInstanceOf[Option[WorkflowExecution]]

              val resp = workflow match {
                case None => HttpResponse(404)
                case Some(workflowExecution) => {
                  val payload = WorkflowExecutionCreated(workflowExecution.id.toString)
                  HttpResponse(201, entity=payload.toJson.toString)
                }
              }

              complete(resp)
            }
          } ~
          pathPrefix(IntNumber) { executionId =>
            put {
              val future = workflowExecutionTable ? WorkflowExecutionProtocol.Exec(workflowId, executionId)
              val execution = Await.result(future, timeout.duration).asInstanceOf[Either[WorkflowExecutionProtocol.ExecutionError, Unit]]

              val resp = execution match {
                case Left(WorkflowExecutionProtocol.NoExecution) => HttpResponse(404)
                case Left(WorkflowExecutionProtocol.ExecutionFinished) => HttpResponse(400)
                case _ => HttpResponse(204)
              }
              complete(resp)
            } ~
            get {
              val future = workflowExecutionTable ? WorkflowExecutionProtocol.Query(workflowId, executionId)
              val execution = Await.result(future, timeout.duration).asInstanceOf[Option[WorkflowExecution]]

              val resp = execution match {
                case None => HttpResponse(404)
                case Some(execution) => {
                  val payload = WorkflowQuery(finished=execution.remainingStep<=0)
                  HttpResponse(200, entity=payload.toJson.toString)
                }
              }

              complete(resp)
            }

          }
        }
      }

    val bindingFuture = Http().bindAndHandle(route, "localhost", 9000)

    println(s"Server online at http://localhost:9000/\nPress RETURN to stop...")
      StdIn.readLine() // let it run until user presses return
      bindingFuture
        .flatMap(_.unbind()) // trigger unbinding from the port
        .onComplete(_ => system.terminate()) // and shutdown when done
  }
}
