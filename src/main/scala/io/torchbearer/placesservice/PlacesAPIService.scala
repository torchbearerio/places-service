package io.torchbearer.placesservice

import java.net.URL

import fi.foyt.foursquare.api.FoursquareApi
import io.torchbearer.ServiceCore.AWSServices.KeyStore.getKey
import io.torchbearer.ServiceCore.CartographyUtils
import io.torchbearer.placesservice.Model.Place

import scala.collection.JavaConversions._

/**
  * Created by fredricvollmer on 4/14/17.
  */
object PlacesAPIService {
  lazy val foursquare = new FoursquareApi(getKey("foursquare-client-id"), getKey("foursquare-client-secret"), "")

  /*
  private val facebook = new FacebookFactory().getInstance
  facebook.setOAuthAppId(getKey("fb-app-id"), getKey("fb-app-secret"))
  private val token = facebook.getOAuthAppAccessToken
  facebook.setOAuthAccessToken(token)
  */

  //getPlacesAtPoint(45.693132, -111.062434, 0, 100, 120)

  /**
    * Returns a list of Places within <radius> meters of point, within a <fov> field of view centered at bearing.
    *
    * @param lat
    * @param long
    * @param fov
    */
  def getPlacesAtPoint(lat: Double, long: Double, bearing: Int, radius: Int, fov: Int): List[Place] = {
    val latlong = s"$lat,$long"
    val params = Map(
      "ll" -> latlong,
      "intent" -> "browse",
      "radius" -> radius.toString,
      "limit" -> "50"
    )
    val results = foursquare.venuesSearch(params)
    if (results.getMeta.getCode != 200) { // if query was ok we can finally we do something with the data
      // An error ocurred fetching from foursquare
      throw new Exception(s"Unable to fetch places from FourSquare: ${results.getMeta.getCode}, ${results.getMeta.getErrorType}, ${results.getMeta.getErrorDetail}")
    }

    results.getResult.getVenues
      // Relative bearing is calculated w/ respect to due north, i.e. the compass bearing between two points.
      // We subtract `ep.bearing` from this number to get a relative bearing w/ respect to `ep.bearing`
      .map(v => new Place(v, CartographyUtils.relativeBearing(lat, long, v.getLocation.getLat, v.getLocation.getLng) - bearing))
      .filter(p => (0 to fov / 2 contains p.relativeBearing) || (360 - fov / 2 to 360 contains p.relativeBearing))
      .toList
  }
}

// Old facebook search
/*
val center = new GeoLocation(lat, long)
val reading = new Reading().fields("name,location,checkins,category")
val places = facebook.searchPlaces("", center, radius, reading).toArray(Array[Place]())
*/

// Old google search
/*
val places = google.getNearbyPlaces(lat, long, radius, Param.name("rankby").value("prominence"))
val relativeBearings = places.map(place => {
val placeLat = place.getLatitude
val placeLong = place.getLongitude
CartographyUtils.bearing(placeLat, placeLong, lat, long)
})
*/
