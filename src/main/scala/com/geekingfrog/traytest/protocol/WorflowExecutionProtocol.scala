package com.geekingfrog.traytest.protocol.workflowExecutionProtocol

sealed trait WorkflowExecutionProtocol

case class Create(workflowId: Int) extends WorkflowExecutionProtocol
case class Exec(workflowId: Int, worfklowExecutionId: Int) extends WorkflowExecutionProtocol
case class Query(workflowId: Int, worfklowExecutionId: Int) extends WorkflowExecutionProtocol

sealed trait ExecutionError

case object NoExecution extends ExecutionError
case object ExecutionFinished extends ExecutionError
