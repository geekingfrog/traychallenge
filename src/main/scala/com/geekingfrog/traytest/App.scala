package com.geekingfrog.traytest

import akka.actor.ActorSystem
import akka.actor.Props
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.pattern.ask
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import scala.io.StdIn

import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.{Await, Future, ExecutionContext}

import com.geekingfrog.traytest.db.WorkflowTable
import com.geekingfrog.traytest.protocol.workflowProtocol._

object WebServer {
  def main(args: Array[String]): Unit = {

    implicit val system = ActorSystem("worflow-manager")
    implicit val materializer = ActorMaterializer()
    // needed for the future flatMap/onComplete in the end
    implicit val executionContext = system.dispatcher

    val workflowTable = system.actorOf(Props[WorkflowTable], "worflowTableActor")

    val route =
      path("hello") {
        get {
          implicit val timeout = Timeout(1 seconds)
          val future = workflowTable ? Create(213)
          val result = Await.result(future, timeout.duration)
          complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "result: " + result))
        }
      }

    val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)

    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
      StdIn.readLine() // let it run until user presses return
      bindingFuture
        .flatMap(_.unbind()) // trigger unbinding from the port
        .onComplete(_ => system.terminate()) // and shutdown when done
  }
}
