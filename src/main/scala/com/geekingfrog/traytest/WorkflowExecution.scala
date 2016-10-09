package com.geekingfrog.traytest

import java.time._

final case class WorkflowExecution(id: Int, workflowId: Int, remainingStep: Int, creationDate: OffsetDateTime)
