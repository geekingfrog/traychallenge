import collection.mutable.Stack

import akka.actor.ActorSystem
import akka.actor.{ Actor, ActorRef }
import akka.actor.Props
import akka.testkit.{ TestActors, TestKit, ImplicitSender, TestActor, TestProbe, TestActorRef }
import org.scalatest.WordSpecLike
import org.scalatest.Matchers
import org.scalatest.BeforeAndAfterAll
import akka.pattern.ask

import com.geekingfrog.traytest._
import com.geekingfrog.traytest.db.{WorkflowTable, WorkflowExecutionTable}
import com.geekingfrog.traytest.protocol.{workflowProtocol => WorkflowProtocol}
import com.geekingfrog.traytest.protocol.{workflowExecutionProtocol => WorkflowExecutionProtocol}


import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.{Await, Future, ExecutionContext}

class IntegrationSpec extends TestKit(ActorSystem("IntegrationSpec"))
  with ImplicitSender
  with WordSpecLike
  with Matchers
  with BeforeAndAfterAll
{
  implicit val timeout = Timeout(1 seconds)

  "Create workflow" in {
    val numberOfSteps = 5
    val workflowTable = system.actorOf(Props[WorkflowTable])
    val future = workflowTable ? WorkflowProtocol.Create(numberOfSteps)
    val result = Await.result(future, timeout.duration).asInstanceOf[Int]

    val futureRes = workflowTable ? WorkflowProtocol.Query(result)
    val finalWorkflow = Await.result(futureRes, timeout.duration).asInstanceOf[Option[Workflow]]
    System.out.print(s"final result: $finalWorkflow\n")
    finalWorkflow match {
      case None => fail("Should return created workflow")
      case Some(w) => w.numberOfSteps should be (numberOfSteps)
    }
  }

  "Create execution - no workflow" in {
    val probe = TestProbe()
    val workflowExecutionTable = system.actorOf(Props(classOf[WorkflowExecutionTable], probe.ref))

    val future = workflowExecutionTable ? WorkflowExecutionProtocol.Create(123)
    probe.expectMsg(10 millis, WorkflowProtocol.Query(123))
    probe.reply(None)
    val workflow = Await.result(future, timeout.duration).asInstanceOf[Option[WorkflowExecution]]
    workflow.isEmpty should be (true)
  }

  "Create execution - existing workflow" in {
    val probe = TestProbe()
    val workflowExecutionTable = system.actorOf(Props(classOf[WorkflowExecutionTable], probe.ref))

    val workflowId = 42
    val future = workflowExecutionTable ? WorkflowExecutionProtocol.Create(workflowId)
    probe.expectMsg(10 millis, WorkflowProtocol.Query(workflowId))

    val existingWorkflow = Workflow(id=workflowId, numberOfSteps=5)
    probe.reply(Some(existingWorkflow))
    val workflowExec = Await.result(future, timeout.duration).asInstanceOf[Option[WorkflowExecution]]
    workflowExec match {
      case None => fail("Should return workflow execution")
      case Some(exec) => {
        exec.remainingStep should be (5)
        exec.workflowId should be (workflowId)
      }
    }
  }

  "Missing execution" in {
    val probe = TestProbe()
    val workflowExecutionTable = system.actorOf(Props(classOf[WorkflowExecutionTable], probe.ref))

    val workflowId = 42
    val future = workflowExecutionTable ? WorkflowExecutionProtocol.Exec(42, 314)

    val workflowExec = Await.result(future, timeout.duration).asInstanceOf[Either[WorkflowExecutionProtocol.ExecutionError, Unit]]

    workflowExec match {
      case Right(_) => fail("Should return an error")
      case Left(WorkflowExecutionProtocol.NoExecution) => succeed
      case Left(_) => fail("Returned invalid error")
    }
  }

  "Execution lifecycle" in {
    val probe = TestProbe()
    val workflowExecutionTable = system.actorOf(Props(classOf[WorkflowExecutionTable], probe.ref))

    val workflowId = 42
    val numberOfSteps = 2

    val future = workflowExecutionTable ? WorkflowExecutionProtocol.Create(workflowId)
    probe.expectMsg(10 millis, WorkflowProtocol.Query(workflowId))

    val existingWorkflow = Workflow(id=workflowId, numberOfSteps=numberOfSteps)
    probe.reply(Some(existingWorkflow))
    val workflowExec = Await.result(future, timeout.duration).asInstanceOf[Option[WorkflowExecution]].get

    workflowExec.workflowId should be (workflowId)
    workflowExec.remainingStep should be (numberOfSteps)

    val exec1 = Await.result(
      workflowExecutionTable ? WorkflowExecutionProtocol.Exec(workflowId, workflowExec.id),
      timeout.duration
    ).asInstanceOf[Either[WorkflowExecutionProtocol.ExecutionError, Unit]]

    val exec2 = Await.result(
      workflowExecutionTable ? WorkflowExecutionProtocol.Exec(workflowId, workflowExec.id),
      timeout.duration
    ).asInstanceOf[Either[WorkflowExecutionProtocol.ExecutionError, Unit]]

    val exec3 = Await.result(
      workflowExecutionTable ? WorkflowExecutionProtocol.Exec(workflowId, workflowExec.id),
      timeout.duration
    ).asInstanceOf[Either[WorkflowExecutionProtocol.ExecutionError, Unit]]

    exec1 should be (Right())
    exec2 should be (Right())
    exec3 should be (Left(WorkflowExecutionProtocol.ExecutionFinished))
  }

}
