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
  var store = new HashMap[(Int, Int), WorkflowExecution]()
  var currentIndex: Int = 0

  def receive = {
    case "test" => log.info("received test")
    case WorkflowExecutionProtocol.Create(workflowId) => {
      implicit val timeout = Timeout(1 seconds)
      log.info(s"creating workflow execution for workflow with id with id: $workflowId and idx: $currentIndex\n")

      // get reference to the other table to check the existence of the workflow
      val workflowTable = context.actorSelection("/user/workflowTableActor")

      val futureWorkflow = workflowTable ? WorkflowProtocol.Query(workflowId)
      val result = Await.result(futureWorkflow, timeout.duration).asInstanceOf[Option[Workflow]]

      val response = result match {
        case None => None
        case Some(workflow) => {
          val idx = currentIndex
          currentIndex += 1
          val workflowExecution = WorkflowExecution(
            id=idx,
            workflowId=workflow.id,
            remainingStep=workflow.numberOfSteps,
            creationDate=OffsetDateTime.now()
            )
          store.put((workflow.id, idx), workflowExecution)
          Some(workflowExecution)
        }
      }

      sender ! response
    }

    case WorkflowExecutionProtocol.Exec(workflowId, workflowExecutionId) => {
      val exec = store.get((workflowId, workflowExecutionId))
      val response = exec match {
        case None => Left(WorkflowExecutionProtocol.NoExecution)
        case Some(execution) if execution.remainingStep <= 1 => {
          Left(WorkflowExecutionProtocol.ExecutionFinished)
        }
        case Some(execution) => {
          store.put((workflowId, workflowExecutionId), execution.copy(remainingStep=execution.remainingStep-1))
          Right()
        }
      }
      sender ! response
    }

    case _      => log.info("received unknown message")
  }
}
