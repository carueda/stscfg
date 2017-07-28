package stscfg

import com.typesafe.config.Config

import scala.collection.JavaConverters._
import scala.reflect.runtime.universe._

trait BaseConfig {
  def config: Config

  def string(implicit valName: sourcecode.Name): Extractor[String] =
    new Extractor(valName.value)

  def int(implicit valName: sourcecode.Name): Extractor[Int] =
    new Extractor(valName.value)

  def double(implicit valName: sourcecode.Name): Extractor[Double] =
    new Extractor(valName.value)

  def bool(implicit valName: sourcecode.Name): Extractor[Boolean] =
    new Extractor(valName.value)

  import scala.language.implicitConversions

  implicit def extractor2t[T](xtor: Extractor[T]): T = xtor.extract

  def obj(implicit valName: sourcecode.Name): Config =
    config.getConfig(valName.value)

  def objList[T](implicit valName: sourcecode.Name, ctor: Config ⇒ T): List[T] =
    config.getObjectList(valName.value).asScala.toList.map(_.toConfig).map(ctor)

  class ObjConfig(implicit objName: sourcecode.Name) extends DConfigNode(config)(objName)

  class Extractor[T : TypeTag](name: String, default: Option[T] = None) {

    def |(d: T): Extractor[T] = new Extractor[T](name, default = Some(d))

    def unary_~ : Extractor[Option[T]] = new Extractor[Option[T]](name)

    // there's probably a less boilerplate way to do this
    def extract: T = typeOf[T] match {
      case t if t =:= typeOf[String] ⇒
        (default match {
          case None ⇒
            config.getString(name)
          case Some(value) ⇒
            if (config.hasPath(name))
              config.getString(name)
            else
              value
        }).asInstanceOf[T]

      case t if t =:= typeOf[Option[String]] ⇒
        (if (config.hasPath(name))
          Some(config.getString(name))
        else None).asInstanceOf[T]

      case t if t =:= typeOf[Int] ⇒
        (default match {
          case None ⇒
            config.getInt(name)
          case Some(value) ⇒
            if (config.hasPath(name))
              config.getInt(name)
            else
              value
        }).asInstanceOf[T]

      case t if t =:= typeOf[Option[Int]] ⇒
        (if (config.hasPath(name))
          Some(config.getInt(name))
        else None).asInstanceOf[T]

      case t if t =:= typeOf[Double] ⇒
        (default match {
          case None ⇒
            config.getDouble(name)
          case Some(value) ⇒
            if (config.hasPath(name))
              config.getDouble(name)
            else
              value
        }).asInstanceOf[T]

      case t if t =:= typeOf[Boolean] ⇒
        (default match {
          case None ⇒
            config.getBoolean(name)
          case Some(value) ⇒
            if (config.hasPath(name))
              config.getBoolean(name)
            else
              value
        }).asInstanceOf[T]

      case t if t =:= typeOf[Option[Boolean]] ⇒
        (if (config.hasPath(name))
          Some(config.getBoolean(name))
        else None).asInstanceOf[T]
    }
  }

}

abstract class DConfigNode(parent: Config)
                          (implicit objName: sourcecode.Name) extends BaseConfig {
  def config: Config = parent.getConfig(objName.value)
}
