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
  * input schema be equal or a superset of the metastore schema.
  *
  * This means that every field in the metastore schema must be
  * present in the incoming records, but the records may have
  * additional fields. These additional fields will be dropped
  * before the data is written out.
  */
object IgnoreEvolutionPolicy extends EvolutionPolicy {

  override def evolve(
    dbName:          DatabaseName,
    tableName:       TableName,
    metastoreSchema: Schema,
    inputSchema:     Schema,
  )(
    implicit
    client: IMetaStoreClient,
  ): Try[Schema] = Try {
    HiveSchemas.toKafka(client.getTable(dbName.value, tableName.value))
  }.map { schema =>
    val compatible = schema.fields().asScala.forall { field =>
      inputSchema.field(field.name) != null ||
      field.schema().isOptional ||
      field.schema().defaultValue() != null
    }
    if (compatible) schema else throw new ConnectException("Input Schema is not compatible with the metastore")
  }
}
