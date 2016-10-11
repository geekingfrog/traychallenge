# Engineering challenge

## How to run ?
`sbt run` and that's it. There is also a dockerfile to build the project within a container

## Some thoughts on the project

* Library choices: akka seemed a good fit. That would allow to have the mocked db in memory isolated.
  No need to use the concurrent collections of java since access to an actor is synchronized.
  The typed version of actors is currently experimental so I decided against it, which unfortunately
  means I had to use some ugly `asInstanceOf` and other unsafe casts like that.

* Data modelling: workflows are immutables, so I can only store the remaining number of steps for
  an execution. That allow to manipulate them without having to query the workflow "table", reducing
  contention.
  I also chose to use `Int` for the ids for simplicity. In a real system, a uuid would probably
  be better as it wouldn't require a centralized and synchronized source.

* Concurrency: easy by leveraging akka scheduler to periodically send messages.
