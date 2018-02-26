package io.torchbearer.placesservice.Model

import fi.foyt.foursquare.api.entities.CompactVenue

/**
  * Created by fredricvollmer on 1/22/18.
  */
class Place(
             var venue: CompactVenue,
             var relativeBearing: Int
           ) {
  val x = 4
}
