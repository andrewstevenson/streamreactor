package com.landoop.streamreactor.connect.hive.sink.mapper

import com.landoop.streamreactor.connect.hive.PartitionPlan
import com.landoop.streamreactor.connect.hive.StructMapper
import org.apache.kafka.connect.data.SchemaBuilder
import org.apache.kafka.connect.data.Struct

import scala.jdk.CollectionConverters.ListHasAsScala

/**
  * compile of [[StructMapper]] that will strip partition values
  * from an input [[Struct]] using a supplied [[PartitionPlan]].
  *
  * Hive does not, by default, include partition values in
  * the data written to disk. This is because the partition
  * values can always be inferred by the partition the source
  * file resides in.
  *
  * For example, if a source file 'abc.parquet' was located in a
  * partition "country=USA", then the value for the country field
  * does not need to be in the file, since it is clear from the
  * directory what the value should be.
  *
  * Therefore this mapper will return an output struct with all
  * partition values removed.
  */
class DropPartitionValuesMapper(plan: PartitionPlan) extends StructMapper {

  override def map(input: Struct): Struct = {
    val partitionKeys = plan.keys.map(_.value).toList
    val dataFields    = input.schema.fields().asScala.filterNot(field => partitionKeys.contains(field.name))
    val builder = dataFields.foldLeft(SchemaBuilder.struct) { (builder, field) =>
      builder.field(field.name, field.schema)
    }
    val schema = builder.build()
    dataFields.foldLeft(new Struct(schema)) { (struct, field) =>
      struct.put(field.name, input.get(field.name))
    }
  }
}
