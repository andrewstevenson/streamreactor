package com.landoop.streamreactor.connect.hive.kerberos

import com.landoop.streamreactor.connect.hive.utils.AbstractConfigExtension._
import com.landoop.streamreactor.connect.hive.utils.FileUtils
import org.apache.kafka.common.config.AbstractConfig

case class UserPasswordSettings(
  user:              String,
  password:          String,
  krb5Path:          String,
  jaasPath:          String,
  jaasEntryName:     String,
  nameNodePrincipal: Option[String],
)

object UserPasswordSettings {
  def from(config: AbstractConfig, hiveConstants: KerberosSettings): UserPasswordSettings = {
    val user     = config.getStringOrThrowIfNull(hiveConstants.KerberosUserKey)
    val password = config.getPasswordOrThrowIfNull(hiveConstants.KerberosPasswordKey)

    val krb5 = config.getStringOrThrowIfNull(hiveConstants.KerberosKrb5Key)
    FileUtils.throwIfNotExists(krb5, hiveConstants.KerberosKrb5Key)

    val jaas = config.getStringOrThrowIfNull(hiveConstants.KerberosJaasKey)
    FileUtils.throwIfNotExists(jaas, hiveConstants.KerberosJaasKey)

    val jaasEntryName     = config.getString(hiveConstants.JaasEntryNameKey)
    val namenodePrincipal = Option(config.getString(hiveConstants.NameNodePrincipalKey))
    UserPasswordSettings(user, password, krb5, jaas, jaasEntryName, namenodePrincipal)
  }
}
