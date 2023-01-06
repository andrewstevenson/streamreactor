import Dependencies.globalExcludeDeps
import Dependencies.gson
import KafkaVersionAxis.ProjectExtension
import Settings._
import sbt.Keys.libraryDependencies
import sbt._
import sbt.internal.ProjectMatrix.projectMatrixToLocalProjectMatrix
import sbt.internal.ProjectMatrix
import sbt.internal.ProjectMatrixReference

import java.io.File

ThisBuild / scalaVersion := "2.13.8"

javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint")

lazy val subProjects: Seq[ProjectMatrix] = Seq(
  common,
  `aws-s3`,
  `azure-documentdb`,
  cassandra,
  elastic6,
  elastic7,
  ftp,
  hazelcast,
  hbase,
  hive,
  influxdb,
  jms,
  kudu,
  mongodb,
  mqtt,
  pulsar,
  redis,
)

lazy val subProjectsRefs: Seq[ProjectMatrixReference] = subProjects.map(projectMatrixToLocalProjectMatrix)

lazy val root = (projectMatrix in file("."))
  .settings(
    publish := {},
    publishArtifact := false,
    name := "stream-reactor",
    headerLicense := None,
  )
  .aggregate(
    subProjectsRefs: _*,
  )
  .disablePlugins(AssemblyPlugin)

lazy val common = (projectMatrix in file("kafka-connect-common"))
  .settings(
    settings ++
      Seq(
        name := "kafka-connect-common",
        description := "Kafka Connect compatible connectors to move data between Kafka and popular data stores",
        libraryDependencies ++= baseDeps,
        publish / skip := true,
        dependencyOverrides ++= nettyOverrides,
      ),
  )
  .kafka2Row()
  .kafka3Row()
  .configureAssembly()
  .configureTests(baseTestDeps)

lazy val `aws-s3` = (projectMatrix in file("kafka-connect-aws-s3"))
  .dependsOn(common)
  .settings(
    settings ++
      Seq(
        name := "kafka-connect-aws-s3",
        description := "Kafka Connect compatible connectors to move data between Kafka and popular data stores",
        libraryDependencies ++= baseDeps ++ kafkaConnectS3Deps,
        publish / skip := true,
        dependencyOverrides ++= nettyOverrides,
        packExcludeJars := Seq(
          "scala-.*\\.jar",
          "zookeeper-.*\\.jar",
        ),
      ),
  )
  .kafka2Row()
  .kafka3Row()
  .configureAssembly()
  .configureTests(baseTestDeps)
  .configureIntegrationTests(kafkaConnectS3TestDeps)
  .enablePlugins(PackPlugin)

lazy val `azure-documentdb` = (projectMatrix in file("kafka-connect-azure-documentdb"))
  .dependsOn(common)
  .settings(
    settings ++
      Seq(
        name := "kafka-connect-azure-documentdb",
        description := "Kafka Connect compatible connectors to move data between Kafka and popular data stores",
        libraryDependencies ++= baseDeps ++ kafkaConnectAzureDocumentDbDeps,
        publish / skip := true,
        dependencyOverrides ++= nettyOverrides,
        packExcludeJars := Seq(
          "scala-.*\\.jar",
          "zookeeper-.*\\.jar",
        ),
      ),
  )
  .kafka2Row()
  .kafka3Row()
  .configureAssembly()
  .configureTests(baseTestDeps)
  .enablePlugins(PackPlugin)

lazy val cassandra = (projectMatrix in file("kafka-connect-cassandra"))
  .dependsOn(common)
  .dependsOn(`test-common` % "fun->compile")
  .settings(
    settings ++
      Seq(
        name := "kafka-connect-cassandra",
        description := "Kafka Connect compatible connectors to move data between Kafka and popular data stores",
        libraryDependencies ++= baseDeps ++ kafkaConnectCassandraDeps,
        publish / skip := true,
        dependencyOverrides ++= nettyOverrides,
        FunctionalTest / baseDirectory := (LocalRootProject / baseDirectory).value,
        packExcludeJars := Seq(
          "scala-.*\\.jar",
          "zookeeper-.*\\.jar",
        ),
      ),
  )
  .kafka2Row()
  .kafka3Row()
  .configureAssembly()
  .configureTests(baseTestDeps)
  .configureIntegrationTests(kafkaConnectCassandraTestDeps)
  .configureFunctionalTests()
  .enablePlugins(PackPlugin)

lazy val elastic6 = (projectMatrix in file("kafka-connect-elastic6"))
  .dependsOn(common)
  .dependsOn(`test-common` % "fun->compile")
  .settings(
    settings ++
      Seq(
        name := "kafka-connect-elastic6",
        description := "Kafka Connect compatible connectors to move data between Kafka and popular data stores",
        libraryDependencies ++= baseDeps ++ kafkaConnectElastic6Deps,
        publish / skip := true,
        dependencyOverrides ++= nettyOverrides,
        FunctionalTest / baseDirectory := (LocalRootProject / baseDirectory).value,
        packExcludeJars := Seq(
          "scala-.*\\.jar",
          "zookeeper-.*\\.jar",
        ),
      ),
  )
  .kafka2Row()
  .kafka3Row()
  .configureAssembly()
  .configureTests(baseTestDeps)
  .configureIntegrationTests(kafkaConnectElastic6TestDeps)
  .configureFunctionalTests()
  .enablePlugins(PackPlugin)

lazy val elastic7 = (projectMatrix in file("kafka-connect-elastic7"))
  .dependsOn(common)
  .dependsOn(`test-common` % "fun->compile")
  .settings(
    settings ++
      Seq(
        name := "kafka-connect-elastic7",
        description := "Kafka Connect compatible connectors to move data between Kafka and popular data stores",
        libraryDependencies ++= baseDeps ++ kafkaConnectElastic7Deps,
        publish / skip := true,
        dependencyOverrides ++= nettyOverrides,
        packExcludeJars := Seq(
          "scala-.*\\.jar",
          "zookeeper-.*\\.jar",
        ),
      ),
  )
  .kafka2Row()
  .kafka3Row()
  .configureAssembly()
  .configureTests(baseTestDeps)
  .configureIntegrationTests(kafkaConnectElastic7TestDeps)
  .configureFunctionalTests()
  .enablePlugins(PackPlugin)

lazy val hazelcast = (projectMatrix in file("kafka-connect-hazelcast"))
  .dependsOn(common)
  .settings(
    settings ++
      Seq(
        name := "kafka-connect-hazelcast",
        description := "Kafka Connect compatible connectors to move data between Kafka and popular data stores",
        libraryDependencies ++= baseDeps ++ kafkaConnectHazelCastDeps,
        publish / skip := true,
        dependencyOverrides ++= nettyOverrides ++ kafkaConnectHazelCastDeps,
        packExcludeJars := Seq(
          "scala-.*\\.jar",
          "zookeeper-.*\\.jar",
        ),
      ),
  )
  .kafka2Row()
  .kafka3Row()
  .configureAssembly()
  .configureTests(baseTestDeps)
  .disableParallel()
  .enablePlugins(PackPlugin)

lazy val influxdb = (projectMatrix in file("kafka-connect-influxdb"))
  .dependsOn(common)
  .settings(
    settings ++
      Seq(
        name := "kafka-connect-influxdb",
        description := "Kafka Connect compatible connectors to move data between Kafka and popular data stores",
        libraryDependencies ++= baseDeps ++ kafkaConnectInfluxDbDeps,
        publish / skip := true,
        dependencyOverrides ++= nettyOverrides,
        packExcludeJars := Seq(
          "scala-.*\\.jar",
          "zookeeper-.*\\.jar",
        ),
      ),
  )
  .kafka2Row()
  .kafka3Row()
  .configureAssembly()
  .configureTests(baseTestDeps)
  .enablePlugins(PackPlugin)

lazy val jms = (projectMatrix in file("kafka-connect-jms"))
  .dependsOn(common)
  .settings(
    settings ++
      Seq(
        name := "kafka-connect-jms",
        description := "Kafka Connect compatible connectors to move data between Kafka and popular data stores",
        libraryDependencies ++= baseDeps ++ kafkaConnectJmsDeps,
        publish / skip := true,
        dependencyOverrides ++= nettyOverrides,
        packExcludeJars := Seq(
          "scala-.*\\.jar",
          "zookeeper-.*\\.jar",
        ),
      ),
  )
  .configureProtobufSources()
  .kafka2Row()
  .kafka3Row()
  .configureAssembly()
  .configureTests(kafkaConnectJmsTestDeps)
  .configureIntegrationTests(kafkaConnectJmsTestDeps)
  .disableParallel()
  .enablePlugins(PackPlugin)

lazy val kudu = (projectMatrix in file("kafka-connect-kudu"))
  .dependsOn(common)
  .settings(
    settings ++
      Seq(
        name := "kafka-connect-kudu",
        description := "Kafka Connect compatible connectors to move data between Kafka and popular data stores",
        libraryDependencies ++= baseDeps ++ kafkaConnectKuduDeps,
        publish / skip := true,
        dependencyOverrides ++= nettyOverrides,
        packExcludeJars := Seq(
          "scala-.*\\.jar",
          "zookeeper-.*\\.jar",
        ),
      ),
  )
  .kafka2Row()
  .kafka3Row()
  .configureAssembly()
  .configureTests(baseTestDeps)
  .enablePlugins(PackPlugin)

lazy val mqtt = (projectMatrix in file("kafka-connect-mqtt"))
  .dependsOn(common)
  .settings(
    settings ++
      Seq(
        name := "kafka-connect-mqtt",
        description := "Kafka Connect compatible connectors to move data between Kafka and popular data stores",
        libraryDependencies ++= baseDeps ++ kafkaConnectMqttDeps,
        publish / skip := true,
        dependencyOverrides ++= nettyOverrides,
        packExcludeJars := Seq(
          "scala-.*\\.jar",
          "zookeeper-.*\\.jar",
        ),
      ),
  )
  .kafka2Row()
  .kafka3Row()
  .configureAssembly()
  .configureTests(baseTestDeps)
  .configureIntegrationTests(kafkaConnectMqttTestDeps)
  .disableParallel()
  .enablePlugins(PackPlugin)

lazy val pulsar = (projectMatrix in file("kafka-connect-pulsar"))
  .dependsOn(common)
  .settings(
    settings ++
      Seq(
        name := "kafka-connect-pulsar",
        description := "Kafka Connect compatible connectors to move data between Kafka and popular data stores",
        libraryDependencies ++= baseDeps ++ kafkaConnectPulsarDeps,
        publish / skip := true,
        dependencyOverrides ++= (nettyOverrides ++ avroOverrides),
        packExcludeJars := Seq(
          "scala-.*\\.jar",
          "zookeeper-.*\\.jar",
        ),
      ),
  )
  .kafka2Row()
  .kafka3Row()
  .configureAssembly()
  .configureTests(baseTestDeps)
  .enablePlugins(PackPlugin)

lazy val ftp = (projectMatrix in file("kafka-connect-ftp"))
  .dependsOn(common)
  .settings(
    settings ++
      Seq(
        name := "kafka-connect-ftp",
        description := "Kafka Connect compatible connectors to move data between Kafka and popular data stores",
        libraryDependencies ++= baseDeps ++ kafkaConnectFtpDeps,
        publish / skip := true,
        dependencyOverrides ++= nettyOverrides,
        packExcludeJars := Seq(
          "scala-.*\\.jar",
          "zookeeper-.*\\.jar",
        ),
      ),
  )
  .kafka2Row()
  .kafka3Row()
  .configureAssembly()
  .configureTests(baseTestDeps)
  .configureIntegrationTests(kafkaConnectFtpTestDeps)
  .enablePlugins(PackPlugin)

lazy val hbase = (projectMatrix in file("kafka-connect-hbase"))
  .dependsOn(common)
  .settings(
    settings ++
      Seq(
        name := "kafka-connect-hbase",
        description := "Kafka Connect compatible connectors to move data between Kafka and popular data stores",
        libraryDependencies ++= baseDeps ++ kafkaConnectHbaseDeps,
        publish / skip := true,
        dependencyOverrides ++= nettyOverrides,
        packExcludeJars := Seq(
          "scala-.*\\.jar",
          "zookeeper-.*\\.jar",
        ),
      ),
  )
  .kafka2Row()
  .kafka3Row()
  .configureAssembly()
  .configureTests(baseTestDeps)
  .enablePlugins(PackPlugin)

lazy val hive = (projectMatrix in file("kafka-connect-hive"))
  .dependsOn(common)
  .settings(
    settings ++
      Seq(
        name := "kafka-connect-hive",
        description := "Kafka Connect compatible connectors to move data between Kafka and popular data stores",
        libraryDependencies ++= baseDeps ++ kafkaConnectHiveDeps,
        publish / skip := true,
        dependencyOverrides ++= nettyOverrides,
        packExcludeJars := Seq(
          "scala-.*\\.jar",
          "zookeeper-.*\\.jar",
        ),
      ),
  )
  .kafka2Row()
  .kafka3Row()
  .configureAssembly()
  .configureTests(kafkaConnectHiveTestDeps)
  .enablePlugins(PackPlugin)

lazy val mongodb = (projectMatrix in file("kafka-connect-mongodb"))
  .dependsOn(common)
  .dependsOn(`test-common` % "test->compile;it->compile;fun->compile")
  .settings(
    settings ++
      Seq(
        name := "kafka-connect-mongodb",
        description := "Kafka Connect compatible connectors to move data between Kafka and popular data stores",
        libraryDependencies ++= baseDeps ++ kafkaConnectMongoDbDeps,
        publish / skip := true,
        dependencyOverrides ++= nettyOverrides,
        FunctionalTest / baseDirectory := (LocalRootProject / baseDirectory).value,
        packExcludeJars := Seq(
          "scala-.*\\.jar",
          "zookeeper-.*\\.jar",
        ),
      ),
  )
  .kafka2Row()
  .kafka3Row()
  .configureAssembly()
  .configureTests(baseTestDeps)
  .configureIntegrationTests(kafkaConnectMongoDbTestDeps)
  .configureFunctionalTests()
  .enablePlugins(PackPlugin)

lazy val redis = (projectMatrix in file("kafka-connect-redis"))
  .dependsOn(common)
  .dependsOn(`test-common` % "fun->compile")
  .settings(
    settings ++
      Seq(
        name := "kafka-connect-redis",
        description := "Kafka Connect compatible connectors to move data between Kafka and popular data stores",
        libraryDependencies ++= baseDeps ++ kafkaConnectRedisDeps,
        publish / skip := true,
        dependencyOverrides ++= nettyOverrides,
        FunctionalTest / baseDirectory := (LocalRootProject / baseDirectory).value,
        packExcludeJars := Seq(
          "scala-.*\\.jar",
          "zookeeper-.*\\.jar",
        ),
      ),
  )
  .kafka2Row()
  .kafka3Row()
  .configureAssembly()
  .configureTests(baseTestDeps ++ Seq(gson))
  .configureIntegrationTests(kafkaConnectRedisTestDeps)
  .configureFunctionalTests()
  .enablePlugins(PackPlugin)

lazy val `test-common` = (projectMatrix in file("test-common"))
  .settings(
    settings ++
      Seq(
        name := "test-common",
        libraryDependencies ++= baseTestDeps ++ testCommonDeps,
      ),
  )
  .kafka2Row()
  .kafka3Row()
  .disablePlugins(AssemblyPlugin)

addCommandAlias(
  "validateAll",
  ";scalafmtCheck;test:scalafmtCheck;it:scalafmtCheck;fun:scalafmtCheck;e2e:scalafmtCheck",
)
addCommandAlias(
  "formatAll",
  ";scalafmt;test:scalafmt;it:scalafmt;fun:scalafmt",
)
addCommandAlias("fullTest", ";test;it:test;fun:test")
addCommandAlias("fullCoverageTest", ";coverage;test;it:test;coverageReport;coverageAggregate")

dependencyCheckFormats := Seq("XML", "HTML")
dependencyCheckNodeAnalyzerEnabled := Some(false)
dependencyCheckNodeAuditAnalyzerEnabled := Some(false)
dependencyCheckNPMCPEAnalyzerEnabled := Some(false)
dependencyCheckRetireJSAnalyzerEnabled := Some(false)

excludeDependencies ++= globalExcludeDeps

val generateModulesList    = taskKey[Seq[File]]("generateModulesList")
val generateItModulesList  = taskKey[Seq[File]]("generateItModulesList")
val generateFunModulesList = taskKey[Seq[File]]("generateFunModulesList")

Compile / generateModulesList :=
  new FileWriter(subProjects).generate((Compile / resourceManaged).value / "modules.txt")
Compile / generateItModulesList :=
  new FileWriter(
    subProjects.filter(p => p.containsDir("src/it")),
  ).generate((Compile / resourceManaged).value / "it-modules.txt")
Compile / generateFunModulesList :=
  new FileWriter(
    subProjects.filter(p => p.containsDir("src/fun")),
  ).generate((Compile / resourceManaged).value / "fun-modules.txt")

Compile / resourceGenerators += (Compile / generateModulesList)
Compile / resourceGenerators += (Compile / generateItModulesList)
Compile / resourceGenerators += (Compile / generateFunModulesList)
