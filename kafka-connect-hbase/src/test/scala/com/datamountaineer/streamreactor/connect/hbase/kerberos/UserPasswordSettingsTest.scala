package com.datamountaineer.streamreactor.connect.hbase.kerberos

import com.datamountaineer.streamreactor.connect.hbase.config.HBaseConfig
import com.datamountaineer.streamreactor.connect.hbase.config.HBaseConfigConstants
import org.apache.kafka.common.config.ConfigException
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import scala.jdk.CollectionConverters.MapHasAsJava

class UserPasswordSettingsTest extends AnyFunSuite with Matchers with FileCreation {
  test("validate a user-password setting") {
    val fileKrb5 = createFile(s"krb1.krb5")
    val fileJaas = createFile(s"jaas1.jaas")
    try {
      val user     = "yoda"
      val password = "123456"

      val config = HBaseConfig(
        Map(
          HBaseConfigConstants.KCQL_QUERY          -> s"INSERT INTO someTable SELECT * FROM someTable",
          HBaseConfigConstants.COLUMN_FAMILY       -> "someColumnFamily",
          HBaseConfigConstants.KerberosKey         -> "true",
          HBaseConfigConstants.KerberosUserKey     -> user,
          HBaseConfigConstants.KerberosPasswordKey -> password,
          HBaseConfigConstants.KerberosKrb5Key     -> fileKrb5.getAbsolutePath,
          HBaseConfigConstants.KerberosJaasKey     -> fileJaas.getAbsolutePath,
          HBaseConfigConstants.JaasEntryNameKey    -> "abc",
        ).asJava,
      )

      val actualSettings = UserPasswordSettings.from(config, HBaseConfigConstants)
      actualSettings shouldBe UserPasswordSettings(user,
                                                   password,
                                                   fileKrb5.getAbsolutePath,
                                                   fileJaas.getAbsolutePath,
                                                   "abc",
                                                   None,
      )
    } finally {
      fileKrb5.delete()
      fileJaas.delete()
      ()
    }
  }

  test("raise an exception when user is null") {
    val fileKrb5 = createFile(s"krb1.krb5")
    val fileJaas = createFile(s"jaas1.jaas")
    try {
      val user     = null
      val password = "123456"

      val config = HBaseConfig(
        Map(
          HBaseConfigConstants.KCQL_QUERY          -> s"INSERT INTO someTable SELECT * FROM someTable",
          HBaseConfigConstants.COLUMN_FAMILY       -> "someColumnFamily",
          HBaseConfigConstants.KerberosKey         -> "true",
          HBaseConfigConstants.KerberosUserKey     -> user,
          HBaseConfigConstants.KerberosPasswordKey -> password,
          HBaseConfigConstants.KerberosKrb5Key     -> fileKrb5.getAbsolutePath,
          HBaseConfigConstants.KerberosJaasKey     -> fileJaas.getAbsolutePath,
        ).asJava,
      )

      intercept[ConfigException] {
        UserPasswordSettings.from(config, HBaseConfigConstants)
      }
    } finally {
      fileKrb5.delete()
      fileJaas.delete()
      ()
    }
  }

  test("raises an exception where password is null") {
    val fileKrb5 = createFile(s"krb1.krb5")
    val fileJaas = createFile(s"jaas1.jaas")
    try {
      val user     = "yoda"
      val password = null

      val config = HBaseConfig(
        Map(
          HBaseConfigConstants.KCQL_QUERY          -> s"INSERT INTO someTable SELECT * FROM someTable",
          HBaseConfigConstants.COLUMN_FAMILY       -> "someColumnFamily",
          HBaseConfigConstants.KerberosKey         -> "true",
          HBaseConfigConstants.KerberosUserKey     -> user,
          HBaseConfigConstants.KerberosPasswordKey -> password,
          HBaseConfigConstants.KerberosKrb5Key     -> fileKrb5.getAbsolutePath,
          HBaseConfigConstants.KerberosJaasKey     -> fileJaas.getAbsolutePath,
        ).asJava,
      )

      intercept[ConfigException] {
        UserPasswordSettings.from(config, HBaseConfigConstants)
      }
    } finally {
      fileKrb5.delete()
      fileJaas.delete()
      ()
    }
  }

  test("raise an exception when there is no krb5 file set") {
    val fileJaas = createFile(s"jaas1.jaas")
    try {
      val user     = "yoda"
      val password = "123456"

      val config = HBaseConfig(
        Map(
          HBaseConfigConstants.KCQL_QUERY          -> s"INSERT INTO someTable SELECT * FROM someTable",
          HBaseConfigConstants.COLUMN_FAMILY       -> "someColumnFamily",
          HBaseConfigConstants.KerberosKey         -> "true",
          HBaseConfigConstants.KerberosUserKey     -> user,
          HBaseConfigConstants.KerberosPasswordKey -> password,
          HBaseConfigConstants.KerberosJaasKey     -> fileJaas.getAbsolutePath,
        ).asJava,
      )

      intercept[ConfigException] {
        UserPasswordSettings.from(config, HBaseConfigConstants)
      }
    } finally {
      fileJaas.delete()
      ()
    }
  }

  test("raises and exception when the jaas file is not set") {
    val fileKrb5 = createFile(s"krb1.krb5")
    try {
      val user     = "yoda"
      val password = "123456"

      val config = HBaseConfig(
        Map(
          HBaseConfigConstants.KCQL_QUERY          -> s"INSERT INTO someTable SELECT * FROM someTable",
          HBaseConfigConstants.COLUMN_FAMILY       -> "someColumnFamily",
          HBaseConfigConstants.KerberosKey         -> "true",
          HBaseConfigConstants.KerberosUserKey     -> user,
          HBaseConfigConstants.KerberosPasswordKey -> password,
          HBaseConfigConstants.KerberosKrb5Key     -> fileKrb5.getAbsolutePath,
        ).asJava,
      )

      intercept[ConfigException] {
        UserPasswordSettings.from(config, HBaseConfigConstants)
      }
    } finally {
      fileKrb5.delete()
      ()
    }
  }
}
