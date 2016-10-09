package com.geekingfrog.traytest.protocol.workflowProtocol

sealed trait WorkflowProtocol

case class Create(numberOfSteps: Int) extends WorkflowProtocol
case class Query(id: Int) extends WorkflowProtocol
