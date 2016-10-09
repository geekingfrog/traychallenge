package com.geekingfrog.traytest.db

import akka.actor.Actor
import akka.actor.Props
import akka.event.Logging
import akka.pattern.ask

import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.{Await, Future, ExecutionContext}

import scala.collection.mutable.HashMap
import java.time.OffsetDateTime

import com.geekingfrog.traytest.{Workflow, WorkflowExecution}
import com.geekingfrog.traytest.db._
import com.geekingfrog.traytest.protocol.{workflowProtocol => WorkflowProtocol}
import com.geekingfrog.traytest.protocol.{workflowExecutionProtocol => WorkflowExecutionProtocol}

class WorkflowExecutionTable extends Actor {
  val log = Logging(context.system, this)
  var store = new HashMap[Int, WorkflowExecution]()
  var currentIndex: Int = 0
  val workflowTable = context.actorOf(Props[WorkflowTable], "worflowTableActor")

  def receive = {
    case "test" => log.info("received test")
    case WorkflowExecutionProtocol.Create(requestId) => {
      implicit val timeout = Timeout(1 seconds)
      log.info(s"creating workflow execution for workflow with id with id: $requestId")
      val futureWorkflow = workflowTable ? WorkflowProtocol.Query(requestId)
      val result = Await.result(futureWorkflow, timeout.duration).asInstanceOf[Option[Workflow]]

      val response = result match {
        case None => None
        case Some(workflow) => {
          val idx = currentIndex
          currentIndex += 1
          val workflowExecution = WorkflowExecution(
            id=workflow.id,
            workflowId=idx,
            remainingStep=workflow.numberOfSteps,
            creationDate=OffsetDateTime.now()
            )
          store.put(idx, workflowExecution)
          Some(workflowExecution)
        }
      }

      sender ! response

          // val result = Await.result(future, timeout.duration)
          // complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "result: " + result))
      // store.put(currentIndex, Workflow(id=currentIndex, numberOfSteps=numberOfSteps))
      // sender() ! currentIndex
      // currentIndex += 1
    }
    case _      => log.info("received unknown message")
  }
}
