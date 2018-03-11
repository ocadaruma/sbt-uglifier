package com.mayreh.uglify

import sbt._
import Keys._
import org.apache.ivy.ant.IvyDependency
import sbt.plugins.JvmPlugin

object SbtPlugin extends AutoPlugin {

  object autoImport {
    val uglify = taskKey[Unit]("uglify all sources in the project.")
  }

  import autoImport._

  override def trigger = allRequirements

  override def requires = JvmPlugin

  override def projectSettings: Seq[Setting[_]] = Seq(
//    uglify := {
//      val files = (sourceDirectories in Compile).value.descendantsExcept(
//        "*.scala",
//        (excludeFilter in Compile).value
//      ).get.toList
//
//      val sourceContents = files.map(IO.read(_))
//
//      val allInOne = Uglifier.uglify(sourceContents)
//
//      IO.write((scalaSource in Compile).value / "__all__.scala", allInOne)
//
//      files.foreach { file =>
//        println(s"delete ${file}")
//        file.delete()
//      }
//    }

    uglify := {
//      val files = (sourceDirectories in Compile).value.descendantsExcept(
//        "*.scala",
//        (excludeFilter in Compile).value
//      ).get.toList.take(63)
//
//      libraryDependencies.value.map { v =>
//        v.extraDependencyAttributes
//      }
//
//      val sourceContents = files.map(IO.read(_))
////
////      files.foreach { file =>
////        val content = IO.read(file)
////        val uglified = Uglifier.uglify(content)
////        IO.write(file, uglified)
////      }
//
//      val allInOne = Uglifier.uglify(sourceContents)
//
//      IO.write((scalaSource in Compile).value / "__all__.scala", allInOne)
//
//      files.foreach { file =>
//        println(s"delete ${file}")
//        file.delete()
//      }

      val report = update.value
      val thisModule = organization.value %% name.value % version.value
      def moduleKey = (m: ModuleID) => {
        val scalaSuffix = s"_${scalaBinaryVersion.value}"
        (m.organization, m.name.replace(scalaSuffix, ""), m.revision)
      }
//      println("~~~~~~~~~~~")
//      println("~~~~~~~~~~~")

      val homepages = report.configurations.flatMap { c =>
        println("??????????")
        println(c.modules.map(_.callers))
        println("??????????")
        c.modules
      }.collect {
        case m if m.callers.map(c => moduleKey(c.caller)).contains(moduleKey(thisModule)) =>
//          println(m.artifacts)
          println(m.artifacts.headOption.map(_._1))
          m.homepage
      }.flatten.distinct
      println("^^^^^^^^^^^^^^^^^^^^^^^")
      println(homepages.mkString("\n"))



//      update.value.configurations.map { c =>
//        println("************************")
//        println(c.details.map(_.modules.head.callers))
//        println("************************")
//      }

//      libraryDependencies.value.map { v =>
//        println("================================")
//        println(v.pomOnly().extraAttributes)
//        println(v.pomOnly().explicitArtifacts)
//        println(v.pomOnly().configurations)
//        println(v)
//        println("================================")
//      }
    }
  )
}
