package com.geekingfrog.traytest

case class CreateWorkflow(number_of_steps: Int)

case class WorkflowCreated(workflow_id: String)

case class WorkflowExecutionCreated(workflow_execution_id: String)

case class WorkflowQuery(finished: Boolean)
