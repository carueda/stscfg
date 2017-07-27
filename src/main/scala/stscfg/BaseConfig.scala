package stscfg

import com.typesafe.config.Config

import scala.collection.JavaConverters._

trait BaseConfig {
  def config: Config

  def string(implicit valName: sourcecode.Name): String =
    config.getString(valName.value)

  def int(implicit valName: sourcecode.Name): Int =
    config.getInt(valName.value)

  def double(implicit valName: sourcecode.Name): Double =
    config.getDouble(valName.value)

  def bool(implicit valName: sourcecode.Name): Boolean =
    config.getBoolean(valName.value)

  def obj(implicit valName: sourcecode.Name): Config =
    config.getConfig(valName.value)

  def objList[T](implicit valName: sourcecode.Name, ctor: Config ⇒ T): List[T] =
    config.getObjectList(valName.value).asScala.toList.map(_.toConfig).map(ctor)

  class ObjConfig(implicit objName: sourcecode.Name) extends DConfigNode(config)(objName)
}

abstract class DConfigNode(parent: Config)
                          (implicit objName: sourcecode.Name) extends BaseConfig {
  def config: Config = parent.getConfig(objName.value)
}
