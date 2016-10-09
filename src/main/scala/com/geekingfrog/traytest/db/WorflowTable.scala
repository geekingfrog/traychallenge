package com.geekingfrog.traytest.db

import akka.actor.Actor
import akka.actor.Props
import akka.event.Logging

import scala.collection.mutable.HashMap

import com.geekingfrog.traytest.Workflow
import com.geekingfrog.traytest.protocol.{workflowProtocol => WorkflowProtocol}

class WorkflowTable extends Actor {
  val log = Logging(context.system, this)
  var store = new HashMap[Int, Workflow]()
  var currentIndex: Int = 0

  def receive = {
    case "test" => log.info("received test")
    case WorkflowProtocol.Create(numberOfSteps) => {
      log.info("creating workflow with id: " + numberOfSteps)
      store.put(currentIndex, Workflow(id=currentIndex, numberOfSteps=numberOfSteps))
      sender() ! currentIndex
      currentIndex += 1
    }
    case _      => log.info("received unknown message")
  }
}
