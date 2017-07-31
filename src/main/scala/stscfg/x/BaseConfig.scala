package stscfg.x

import com.typesafe.config.{Config, ConfigFactory}

import scala.collection.JavaConverters._
import scala.reflect.ClassTag
import scala.reflect.runtime.universe._

/**
  * Extend this to define a base configuration object.
  *
  * @param config Typesafe configuration object
  */
abstract class BaseConfig(config: Config) {

  /**
    * Extend this to define an embedded configuration object.
    */
  class ObjConfig(implicit name: sourcecode.Name) extends BaseConfig(config.getConfig(name.value))

  /**
    * Designates a configuration entry.
    *
    * @tparam T  The expected type for the entry
    */
  def $[T : ClassTag : TypeTag](implicit valName: sourcecode.Name): T = {
    doIt[T](valName.value)
  }

  /**
    * Use this to indicate an entry with a default value.
    * @param default  The default value
    * @tparam T       The expected type for the entry
    */
  def $[T : ClassTag : TypeTag](default: T)(implicit valName: sourcecode.Name): T = {
    doIt[T](valName.value, defaultOpt = Some(default))
  }

  private def doIt[T : ClassTag : TypeTag](name: String, defaultOpt: Option[T] = None): T = {

    def requireValue(configValueOpt: Option[Any]): Any =
      (configValueOpt orElse defaultOpt).getOrElse(
        throw new RuntimeException("missing value for " + name)
      )

    def requireList(configValueOpt: Option[Any]): List[_] = {
      requireValue(configValueOpt) match {
        case l:java.util.ArrayList[_] ⇒ l.asScala.toList
        case _ ⇒ throw new RuntimeException("expecting list for " + name)
      }
    }

    def requireHashMap(configValueOpt: Option[Any]): HMap = {
      requireValue(configValueOpt) match {
        case m:HMap ⇒ m
        case _      ⇒ throw new RuntimeException("expecting object for " + name)
      }
    }

    def handleType(typ: Type, configValueOpt: Option[Any]): T = typ match {
      case t if t <:< typeOf[BaseConfig] ⇒
        val m = runtimeMirror(getClass.getClassLoader)
        val cls = m.runtimeClass(typ.typeSymbol.asClass)
        createObject(cls, requireHashMap(configValueOpt)).asInstanceOf[T]

      case t if t <:< typeOf[Option[_]] ⇒
        handleOption(typ.typeArgs.head, configValueOpt)

      case t if t <:< typeOf[List[_]] ⇒
        handleList(typ.typeArgs.head, requireList(configValueOpt))

      case _ ⇒
        handleBasic(typ, requireValue(configValueOpt))
    }

    def handleOption(argType: Type, configValueOpt: Option[Any]): T =
      configValueOpt.map(v ⇒ handleType(argType, Some(v))).asInstanceOf[T]

    def handleList(argType: Type, values: List[_]): T =
      values.map(v ⇒ handleType(argType, Some(v))).asInstanceOf[T]

    def handleBasic(basicType: Type, value: Any): T =
      value.asInstanceOf[T]

    handleType(typeOf[T], if (config.hasPath(name))
      Some(config.getAnyRef(name))
    else None)
  }

  private def createObject(cls: Class[_], value: HMap): Any = {
    val d = ConfigFactory.parseMap(value.asInstanceOf[java.util.HashMap[String, _]])
    val ctor = cls.getConstructor(classOf[Config])
    ctor.newInstance(d)
  }

  private type HMap = java.util.HashMap[_, _]
}
