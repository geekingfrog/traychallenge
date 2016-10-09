package com.geekingfrog.traytest

import com.geekingfrog.traytest._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json._
import DefaultJsonProtocol._

trait JsonSupport extends DefaultJsonProtocol with SprayJsonSupport {
  implicit val CreateWorkflowFormat = jsonFormat1(CreateWorkflow)
  implicit val WorkflowCreatedFormat = jsonFormat1(WorkflowCreated)
  implicit val WorkflowExecutionCreatedFormat = jsonFormat1(WorkflowExecutionCreated)
  implicit val WorkflowQueryFormat = jsonFormat1(WorkflowQuery)
}
