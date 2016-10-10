import collection.mutable.Stack

import akka.actor.ActorSystem
import akka.actor.Actor
import akka.actor.Props
import akka.testkit.{ TestActors, TestKit, ImplicitSender }
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
  // val workflowExecutionTable = system.actorOf(Props[WorkflowExecutionTable], "worflowExecutionTableActor")
  // val workflowTable = system.actorOf(Props[WorkflowTable], "worflowTableActor")

  override def afterAll {
    System.out.print("afterAll \n")
  }

  override def beforeAll {
    System.out.print("before all here \n")
  }

  // "Create workflow" in {
  //   val numberOfSteps = 5
  //   // val workflowTable = system.actorOf(Props[WorkflowTable], "worflowTableActor")
  //   val future = workflowTable ? WorkflowProtocol.Create(numberOfSteps)
  //   val result = Await.result(future, timeout.duration).asInstanceOf[Int]
  //
  //   val futureRes = workflowTable ? WorkflowProtocol.Query(result)
  //   val finalWorkflow = Await.result(futureRes, timeout.duration).asInstanceOf[Option[Workflow]]
  //   System.out.print(s"final result: $finalWorkflow\n")
  //   finalWorkflow match {
  //     case None => fail("Should return created workflow")
  //     case Some(w) => w.numberOfSteps should be (numberOfSteps)
  //   }
  // }

  "Create execution" in {
    val workflowExecutionTable = system.actorOf(Props[WorkflowExecutionTable], "worflowExecutionTableActor")
    val workflowTable = system.actorOf(Props[WorkflowTable], "worflowTableActor")
    val future = workflowExecutionTable ? WorkflowExecutionProtocol.Create(123)
    val workflow = Await.result(future, timeout.duration).asInstanceOf[Option[WorkflowExecution]]
    workflow.isEmpty should be (true)
  }

}
