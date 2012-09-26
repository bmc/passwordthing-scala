import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

    val appName         = "passwordthing"
    val appVersion      = "1.0-SNAPSHOT"

    val appDependencies = Seq(
      "org.xerial" % "sqlite-jdbc" % "3.7.2",
      "net.sf.opencsv" % "opencsv" % "2.0"
    )

    val main = PlayProject(appName, appVersion, appDependencies, mainLang = SCALA).settings(
      // Add your own project settings here      
    )

}
