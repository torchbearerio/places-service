package io.torchbearer.placesservice

import com.javadocmd.simplelatlng.util.LengthUnit
import io.torchbearer.ServiceCore.Orchestration.Task
import io.torchbearer.ServiceCore.DataModel.{ExecutionPoint, Landmark, LandmarkStatus, StreetviewImage}
import io.torchbearer.ServiceCore.AWSServices.S3
import com.javadocmd.simplelatlng.{LatLng, LatLngTool}
import com.amazonaws.services.s3.model.{ObjectMetadata, PutObjectRequest}
import io.torchbearer.ServiceCore.Constants

/**
  * Created by fredricvollmer on 4/13/17.
  */
class PlacesTask(epId: Int, hitId: Int, taskToken: String)
  extends Task(epId = epId, hitId = hitId, taskToken = taskToken) {

  override def run(): Unit = {
    // Load ExecutionPoint
    val ep = ExecutionPoint.getExecutionPoint(epId) getOrElse { return }

    try {
      // In order to computer accurate relative bearing, we need actual coordinate of streetview car
      val image = StreetviewImage.getStreetviewImagesForExecutionPoint(ep.executionPointId, Some(Constants.POSITION_AT)).head

      val places = PlacesAPIService.getPlacesAtPoint(image.latitude, image.longitude, ep.bearing, 100, 120)

      // Map places onto Landmarks, including a relative bearing
      val landmarks = places.map(place => {
        val landmark = Landmark(hitId)
        val category = place.venue.getCategories.find(c => c.getPrimary).map(c => c.getName) getOrElse {
          if (place.venue.getCategories.length > 0) place.venue.getCategories.head.getName else ""
        }
        landmark.status = LandmarkStatus.VERIFIED
        landmark.description = Some(s"${place.venue.getName} $category")
        landmark.semanticSaliencyScore = place.venue.getStats.getCheckinsCount.toDouble
        landmark.relativeBearing = Some(place.relativeBearing)
        landmark.position = Some(Constants.POSITION_AT)
        landmark
      })

      Landmark.insertLandmarks(landmarks)

      this.sendSuccess()

      println(s"Completed Places task for ep $epId and hit $hitId")
    }
    catch {
      case e: Throwable =>
        println(s"Places API error for epi $epId and hit $hitId")
        e.printStackTrace()
        sendFailure("Places API Error", e.getMessage)
    }
  }
}
