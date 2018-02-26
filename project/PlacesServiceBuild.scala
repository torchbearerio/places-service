import sbt._
import sbt.Defaults.defaultSettings
import Keys._
import sbtdocker.DockerPlugin.autoImport._
import sbtdocker.DockerKeys.{docker, dockerfile}
import sbtdocker.DockerPlugin
import sbtassembly.AssemblyKeys._
import sbtassembly.AssemblyPlugin._
import sbtassembly.{MergeStrategy, PathList}

object PlacesServiceBuild extends Build {

  // Scala version used
  val buildScalaVersion = "2.11.5"

  // Compiler flag
  final val StandardSettings = defaultSettings ++ Seq(
    scalacOptions ++= Seq("-unchecked", "-deprecation"),
    scalaVersion := buildScalaVersion,
    retrieveManaged in ThisBuild := true
  )

  // The default project
  lazy val placesService = Project(
    id = "places-service",
    base = file("."),
    settings = StandardSettings ++ tsAssemblySettings ++ Seq(
      description := "Places Service",
      libraryDependencies ++= additionalComponents, // See below
      resolvers ++= ExtraResolvers,
      mainClass in(Compile, run) := Some("io.torchbearer.placesservice.PlacesService"),
      dockerSettings
    )
  ).dependsOn(core).enablePlugins(DockerPlugin)


  // ------------------------------------------
  // Docker Builder
  // ------------------------------------------
  val dockerSettings = dockerfile in docker := {
    // The assembly task generates a fat JAR file
    val artifact: File = assembly.value
    val artifactTargetPath = s"/target/${artifact.name}"

    new Dockerfile {
      from("java")
      add("build.jar", artifactTargetPath)
      expose(8080)
      entryPoint("java", "-jar", artifactTargetPath)
    }
  }

  // ------------------------------------------
  // Assembly Plugin
  // ------------------------------------------
  val tsAssemblySettings = assemblySettings ++ Seq(
    assemblyOutputPath in assembly := file("target/build.jar"),
    assemblyJarName in assembly := "build.jar",
    mainClass in assembly := Some("io.torchbearer.placesservice.PlacesService")
  )

  // ------------------------------------------
  // Torchbearer Core Module
  // ------------------------------------------

  lazy val core = ProjectRef(file("../service-core"), "service-core")

  // ------------------------------------------
  // Additional components
  // ------------------------------------------

  // To enable a component remove the //
  val additionalComponents =
  Seq(akka, simplelatlng, foursquare)


  // ------------------------------------------
  // Component versions
  // ------------------------------------------

  // Other components
  lazy val akka = "com.typesafe.akka" %% "akka-actor" % "2.5.0"
  lazy val simplelatlng = "com.javadocmd" % "simplelatlng" % "1.3.1"
  lazy val fb4j = "org.facebook4j" % "facebook4j-core" % "2.4.10"
  lazy val gMaps = "se.walkercrou" % "google-places-api-java" % "2.1.7"
  lazy val foursquare = "me.atlis" % "foursquare-api" % "1.0.6"

  // Additional repos, required by some components
  final val ExtraResolvers = Seq(
    // Similar to Scala-tools.org
    "SonaScalaTools" at "http://oss.sonatype.org/content/groups/scala-tools/"

    // Typesafe repo - needed for Akka, Scalatra
    , "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

    // Snapshots: the bleeding edge
    , "snapshots-repo" at "https://oss.sonatype.org/content/repositories/snapshots/"

    // For geocoder jar at http://jgeocoder.sourceforge.net/
    , "Drexel" at "https://www.cs.drexel.edu/~zl25/maven2/repo"
  )

}
