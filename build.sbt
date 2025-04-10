// See README.md for license details.

ThisBuild / scalaVersion     := "2.13.15"
ThisBuild / version          := "0.1.0"
ThisBuild / organization     := "%ORGANIZATION%"


val chiselVersion = "6.6.0"

lazy val root = (project in file("."))
  .settings(
    name := "SHA256GENERATOR",
    libraryDependencies ++= Seq(
      "org.chipsalliance" %% "chisel" % chiselVersion,
      "org.scalatest" %% "scalatest" % "3.2.16" % Test,
      "edu.berkeley.cs" %% "chiseltest" % "6.0.0" % Test,
      "com.sifive" %% "chisel-circt" % "0.8.0",
      "org.chipsalliance" % "llvm-firtool" % "1.62.1"
    ),
    scalacOptions ++= Seq(
      "-language:reflectiveCalls",
      "-deprecation",
      "-feature",
      "-Xcheckinit",
      "-Ymacro-annotations",
    ),
    addCompilerPlugin("org.chipsalliance" % "chisel-plugin" % chiselVersion cross CrossVersion.full),
  )
