package com.landoop.streamreactor.connect.hive.it

import com.dimafeng.testcontainers.ForAllTestContainer
import com.landoop.streamreactor.connect.hive._
import com.landoop.streamreactor.connect.hive.formats.OrcHiveFormat
import com.landoop.streamreactor.connect.hive.sink.HiveSink
import com.landoop.streamreactor.connect.hive.sink.config.{HiveSinkConfig, TableOptions}
import com.landoop.streamreactor.connect.hive.sink.evolution.AddEvolutionPolicy
import com.landoop.streamreactor.connect.hive.sink.partitioning.StrictPartitionHandler
import com.landoop.streamreactor.connect.hive.sink.staging.DefaultCommitPolicy
import org.apache.hadoop.fs.Path
import org.apache.kafka.connect.data.{SchemaBuilder, Struct}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.jdk.CollectionConverters.ListHasAsScala
import scala.util.Try

class HiveOrcSinkTest extends AnyFlatSpec with Matchers with ForAllTestContainer with HiveTestConfig {

  override val container = dockerContainers

  val schema = SchemaBuilder.struct()
    .field("name", SchemaBuilder.string().required().build())
    .field("title", SchemaBuilder.string().optional().build())
    .field("salary", SchemaBuilder.float64().optional().build())
    .build()

  val schemaIntKey = SchemaBuilder.struct()
    .field("department_number", SchemaBuilder.int64().required().build())
    .field("student_name", SchemaBuilder.string().optional().build())
    .build()

  val dbname = "orc_sink_test"

  "hive sink" should "write orc to a non partitioned table" in {

    implicit val (client, fs) = testInit(dbname, container)

    val users = List(
      new Struct(schema).put("name", "sam").put("title", "mr").put("salary", 100.43),
      new Struct(schema).put("name", "laura").put("title", "ms").put("salary", 429.06),
      new Struct(schema).put("name", "tom").put("title", null).put("salary", 395.44)
    )

    Try {
      client.dropTable(dbname, "employees", true, true)
    }

    val config = HiveSinkConfig(DatabaseName(dbname),
      tableOptions = Set(
        TableOptions(TableName("employees"), Topic("mytopic"), true, true, format = OrcHiveFormat)
      ),
      kerberos = None,
      hadoopConfiguration = HadoopConfiguration.Empty
    )

    val sink = HiveSink.from(TableName("employees"), config)
    users.foreach(sink.write(_, TopicPartitionOffset(Topic("mytopic"), 1, Offset(1))))
    sink.close()

    // should be files in the folder now
    fs.listFiles(new Path("hdfs://namenode:8020/user/hive/warehouse/orc_sink_test/employees"), true).hasNext shouldBe true
  }

  it should "write to a partitioned table" in {

    implicit val (client, fs) = testInit(dbname)

    val table = "employees_partitioned"

    Try {
      client.dropTable(dbname, table, true, true)
    }

    val users = List(
      new Struct(schema).put("name", "sam").put("title", "mr").put("salary", 100.43),
      new Struct(schema).put("name", "laura").put("title", "ms").put("salary", 429.06)
    )

    val config = HiveSinkConfig(DatabaseName(dbname),
      tableOptions = Set(
        TableOptions(TableName(table), Topic("mytopic"), true, true, partitions = Seq(PartitionField("title")), format = OrcHiveFormat)
      ),
      kerberos = None,
      hadoopConfiguration = HadoopConfiguration.Empty
    )

    val sink = HiveSink.from(TableName(table), config)
    users.foreach(sink.write(_, TopicPartitionOffset(Topic("mytopic"), 1, Offset(1))))
    sink.close()

    // should be files in the partition folders now
    fs.listFiles(new Path("hdfs://namenode:8020//user/hive/warehouse/orc_sink_test/employees_partitioned/title=mr"), true).hasNext shouldBe true
    fs.listFiles(new Path("hdfs://namenode:8020//user/hive/warehouse/orc_sink_test/employees_partitioned/title=ms"), true).hasNext shouldBe true
  }

  it should "create new partitions in the metastore when using dynamic partitions" in {

    implicit val (client, fs) = testInit(dbname)

    val table = "employees_dynamic_partitions"

    val users = List(
      new Struct(schema).put("name", "sam").put("title", "mr").put("salary", 100.43),
      new Struct(schema).put("name", "laura").put("title", "ms").put("salary", 429.06)
    )

    Try {
      client.dropTable(dbname, table, true, true)
    }

    val config = HiveSinkConfig(DatabaseName(dbname),
      tableOptions = Set(
        TableOptions(TableName(table), Topic("mytopic"), true, true, partitions = Seq(PartitionField("title")), format = OrcHiveFormat)
      ),
      kerberos = None,
      hadoopConfiguration = HadoopConfiguration.Empty
    )

    val sink = HiveSink.from(TableName(table), config)
    users.foreach(sink.write(_, TopicPartitionOffset(Topic("mytopic"), 1, Offset(1))))
    sink.close()

    val partitions = client.listPartitions(dbname, table, Short.MaxValue).asScala

    partitions.exists { partition =>
      partition.getValues.asScala.toList == List("mr")
    } shouldBe true

    partitions.exists { partition =>
      partition.getValues.asScala.toList == List("ms")
    } shouldBe true

    partitions.exists { partition =>
      partition.getValues.asScala.toList == List("other")
    } shouldBe false
  }

  it should "allow setting table type of new tables" in {

    implicit val (client, fs) = testInit(dbname)

    val users = List(new Struct(schema).put("name", "sam").put("title", "mr").put("salary", 100.43))

    val config1 = HiveSinkConfig(DatabaseName(dbname), tableOptions = Set(
      TableOptions(TableName("abc"), Topic("mytopic"), true, true, partitions = Seq(PartitionField("title")), format = OrcHiveFormat)
    ),
      kerberos = None,
      hadoopConfiguration = HadoopConfiguration.Empty
    )

    Try {
      client.dropTable(dbname, "abc", true, true)
    }

    val sink1 = HiveSink.from(TableName("abc"), config1)
    users.foreach(sink1.write(_, TopicPartitionOffset(Topic("mytopic"), 1, Offset(1))))
    sink1.close()

    client.getTable(dbname, "abc").getTableType shouldBe "MANAGED_TABLE"

    val config2 = HiveSinkConfig(DatabaseName(dbname), tableOptions = Set(
      TableOptions(TableName("abc"), Topic("mytopic"), true, true, location = Option("hdfs://namenode:8020/user/hive/warehouse/foo"), format = OrcHiveFormat)
    ),
      kerberos = None,
      hadoopConfiguration = HadoopConfiguration.Empty
    )

    Try {
      client.dropTable(dbname, "abc", true, true)
    }

    val sink2 = HiveSink.from(TableName("abc"), config2)
    users.foreach(sink2.write(_, TopicPartitionOffset(Topic("mytopic"), 1, Offset(1))))
    sink2.close()

    client.getTable(dbname, "abc").getTableType shouldBe "EXTERNAL_TABLE"
  }

  it should "create staging files" in {
    implicit val (client, fs) = testInit(dbname)

    val user1 = new Struct(schema).put("name", "sam").put("title", "mr").put("salary", 100.43)

    val tableName = "commit_test"

    Try {
      client.dropTable(dbname, tableName, true, true)
    }

    val config = HiveSinkConfig(DatabaseName(dbname), tableOptions = Set(
      TableOptions(TableName(tableName), Topic("mytopic"), true, true, format = OrcHiveFormat)
    ),
      kerberos = None,
      hadoopConfiguration = HadoopConfiguration.Empty
    )

    val sink = HiveSink.from(TableName(tableName), config)
    sink.write(user1, TopicPartitionOffset(Topic("mytopic"), 1, Offset(44)))
    fs.exists(new Path(s"hdfs://namenode:8020/user/hive/warehouse/$dbname/$tableName/streamreactor_mytopic_1")) shouldBe false
    sink.close()
  }

  it should "commit files when sink is closed" in {

    implicit val (client, fs) = testInit(dbname)

    val user1 = new Struct(schema).put("name", "sam").put("title", "mr").put("salary", 100.43)

    val tableName = "commit_test"

    Try {
      client.dropTable(dbname, tableName, true, true)
    }

    val config = HiveSinkConfig(DatabaseName(dbname), tableOptions = Set(
      TableOptions(TableName(tableName), Topic("mytopic"), true, true, format = OrcHiveFormat)
    ),
      kerberos = None,
      hadoopConfiguration = HadoopConfiguration.Empty
    )

    val sink = HiveSink.from(TableName(tableName), config)
    for (k <- 1 to 1200) {
      sink.write(user1, TopicPartitionOffset(Topic("mytopic"), 1, Offset(k.toLong)))
    }
    fs.exists(new Path(s"hdfs://namenode:8020/user/hive/warehouse/$dbname/$tableName/.streamreactor_mytopic_1")) shouldBe true

    // once we close the sink, the file will be committed
    sink.write(user1, TopicPartitionOffset(Topic("mytopic"), 1, Offset(2500)))
    sink.close()

    fs.exists(new Path(s"hdfs://namenode:8020/user/hive/warehouse/$dbname/$tableName/.streamreactor_mytopic_1")) shouldBe false
    fs.exists(new Path(s"hdfs://namenode:8020/user/hive/warehouse/$dbname/$tableName/streamreactor_mytopic_1_2500")) shouldBe true
  }

  it should "use file per topic partition" in {

    implicit val (client, fs) = testInit(dbname)

    val user1 = new Struct(schema).put("name", "sam").put("title", "mr").put("salary", 100.43)
    val user2 = new Struct(schema).put("name", "laura").put("title", "ms").put("salary", 417.61)

    val tableName = "stage_per_partition"

    Try {
      client.dropTable(dbname, tableName, true, true)
    }

    val config = HiveSinkConfig(DatabaseName(dbname), tableOptions = Set(
      TableOptions(TableName(tableName), Topic("mytopic"), true, true, format = OrcHiveFormat)
    ),
      kerberos = None,
      hadoopConfiguration = HadoopConfiguration.Empty
    )

    val sink = HiveSink.from(TableName(tableName), config)
    sink.write(user1, TopicPartitionOffset(Topic("mytopic"), 1, Offset(44)))
    sink.write(user2, TopicPartitionOffset(Topic("mytopic"), 4, Offset(45)))
    sink.close()

    fs.exists(new Path(s"hdfs://namenode:8020/user/hive/warehouse/$dbname/$tableName/streamreactor_mytopic_1_44")) shouldBe true
    fs.exists(new Path(s"hdfs://namenode:8020/user/hive/warehouse/$dbname/$tableName/streamreactor_mytopic_4_45")) shouldBe true
  }

  it should "set partition keys in the sd column descriptors" in {

    implicit val (client, fs) = testInit(dbname)

    val users = List(new Struct(schema).put("name", "sam").put("title", "mr").put("salary", 100.43))
    val tableName = "partition_keys_test"
    Try {
      client.dropTable(dbname, tableName, true, true)
    }

    val config = HiveSinkConfig(DatabaseName(dbname), tableOptions = Set(
      TableOptions(TableName(tableName), Topic("mytopic"), true, true, partitions = Seq(PartitionField("title")), format = OrcHiveFormat)
    ),
      kerberos = None,
      hadoopConfiguration = HadoopConfiguration.Empty
    )

    val sink = HiveSink.from(TableName(tableName), config)
    users.foreach(sink.write(_, TopicPartitionOffset(Topic("mytopic"), 1, Offset(1))))
    sink.close()

    val table = client.getTable(dbname, tableName)
    table.getPartitionKeys.asScala.map(_.getName) shouldBe Seq("title")
    table.getSd.getCols.asScala.map(_.getName) shouldBe Seq("name", "salary")
  }

  it should "throw an exception if a partition doesn't exist with strict partitioning" in {

    implicit val (client, fs) = testInit(dbname)

    val users = List(new Struct(schema).put("name", "sam").put("title", "mr").put("salary", 100.43))

    val tableName = "strict_partitioning_test"
    Try {
      client.dropTable(dbname, tableName, true, true)
    }

    val config = HiveSinkConfig(DatabaseName(dbname), tableOptions = Set(
      TableOptions(TableName(tableName), Topic("mytopic"), true, true, partitions = Seq(PartitionField("title")), partitioner = StrictPartitionHandler, format = OrcHiveFormat)
    ),
      kerberos = None,
      hadoopConfiguration = HadoopConfiguration.Empty
    )

    intercept[RuntimeException] {
      val sink = HiveSink.from(TableName(tableName), config)
      users.foreach(sink.write(_, TopicPartitionOffset(Topic("mytopic"), 1, Offset(1))))
      sink.close()
    }.getMessage shouldBe "Partition 'mr' does not exist and strict policy requires upfront creation"
  }

  it should "evolve the schema by adding a missing field when evolution policy is set to add" in {
    implicit val (client, fs) = testInit(dbname)

    val tableName = "add_evolution_test"

    val schema1 = SchemaBuilder.struct()
      .field("a", SchemaBuilder.string().required().build())
      .field("b", SchemaBuilder.string().optional().build())
      .build()

    val list1 = List(new Struct(schema1).put("a", "aaa").put("b", "bbb"))

    val config = HiveSinkConfig(DatabaseName(dbname), tableOptions = Set(
      TableOptions(TableName(tableName), Topic("mytopic"), true, true, evolutionPolicy = AddEvolutionPolicy, format = OrcHiveFormat)
    ),
      kerberos = None,
      hadoopConfiguration = HadoopConfiguration.Empty
    )

    // first we write out one row, with fields a,b and then we write out a second row, with an extra
    // field, and then the schema should have been evolved to add the extra field.

    val sink1 = HiveSink.from(TableName(tableName), config)
    list1.foreach(sink1.write(_, TopicPartitionOffset(Topic("mytopic"), 1, Offset(1))))
    sink1.close()

    client.getTable(dbname, tableName).getSd.getCols.asScala.map(_.getName) shouldBe Seq("a", "b")

    val schema2 = SchemaBuilder.struct()
      .field("a", SchemaBuilder.string().required().build())
      .field("b", SchemaBuilder.string().optional().build())
      .field("x", SchemaBuilder.string().optional().build())
      .build()

    val list2 = List(new Struct(schema2).put("a", "aaaa").put("b", "bbbb").put("x", "xxxx"))

    val sink2 = HiveSink.from(TableName(tableName), config)
    list2.foreach(sink2.write(_, TopicPartitionOffset(Topic("mytopic"), 1, Offset(2))))
    sink2.close()

    client.getTable(dbname, tableName).getSd.getCols.asScala.map(_.getName) shouldBe Seq("a", "b", "x")
  }


  "hive sink" should "write orc with int schema keys" in {
    implicit val (client, fs) = testInit(dbname)

    val students = List(
      new Struct(schemaIntKey).put("department_number", 1L).put("student_name", "Andy"),
      new Struct(schemaIntKey).put("department_number", 1L).put("student_name", "Bob"),
      new Struct(schemaIntKey).put("department_number", 2L).put("student_name", "Charlie")
    )

    Try {
      client.dropTable(dbname, "students", true, true)
    }

    val config = HiveSinkConfig(DatabaseName(dbname),
      tableOptions = Set(
        TableOptions(TableName("students"), Topic("mytopic"), true, true, format = OrcHiveFormat, partitions = Seq(PartitionField("department_number")), commitPolicy = DefaultCommitPolicy(None, None, Some(1)))
      ),
      kerberos = None,
      hadoopConfiguration = HadoopConfiguration.Empty,
    )

    val sink = HiveSink.from(TableName("students"), config)
    students.foreach(sink.write(_, TopicPartitionOffset(Topic("mytopic"), 1, Offset(1))))
    sink.close()

    // should be files in the folder now
    fs.listFiles(new Path("hdfs://namenode:8020/user/hive/warehouse/orc_sink_test/students"), true).hasNext shouldBe true
  }

}
