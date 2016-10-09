package com.geekingfrog.traytest.db

import akka.actor.Actor
import akka.actor.Props
import akka.event.Logging

import scala.collection.mutable.HashMap

import com.geekingfrog.traytest.Workflow


class WorkflowTable extends Actor {
  val log = Logging(context.system, this)
  var store = new HashMap[Int, Workflow]()

  def receive = {
    case "test" => log.info("received test")
    case _      => log.info("received unknown message")
  }
}
