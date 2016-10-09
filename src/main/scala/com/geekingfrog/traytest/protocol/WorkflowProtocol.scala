package com.geekingfrog.traytest.protocol.workflowProtocol

sealed trait WorkflowProtocol

case class Create(numberOfSteps: Int) extends WorkflowProtocol
