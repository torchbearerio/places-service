package io.torchbearer.placesservice

import io.torchbearer.ServiceCore.AWSServices.SFN.getTaskForActivityArn
import io.torchbearer.ServiceCore.{Constants, TorchbearerDB}
import io.torchbearer.placesservice.PlacesAPIService
import org.json4s.jackson.JsonMethods._
import org.json4s.DefaultFormats

import scala.concurrent.{Future, blocking}
import scala.concurrent.ExecutionContext.Implicits.global

object PlacesService extends App {
  implicit val formats = DefaultFormats

  println("Welcome to places-service")

  // Initialize core services
  TorchbearerDB.init()

  //val placesTask = new PlacesTask(246, 56, "sdfsf")
  //placesTask.run()

  while (true) {
    println("Waiting for task...")
    val task = getTaskForActivityArn(Constants.ActivityARNs("DB_DESCRIPTION"))

    // If no tasks were returned, exit
    if (task.getTaskToken != null) {

      val input = parse(task.getInput)
      val epId = (input \ "epId").extract[Int]
      val hitId = (input \ "hitId").extract[Int]
      val taskToken = task.getTaskToken

      val loadTask = new PlacesTask(epId, hitId, taskToken)

      println(s"Starting places task for epId $epId hit $hitId")

      Future {
        blocking {
          loadTask.run()
        }
      }
    }
  }
}
