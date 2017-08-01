package stscfg

import java.time.Duration

import com.typesafe.config.{Config, ConfigFactory}

import scala.collection.JavaConverters._
import scala.reflect.ClassTag
import scala.reflect.runtime.universe._

/**
  * Represents a size-in-bytes value
  */
final case class Size(bytes: Long)

/**
  * Extend this to define a base configuration object.
  * Example:
  * {{{
  * object cfg extends BaseConfig(config) {
  *   val interface : String = string | "0.0.0.0"
  *   val port      : Int    = int | 8080
  *   ...
  * }
  * }}}
  *
  * @param config Typesafe configuration object
  */
abstract class BaseConfig(config: Config) {

  abstract class ObjConfig(implicit name: sourcecode.Name) extends BaseConfig(config.getConfig(name.value))

  import scala.language.implicitConversions

  implicit def extractor2t[T](xtor: Extractor[T]): T = xtor.extract

  def $[T : ClassTag : TypeTag](implicit valName: sourcecode.Name): Extractor[T] = {
    new Extractor[T](valName.value)
  }

  def string(implicit valName: sourcecode.Name): Extractor[String] = $[String]

  def int(implicit valName: sourcecode.Name): Extractor[Int] = $[Int]

  def long(implicit valName: sourcecode.Name): Extractor[Long] = $[Long]

  def double(implicit valName: sourcecode.Name): Extractor[Double] = $[Double]

  def bool(implicit valName: sourcecode.Name): Extractor[Boolean] = $[Boolean]

  def duration(implicit valName: sourcecode.Name): Extractor[Duration] = $[Duration]

  def size(implicit valName: sourcecode.Name): Extractor[Size] = $[Size]

  def optional[T : TypeTag](x: Extractor[T])
             (implicit valName: sourcecode.Name): Extractor[Option[T]] =
    new Extractor[Option[T]](valName.value)

  def optional[T : ClassTag : TypeTag](implicit valName: sourcecode.Name): Extractor[Option[T]] = {
    new Extractor[Option[T]](valName.value)
  }

  /**
    * Actual extraction of the configuration value.
    */
  class Extractor[T : ClassTag : TypeTag](name: String, defaultOpt: Option[T] = None) {

    /**
      * Allows to indicate a default value.
      * Example:
      * {{{
      *   val string = string | "foo"
      *   val port   = int | 8080
      * }}}
      * @param d  The default value
      */
    def |(d: T): Extractor[T] = defaultOpt match {
      case None    ⇒ new Extractor[T](name, defaultOpt = Some(d))

      case Some(v) ⇒ throw new RuntimeException(
        s"| operator already used for $name (with value $v)")
    }

    def list: Extractor[List[T]] = new Extractor[List[T]](name)

    def optional: Extractor[Option[T]] = new Extractor[Option[T]](name)

    def extract: T = {
      def requireValue(configValueOpt: Option[Any]): Any =
        (configValueOpt orElse defaultOpt).getOrElse(
          throw new RuntimeException("missing value for " + name)
        )

      def requireList(configValueOpt: Option[Any]): List[_] = {
        requireValue(configValueOpt) match {
          case l: java.util.ArrayList[_] ⇒ l.asScala.toList
          case _ ⇒ throw new RuntimeException("expecting list for " + name)
        }
      }

      def requireHashMap(configValueOpt: Option[Any]): HMap = {
        requireValue(configValueOpt) match {
          case m: HMap ⇒ m
          case _ ⇒ throw new RuntimeException("expecting object for " + name)
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

        case t if t =:= typeOf[Duration] ⇒
          handleDuration(requireValue(configValueOpt))

        case t if t =:= typeOf[Size] ⇒
          handleSize(requireValue(configValueOpt))

        case t if t =:= typeOf[Long] ⇒
          handleLong(requireValue(configValueOpt))

        case _ ⇒
          handleBasic(requireValue(configValueOpt))
      }

      def handleOption(argType: Type, configValueOpt: Option[Any]): T =
        configValueOpt.map(v ⇒ handleType(argType, Some(v))).asInstanceOf[T]

      def handleList(argType: Type, values: List[_]): T =
        values.map(v ⇒ handleType(argType, Some(v))).asInstanceOf[T]

      def handleDuration(value: Any): T =
        ConfigFactory.parseString(s"d = $value").getDuration("d").asInstanceOf[T]

      def handleSize(value: Any): T = {
        val v = value match {
          case Size(b) ⇒ b
          case _       ⇒ value
        }
        val bytes = ConfigFactory.parseString(s"p = $v").getBytes("p")
        Size(bytes).asInstanceOf[T]
      }

      def handleLong(value: Any): T =
        value.asInstanceOf[java.lang.Number].longValue().asInstanceOf[T]

      def handleBasic(value: Any): T =
        value.asInstanceOf[T]

      handleType(typeOf[T], if (config.hasPath(name))
        Some(config.getAnyRef(name))
      else None)
    }
  }

  private def createObject(cls: Class[_], value: HMap): Any = {
    val d = ConfigFactory.parseMap(value.asInstanceOf[java.util.HashMap[String, _]])
    val ctor = cls.getConstructor(classOf[Config])
    ctor.newInstance(d)
  }

  private type HMap = java.util.HashMap[_, _]
}
