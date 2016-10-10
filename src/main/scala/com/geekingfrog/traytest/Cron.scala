package com.geekingfrog.traytest

import akka.actor.Actor
import akka.actor.ActorRef
import akka.event.Logging

import com.geekingfrog.traytest.protocol.tick.Tick
import com.geekingfrog.traytest.protocol.{workflowExecutionProtocol => WorkflowExecutionProtocol}

class CronActor(workflowExecutionTable: ActorRef) extends Actor {
  val log = Logging(context.system, this)

  def receive = {
    case Tick => {
      log.info("Running cron to delete old executions")
      workflowExecutionTable ! WorkflowExecutionProtocol.DeleteOld
    }
    case _      => log.info("received unknown message")
  }
}
