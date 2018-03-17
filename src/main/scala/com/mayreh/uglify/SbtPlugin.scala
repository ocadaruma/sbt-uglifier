package com.mayreh.uglify

import sbt._
import Keys._
import sbt.plugins.JvmPlugin

object SbtPlugin extends AutoPlugin {

  object autoImport {
    val uglify = taskKey[Unit]("uglify all sources in the project.")
  }

  import autoImport._

  override def trigger = allRequirements

  override def requires = JvmPlugin

  override def projectSettings: Seq[Setting[_]] = Seq(
    uglify := {
      val files = (sourceDirectories in Compile).value.descendantsExcept(
        "*.scala",
        (excludeFilter in Compile).value
      ).get.toList

      val sourceContents = files.map(IO.read(_))

      val allInOne = Uglifier.uglify(sourceContents)

      IO.write((scalaSource in Compile).value / "__all__.scala", allInOne)

      files.foreach { file =>
        println(s"delete ${file}")
        file.delete()
      }
    }
  )
}
