package com.geekingfrog.traytest.db

import akka.actor.Actor
import akka.actor.Props
import akka.event.Logging

import scala.collection.mutable.HashMap

import com.geekingfrog.traytest.Workflow
import com.geekingfrog.traytest.protocol.{workflowProtocol => WorkflowProtocol}

class WorkflowTable extends Actor {
  private val log = Logging(context.system, this)
  private var store = new HashMap[Int, Workflow]()
  private var currentIndex: Int = 0

  def receive = {
    case "test" => log.info("received test")
    case WorkflowProtocol.Create(numberOfSteps) => {
      log.info(s"creating workflow with id: $currentIndex")
      val idx = currentIndex
      store.put(currentIndex, Workflow(id=idx, numberOfSteps=numberOfSteps))
      log.info(s"$store\n")
      currentIndex += 1
      sender() ! idx
    }
    case WorkflowProtocol.Query(id) => {
      log.info(s"querying store for id $id\n${this.store}\n")
      sender() ! store.get(id)
    }
    case _      => log.info("received unknown message")
  }
}
