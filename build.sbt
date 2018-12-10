name := "sql-hackathon"

version := "0.1"

scalaVersion := "2.11.12"

val flinkVersion = "1.7.0"

val flinkDependencies = Seq(
  "org.apache.flink" %% "flink-scala" % flinkVersion % "provided",
  "org.apache.flink" %% "flink-streaming-scala" % flinkVersion % "provided",
  "org.apache.flink" %% "flink-streaming-java" % flinkVersion % "provided",
  "org.apache.flink" %% "flink-table" % flinkVersion % "provided",
  "org.apache.flink" %% "flink-cep" % flinkVersion % "provided"
)

libraryDependencies ++= flinkDependencies