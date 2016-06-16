

val amm = taskKey[Unit]("Run the Ammonite REPL")

val ammSettings = Seq(
  libraryDependencies += "com.lihaoyi" % "ammonite-repl" % "0.6.0" % "test" cross CrossVersion.full,
  amm := toError((runner in run).value.run(
      "ammonite.repl.Main", 
      Attributed.data((fullClasspath in Test).value), {
        val opts = scalacOptions.value.mkString("List(\"", "\",\"", "\")")
        List("-p", s"compiler.settings.processArguments($opts, true)\n${initialCommands.value}")
      },
      streams.value.log
  ))
)

lazy val buildSettings = Seq(
  organization := "org.tpolecat",
  licenses ++= Seq(("MIT", url("http://opensource.org/licenses/MIT"))),
  scalaVersion := "2.11.8",
  libraryDependencies ++= macroParadise(scalaVersion.value) ++ Seq(
    "org.scalacheck" %% "scalacheck"  % "1.11.5" % "test",
    "org.specs2"     %% "specs2-core" % "3.6.5"  % "test"
  )
  // no cross version yet ... 2.10 doesn't quite work, no 2.12 for doobie 0.2.x
)

def macroParadise(v: String): List[ModuleID] =
  if (v.startsWith("2.10")) List(compilerPlugin("org.scalamacros" % "paradise" % "2.0.1" cross CrossVersion.full))
  else Nil

lazy val commonSettings = Seq(
    scalacOptions ++= Seq(
      "-encoding", "UTF-8", // 2 args
      "-feature",
      "-deprecation",
      "-language:existentials",
      "-language:higherKinds",
      "-language:implicitConversions",
      "-language:experimental.macros",
      "-unchecked",
      "-Xlint",
      "-Yno-adapted-args",
      "-Ywarn-dead-code"
      // "-Ywarn-value-discard"
    ),
    addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.7.1")
) ++ ammSettings

lazy val root = project.in(file("."))
  .settings(buildSettings)
  .aggregate(core, ammonite, postgres, h2)

lazy val core = project.in(file("core"))
  .settings(buildSettings)
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-reflect"  % scalaVersion.value,
      "org.scala-lang" % "scala-compiler" % scalaVersion.value,
      "org.typelevel" %% "macro-compat"   % "1.1.1",
      "org.tpolecat"  %% "doobie-core"    % "0.2.3"
    ),
    addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)
  )

lazy val ammonite = project.in(file("contrib/ammonite"))
  .settings(buildSettings)
  .settings(commonSettings)
  .settings(
    libraryDependencies += "com.lihaoyi" % "ammonite-repl" % "0.6.0" cross CrossVersion.full
  )
  .dependsOn(core)

lazy val postgres = project.in(file("contrib/postgres"))
  .settings(buildSettings)
  .settings(commonSettings)
  .settings(
    scalacOptions ++= Seq(
      "-Xmacro-settings:doobie.driver=org.postgresql.Driver",
      "-Xmacro-settings:doobie.connect=jdbc:postgresql:world",
      "-Xmacro-settings:doobie.user=postgres",
      "-Xmacro-settings:doobie.password="
    ),
    libraryDependencies ++= Seq(
      "org.tpolecat"  %% "doobie-contrib-postgresql" % "0.2.3"
    ),
    initialCommands := """
      |import scalaz._,Scalaz._
      |import scalaz.concurrent.Task
      |import doobie.imports._
      |import doobie.contrib.postgresql.pgtypes._
      |val xa: Transactor[Task] = DriverManagerTransactor[Task]("org.postgresql.Driver", "jdbc:postgresql:world", "postgres", "")
      |import xa.yolo._
      |import org.postgis._
      |import org.postgresql.util._
      |import org.postgresql.geometric._
      |import tsql._
      |import shapeless._
      """.stripMargin
  )
  .dependsOn(core, ammonite)


lazy val h2 = project.in(file("contrib/h2"))
  .settings(buildSettings)
  .settings(commonSettings)
  .settings(
    scalacOptions ++= Seq(
      "-Xmacro-settings:doobie.driver=org.postgresql.Driver",
      "-Xmacro-settings:doobie.connect=jdbc:postgresql:world",
      "-Xmacro-settings:doobie.user=postgres",
      "-Xmacro-settings:doobie.password="
    ),
    libraryDependencies ++= Seq(
      "org.tpolecat"  %% "doobie-contrib-postgresql" % "0.2.3"
    ),
    initialCommands := s"""
      |import tsql._, tsql.amm._, scalaz._, Scalaz._, doobie.imports._, shapeless._
      |""".stripMargin
  )
  .dependsOn(core, ammonite)



