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

//  def obj[T](implicit valName: sourcecode.Name, ctor: Config ⇒ T): T =
//    ctor(config.getConfig(valName.value))

  def obj(implicit valName: sourcecode.Name): Extractor[Config] =
    new Extractor(valName.value)

//  def objList[T](implicit valName: sourcecode.Name, ctor: Config ⇒ T): List[T] =
//    config.getObjectList(valName.value).asScala.toList.map(_.toConfig).map(ctor)

  class ObjConfig(implicit objName: sourcecode.Name) extends DConfigNode(config)(objName)

  // TODO support complex things like these:
  //     int.list.list.optional
  //     obj.list.optional
  // with *composition* of Extractors: BasicExtractor, OptExtractor,
  // ListExtractor, and the like.
  // Currently, the following is simplistic and with a bunch of boilerplate..

  class Extractor[T : TypeTag](name: String, default: Option[T] = None) {

    def |(d: T): Extractor[T] = new Extractor[T](name, default = Some(d))

    def optional: Extractor[Option[T]] = new Extractor[Option[T]](name)

    def list: Extractor[List[T]] = new Extractor[List[T]](name)

    def unary_~ : Extractor[Option[T]] = optional
    def unary_! : Extractor[List[T]]   = list

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

      case t if t =:= typeOf[List[Int]] ⇒
        config.getIntList(name).asScala.toList.asInstanceOf[T]

      // Double
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

      case t if t =:= typeOf[Option[Double]] ⇒
        (if (config.hasPath(name))
          Some(config.getDouble(name))
        else None).asInstanceOf[T]

      case t if t =:= typeOf[List[Double]] ⇒
        config.getDoubleList(name).asScala.toList.asInstanceOf[T]

      // Boolean
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

      case t if t =:= typeOf[List[Boolean]] ⇒
        config.getBooleanList(name).asScala.toList.asInstanceOf[T]

      // Config
      case t if t =:= typeOf[Config] ⇒
        (default match {
          case None ⇒
            config.getConfig(name)
          case Some(value) ⇒
            if (config.hasPath(name))
              config.getConfig(name)
            else
              value
        }).asInstanceOf[T]

      case t if t =:= typeOf[Option[Config]] ⇒
        (if (config.hasPath(name))
          Some(config.getConfig(name))
        else None).asInstanceOf[T]

      case t if t =:= typeOf[List[Config]] ⇒
        config.getObjectList(name).asScala.toList.map(_.toConfig).asInstanceOf[T]

    }
  }
}

abstract class DConfigNode(parent: Config)
                          (implicit objName: sourcecode.Name) extends BaseConfig {
  def config: Config = parent.getConfig(objName.value)
}
