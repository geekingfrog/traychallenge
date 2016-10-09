package com.geekingfrog.traytest

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json._
import DefaultJsonProtocol._

case class CreateWorkflow(number_of_steps: Int)

trait JsonSupport extends DefaultJsonProtocol with SprayJsonSupport {
  implicit val CreateWorkflowFormat = jsonFormat1(CreateWorkflow)
}
