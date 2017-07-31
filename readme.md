# stscfg

This preliminary project captures some ideas to extend the basic mechanism from 
[static-config](https://github.com/Krever/static-config)
to incorporate features from [tscfg](https://github.com/carueda/tscfg).
Possible future directions to explore include combining or bringing in aspects from
[refined](https://github.com/fthomas/refined), 
[bond](https://github.com/fwbrasil/bond) and similar.

## Usage

stscfg has two main classes, `BaseConfig` and `ObjConfig`.
Extend `BaseConfig` to define a base configuration object,
and extend `ObjConfig` to directly define an embedded 
configuration object. 

## Example

```scala
import com.typesafe.config.{Config, ConfigFactory}
import stscfg._

object cfg extends BaseConfig(ConfigFactory.load().resolve()) {

  val path : String  = string
  val url  : String  = string | "http://example.net"

  // directly defined embedded object
  object service extends ObjConfig {
    val port : Int = int | 8080
  }

  val foo    : FooCfg         = $[FooCfg]
  val optFoo : Option[FooCfg] = optional($[FooCfg])
  val foos   : List[FooCfg]   = $[FooCfg].list

  class FooCfg(c: Config) extends BaseConfig(c) {
    val str    : String      = string
    val optInt : Option[Int] = optional(int)
  }
}
```

With the `cfg` object defined above we can parse the following
configuration:

```
path = "/tmp"

service = {
  port = 9090
}

foo = { str = "baz", optInt = 3 }
optFoo = { str = "bar" }
foos = [ { str = "baz0" }, { str = "baz1" } ]
```

```scala
assert( cfg.path == "/tmp" )
assert( cfg.service.port == 9090 )
assert( cfg.foo.str == "baz" )
assert( cfg.foo.optInt == Some(3) )
```