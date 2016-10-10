package com.geekingfrog.traytest.db

import akka.actor.Actor
import akka.actor.ActorRef
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

class WorkflowExecutionTable(workflowTable: ActorRef) extends Actor {
  val log = Logging(context.system, this)
  val store = new HashMap[(Int, Int), WorkflowExecution]()
  var currentIndex: Int = 0

  def receive = {
    case "test" => log.info("received test")
    case WorkflowExecutionProtocol.Create(workflowId) => {
      implicit val timeout = Timeout(1 seconds)
      log.info(s"creating workflow execution for workflow for id $workflowId\n")

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
        case Some(execution) if execution.remainingStep <= 0 => {
          Left(WorkflowExecutionProtocol.ExecutionFinished)
        }
        case Some(execution) => {
          store.put((workflowId, workflowExecutionId), execution.copy(remainingStep=execution.remainingStep-1))
          Right()
        }
      }
      sender ! response
    }

    case WorkflowExecutionProtocol.Query(workflowId, workflowExecutionId) => {
      val response = store.get((workflowId, workflowExecutionId))
      log.info(s"querying stuff: ${response}")
      sender ! response
    }

    case _      => log.info("received unknown message")
  }
}
