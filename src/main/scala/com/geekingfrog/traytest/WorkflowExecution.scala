package com.geekingfrog.traytest

import java.time._

final case class WorkflowExecution(id: Int, worflowId: Int, remainingStep: Int, creationDate: OffsetDateTime)
