package com.landoop.streamreactor.connect.hive.sink.evolution

import com.landoop.streamreactor.connect.hive.DatabaseName
import com.landoop.streamreactor.connect.hive.HiveSchemas
import com.landoop.streamreactor.connect.hive.TableName
import org.apache.hadoop.hive.metastore.IMetaStoreClient
import org.apache.kafka.connect.data.Schema
import org.apache.kafka.connect.errors.ConnectException

import scala.jdk.CollectionConverters.ListHasAsScala
import scala.util.Try

/**
  * An compile of [[EvolutionPolicy]] that requires the
  * input schema to be equal to the metastore schema.
  *
  * This means that every field in the metastore schema must be
  * present in the incoming records, and the incoming records
  * cannot contain any extra fields.
  */
object StrictEvolutionPolicy extends EvolutionPolicy {

  override def evolve(
    dbName:          DatabaseName,
    tableName:       TableName,
    metastoreSchema: Schema,
    inputSchema:     Schema,
  )(
    implicit
    client: IMetaStoreClient,
  ): Try[Schema] = Try {
    val schema = HiveSchemas.toKafka(client.getTable(dbName.value, tableName.value))
    schema
  }.map { schema =>
    //Hive keeps the fields in lowercase
    val inputFields = inputSchema.fields().asScala.map { f =>
      f.name().toLowerCase()
    }.toSet
    schema.fields().asScala.foreach { field =>
      val exists     = inputFields.contains(field.name)
      val optional   = field.schema().isOptional
      val default    = field.schema().defaultValue()
      val compatible = exists || optional || default != null
      if (!compatible) {
        throw new ConnectException(s"Input Schema is not compatible with the metastore for field [${field.name()}]")
      }
    }
    schema
  }
}
