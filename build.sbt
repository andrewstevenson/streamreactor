import Dependencies.globalExcludeDeps
import KafkaVersionAxis.ProjectExtension
import Settings._
import sbt.Keys.libraryDependencies
import sbt._
import sbt.internal.ProjectMatrix.projectMatrixToLocalProjectMatrix
import sbt.internal.{ProjectMatrix, ProjectMatrixReference}

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
  redis
)
lazy val subProjectsRefs: Seq[ProjectMatrixReference] = subProjects.map(projectMatrixToLocalProjectMatrix)

lazy val root = (projectMatrix in file("."))
  .settings(
    publish := {},
    publishArtifact := false,
    name := "stream-reactor"
  )
  .aggregate(
    subProjectsRefs: _*
  )
  .dependsOn(cassandra, elastic6, mongodb, redis)
  .disablePlugins(AssemblyPlugin)
  .settings(
    E2ETest / baseDirectory := (LocalRootProject/baseDirectory).value,
    E2ETest / testOptions := Seq(Tests.Argument(TestFrameworks.JUnit, "-a", "-q", "-v"))
  )
  .kafka2Row()
  .kafka3Row()
  .configureE2ETests()

lazy val common = (projectMatrix in file("kafka-connect-common"))
  .settings(
    settings ++
      Seq(
        name := "kafka-connect-common",
        description := "Kafka Connect compatible connectors to move data between Kafka and popular data stores",
        libraryDependencies ++= baseDeps,
        publish / skip := true,
        dependencyOverrides ++= nettyOverrides
      )
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
        dependencyOverrides ++= nettyOverrides
      )
  )
  .kafka2Row()
  .kafka3Row()
  .configureAssembly()
  .configureTests(kafkaConnectS3TestDeps)

lazy val `azure-documentdb` = (projectMatrix in file("kafka-connect-azure-documentdb"))
  .dependsOn(common)
  .settings(
    settings ++
      Seq(
        name := "kafka-connect-azure-documentdb",
        description := "Kafka Connect compatible connectors to move data between Kafka and popular data stores",
        libraryDependencies ++= baseDeps ++ kafkaConnectAzureDocumentDbDeps,
        publish / skip := true,
        dependencyOverrides ++= nettyOverrides
      )
  )
  .kafka2Row()
  .kafka3Row()
  .configureAssembly()
  .configureTests(baseTestDeps)

lazy val cassandra = (projectMatrix in file("kafka-connect-cassandra"))
  .dependsOn(common)
  .settings(
    settings ++
      Seq(
        name := "kafka-connect-cassandra",
        description := "Kafka Connect compatible connectors to move data between Kafka and popular data stores",
        libraryDependencies ++= baseDeps ++ kafkaConnectCassandraDeps,
        publish / skip := true,
        dependencyOverrides ++= nettyOverrides
      )
  )
  .kafka2Row()
  .kafka3Row()
  .configureAssembly()
  .configureTests(kafkaConnectCassandraTestDeps)

lazy val elastic6 = (projectMatrix in file("kafka-connect-elastic6"))
  .dependsOn(common)
  .settings(
    settings ++
      Seq(
        name := "kafka-connect-elastic6",
        description := "Kafka Connect compatible connectors to move data between Kafka and popular data stores",
        libraryDependencies ++= baseDeps ++ kafkaConnectElastic6Deps,
        publish / skip := true,
        dependencyOverrides ++= nettyOverrides
      )
  )
  .kafka2Row()
  .kafka3Row()
  .configureAssembly()
  .configureTests(kafkaConnectElastic6TestDeps)

lazy val elastic7 = (projectMatrix in file("kafka-connect-elastic7"))
  .dependsOn(common)
  .settings(
    settings ++
      Seq(
        name := "kafka-connect-elastic7",
        description := "Kafka Connect compatible connectors to move data between Kafka and popular data stores",
        libraryDependencies ++= baseDeps ++ kafkaConnectElastic7Deps,
        publish / skip := true,
        dependencyOverrides ++= nettyOverrides
      )
  )
  .kafka2Row()
  .kafka3Row()
  .configureAssembly()
  .configureTests(kafkaConnectElastic7TestDeps)

lazy val hazelcast = (projectMatrix in file("kafka-connect-hazelcast"))
  .dependsOn(common)
  .settings(
    settings ++
      Seq(
        name := "kafka-connect-hazelcast",
        description := "Kafka Connect compatible connectors to move data between Kafka and popular data stores",
        libraryDependencies ++= baseDeps ++ kafkaConnectHazelCastDeps,
        publish / skip := true,
        dependencyOverrides ++= nettyOverrides ++ kafkaConnectHazelCastDeps
        
      )
  )
  .kafka2Row()
  .kafka3Row()
  .configureAssembly()
  .configureTests(baseTestDeps)
  .disableParallel()

lazy val influxdb = (projectMatrix in file("kafka-connect-influxdb"))
  .dependsOn(common)
  .settings(
    settings ++
      Seq(
        name := "kafka-connect-influxdb",
        description := "Kafka Connect compatible connectors to move data between Kafka and popular data stores",
        libraryDependencies ++= baseDeps ++ kafkaConnectInfluxDbDeps,
        publish / skip := true,
        dependencyOverrides ++= nettyOverrides
      )
  )
  .kafka2Row()
  .kafka3Row()
  .configureAssembly()
  .configureTests(baseTestDeps)

lazy val jms = (projectMatrix in file("kafka-connect-jms"))
  .dependsOn(common)
  .settings(
    settings ++
      Seq(
        name := "kafka-connect-jms",
        description := "Kafka Connect compatible connectors to move data between Kafka and popular data stores",
        libraryDependencies ++= baseDeps ++ kafkaConnectJmsDeps,
        publish / skip := true,
        dependencyOverrides ++= nettyOverrides
      )
  )
  .kafka2Row()
  .kafka3Row()
  .configureAssembly()
  .configureTests(kafkaConnectJmsTestDeps)
  .disableParallel()

lazy val kudu = (projectMatrix in file("kafka-connect-kudu"))
  .dependsOn(common)
  .settings(
    settings ++
      Seq(
        name := "kafka-connect-kudu",
        description := "Kafka Connect compatible connectors to move data between Kafka and popular data stores",
        libraryDependencies ++= baseDeps ++ kafkaConnectKuduDeps,
        publish / skip := true,
        dependencyOverrides ++= nettyOverrides
      )
  )
  .kafka2Row()
  .kafka3Row()
  .configureAssembly()
  .configureTests(baseTestDeps)

lazy val mqtt = (projectMatrix in file("kafka-connect-mqtt"))
  .dependsOn(common)
  .settings(
    settings ++
      Seq(
        name := "kafka-connect-mqtt",
        description := "Kafka Connect compatible connectors to move data between Kafka and popular data stores",
        libraryDependencies ++= baseDeps ++ kafkaConnectMqttDeps,
        publish / skip := true,
        dependencyOverrides ++= nettyOverrides
      )
  )
  .kafka2Row()
  .kafka3Row()
  .configureAssembly()
  .configureTests(kafkaConnectMqttTestDeps)
  .disableParallel()

lazy val pulsar = (projectMatrix in file("kafka-connect-pulsar"))
  .dependsOn(common)
  .settings(
    settings ++
      Seq(
        name := "kafka-connect-pulsar",
        description := "Kafka Connect compatible connectors to move data between Kafka and popular data stores",
        libraryDependencies ++= baseDeps ++ kafkaConnectPulsarDeps,
        publish / skip := true,
        dependencyOverrides ++= (nettyOverrides ++ avroOverrides)
      )
  )
  .kafka2Row()
  .kafka3Row()
  .configureAssembly()
  .configureTests(baseTestDeps)

lazy val ftp = (projectMatrix in file("kafka-connect-ftp"))
  .dependsOn(common)
  .settings(
    settings ++
      Seq(
        name := "kafka-connect-ftp",
        description := "Kafka Connect compatible connectors to move data between Kafka and popular data stores",
        libraryDependencies ++= baseDeps ++ kafkaConnectFtpDeps,
        publish / skip := true,
        dependencyOverrides ++= nettyOverrides
      )
  )
  .kafka2Row()
  .kafka3Row()
  .configureAssembly()
  .configureTests(kafkaConnectFtpTestDeps)
  .disableParallel()

lazy val hbase = (projectMatrix in file("kafka-connect-hbase"))
  .dependsOn(common)
  .settings(
    settings ++
      Seq(
        name := "kafka-connect-hbase",
        description := "Kafka Connect compatible connectors to move data between Kafka and popular data stores",
        libraryDependencies ++= baseDeps ++ kafkaConnectHbaseDeps,
        publish / skip := true,
        dependencyOverrides ++= nettyOverrides
      )
  )
  .kafka2Row()
  .kafka3Row()
  .configureAssembly()
  .configureTests(kafkaConnectHbaseTestDeps)

lazy val hive = (projectMatrix in file("kafka-connect-hive"))
  .dependsOn(common)
  .settings(
    settings ++
      Seq(
        name := "kafka-connect-hive",
        description := "Kafka Connect compatible connectors to move data between Kafka and popular data stores",
        libraryDependencies ++= baseDeps ++ kafkaConnectHiveDeps,
        publish / skip := true,
        dependencyOverrides ++= nettyOverrides
      )
  )
  .kafka2Row()
  .kafka3Row()
  .configureAssembly()
  .configureTests(kafkaConnectHiveTestDeps)

lazy val mongodb = (projectMatrix in file("kafka-connect-mongodb"))
  .dependsOn(common)
  .settings(
    settings ++
      Seq(
        name := "kafka-connect-mongodb",
        description := "Kafka Connect compatible connectors to move data between Kafka and popular data stores",
        libraryDependencies ++= baseDeps ++ kafkaConnectMongoDbDeps,
        publish / skip := true,
        dependencyOverrides ++= nettyOverrides
      )
  )
  .kafka2Row()
  .kafka3Row()
  .configureAssembly()
  .configureTests(kafkaConnectMongoDbTestDeps)

lazy val redis = (projectMatrix in file("kafka-connect-redis"))
  .dependsOn(common)
  .settings(
    settings ++
      Seq(
        name := "kafka-connect-redis",
        description := "Kafka Connect compatible connectors to move data between Kafka and popular data stores",
        libraryDependencies ++= baseDeps ++ kafkaConnectRedisDeps,
        publish / skip := true,
        dependencyOverrides ++= nettyOverrides
      )
  )
  .kafka2Row()
  .kafka3Row()
  .configureAssembly()
  .configureTests(kafkaConnectRedisTestDeps)

addCommandAlias(
  "validateAll",
  ";headerCheck;test:headerCheck;fun:headerCheck;it:headerCheck;scalafmtCheck;test:scalafmtCheck;it:scalafmtCheck;fun:scalafmtCheck;e2e:scalafmtCheck"
)
addCommandAlias(
  "formatAll",
  ";headerCreate;test:headerCreate;fun:headerCreate;it:headerCreate;scalafmt;test:scalafmt;it:scalafmt;fun:scalafmt;e2e:scalafmt"
)
addCommandAlias("fullTest", ";test;fun:test;it:test;e2e:test")
addCommandAlias("fullCoverageTest", ";coverage;test;fun:test;it:test;e2e:test;coverageReport;coverageAggregate")

dependencyCheckFormats := Seq("XML", "HTML")
dependencyCheckNodeAnalyzerEnabled := Some(false)
dependencyCheckNodeAuditAnalyzerEnabled := Some(false)
dependencyCheckNPMCPEAnalyzerEnabled := Some(false)
dependencyCheckRetireJSAnalyzerEnabled := Some(false)

excludeDependencies ++= globalExcludeDeps

val generateModulesList = taskKey[Seq[File]]("generateModulesList")

Compile / generateModulesList :=
  new FileWriter(subProjects).generate((Compile / resourceManaged).value / "modules.txt")

Compile / resourceGenerators += (Compile / generateModulesList)

