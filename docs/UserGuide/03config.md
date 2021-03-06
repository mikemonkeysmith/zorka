
# Configuring Zorka

Zorka exposes its API as modules with functions in a Beanshell interpreter. Zabbix server can call these functions
directly (with slightly different syntax), extension scripts can use them as well (and define their own functions
that will be visible for Zabbix or other monitoring system.

Beanshell scripts are primary way to configure and extend agent. Zorka executes all beanshell scripts from
`$ZORKA_HOME/conf` directory at startup (in alphabetical order). All elements declared in those scripts will
be visible via queries from monitoring servers. User can define his own namespaces, functions. Declaring which
elements should be instrumented (and how) is also possible via `.bsh` scripts.

## zorka.properties configuration file

Interesting configuration directives in zorka.properties are described below (along with some sample values).

Zorka agent general directives:

* `zorka.mbs.autoregister = yes` - this setting controls whether standard platform mbean server should be automatically
registered at first use (or startup time): this is useful when application server substitutes mbean server at startup
time and it has be acquired in other manner than at startup (JBoss 4/5/6 do this, so both 'java' and 'jboss' mbean
servers are acquired by zorka5.sar module instead of `ManagementFactory.getPlatformMBeanServer()`;

* `zorka.hostname = tomcat.myserver` - 'hostname' this agent will advertise itself; this is useful for for automatic
informing operating system about server hostname (eg. standard Zabbix Agent template has such item);

Zabbix related directives:

* `zabbix.enabled = yes` - this setting controls whether Zorka should work as zabbix agent (serving zabbix agent
protocol on some address:port); zabbix protocol is the only one supported at the moment;

* `zabbix.listen.addr = 0.0.0.0` - address zabbix agent will listen on (`127.0.0.1` is the default value, so it has to
be set explicitly);

* `zabbix.listen.port = 10055` - port zabbix agent will listen on;

* `zabbix.server.addr = 127.0.0.1` - zabbix server address; agent will accept connections only from this server;

Logging directives:

* `zorka.log.level = TRACE` - log level (`TRACE`, `DEBUG`, `INFO`, `WARN`, `ERROR`, `FATAL` are allowed);

* `zorka.log.size = 4m` - maximum size of log files (in megabytes if `m` is added as a suffix);

* `zorka.log.num = 4` - maximum number of archived log files;

* `zorka.log.exceptions = yes` - controls whether full stack traces of encountered exceptions will be logged;

Logging directives related to syslog:

* `zorka.syslog = no` - enables sending zorka logs to remote syslog server if set to `yes`;

* `zorka.syslog.server` - IP address and (optional) port number of syslog server in `addr:port` form;

* `zorka.syslog.facility` - syslog facility code each message will be tagged with (eg. `F_LOCAL0`);

Directives useful for tuning (you propably don't need to change default values):

* `zorka.req.threads = 4` - number of threads for processing requests;

* `zorka.req.queue = 64` -

* `zorka.req.timeout = 10000` - request timeout (in milliseconds) - requests taking more than this will be canceled
and zorka will close connection abruply;

* `syslog = yes` - controls whether enable (or disable) syslog support; syslog protocol is enabled by default;


## Basic API

Zorka exposes standard BeanShell environment (with access to all classes visible from system class loader plus all
Zorka code). There are several library objects visible:

* `zorka` - zorka-specific library functions (logging, JMX access etc., always available);

* `spy` - functions for configuring instrumentation engine (available if spy is enabled);

* `zabbix` - zabbix-specific functions (available if zabbix interface is enabled);

* `nagios` - nagios-specific functions (available if nagios interface is enabled);

* `normalizers` - data normalization framework functions;

* `syslog` - functions for sending messages to log host using syslog protocol;

All above things are visible Beanshell scripts from `$ZORKA_HOME/conf` directory. Interfaces to monitoring systems
(zabbix, nagios etc.) can call Beanshell functions as well (both built-in and user-defined) but due to their syntax
are typically limited to simple calls. For example:

    zorka.jmx["java","java.lang:type=OperatingSystem","Arch"]

is equivalent to:

    zorka.jmx("java","java.lang:type=OperatingSystem","Arch");


## MBean servers

Zorka can track many mbean servers at once. This is useful for example in JBoss 4/5/6 which have two mbean servers:
platform specific (JVM) and application server specific (JBoss). Each mbean server is available at some name.
Known names:

* `java` - standard plaform mbean server;

* `jboss` - JBoss JMX kernel mbean server;

MBean server name is passed in virtually all functions looking for object names or registering/manipulating
objects/attributes in JMX.

## Bytecode instrumentation

Instrumentation part of Zorka agent is called Zorka Spy. With version 0.2 a fluent-style configuration API has been
introduced. You can define spy configuration properties in fluent style using `SpyDefinition` object and then submit it
to instrumentation engine. In order to be able to configure instrumentations, you need to understand structure of
instrumentation engine and how events coming from instrumented methods are processed.


`TODO Diagram 1: Zorka Spy processing scheme.`


Zorka Spy will insert probes into certain points of instrumented methods. There are three kinds of points: entry points,
return points and error points (when exception has been thrown). A probe can fetch some data, eg. current time, method
argument, some class (method context) etc. All etched values are packed into a submission record (`SpyRecord` class) and
processed by one of argument processing chains (`ON_ENTER`, `ON_RETURN` or `ON_ERROR` depending of probe type), then
results of all probes from a method call are bound together and  processed by `ON_SUBMIT` chain. All these operations
are performed in method calling thread context (so these processing stages must be thread safe). After that, record is
passed to `ON_COLLECT` chain which is guaranteed to be single threaded. Finally records are dispatched into collectors
(which is also done in a single thread). Collectors can do various things with records: update statistics in some mbean,
call some BSH function, log it to file etc. New collector implementations can be added on the fly - either in Java or
as BeanShell scripts.


Spy definition example:

    collect(record) {
      mbs = record.get(4,0);
      zorka.registerMbs("jboss", mbs);
    }

    sdef = spy.instance().onReturn().withArguments(0)
       .lookFor("org.jboss.mx.server.MBeanServerImpl", "<init>")
       .toBsh("jboss");

    spy.add(sdef);

This is part of Zorka configuration for JBoss 5.x. It intercepts instantiation of `MBeanServerImpl` (at the end of its
constructor) and calls `collect()` function of jboss namespace (everything is declared in jboss namespace).
`SpyDefinition` objects are immutable, so when tinkering with `sdef` multiple times, remember to assign result of last
method call to some variable. Method `spy.instance()` returns empty (unconfigured) object of `SpyDefinition` type that
can be further configured using methods described in next sections.

For more examples see *Examples* section above.

### Choosing processing stages

Argument fetch and argument processing can be done in one of several points (see Diagram 1). Use the following functions
to choose stage (or probe point):

    sdef = sdef.onEnter(args...);
    sdef = sdef.onReturn(args...);
    sdef = sdef.onError(args...);
    sdef = sdef.onSubmit(args...);
    sdef = sdef.onCollect(args...);

### Fetching arguments

Data to be fetched by probes can be defined using `withArguments()` method:

    sdef = sdef.onEnter(arg1, arg2, ...);

Arguments can passed as numbers representing argument indexes or special data. For instance methods visible arguments
start with number 1 at number 0 there is reference to object itself (`this`). For static methods arguments start with
number 0.

There are some special indexes that represent other data possible to fetch by instrumentation probes:

* `spy.FETCH_TIME` (-1) - fetch current time;
* `spy.FETCH_RET_VAL` (-2) - fetch return value (this is valid only on return points);
* `spy.FETCH_ERROR` (-3) - fetch exception object (this is valid only on error points);
* `spy.FETCH_THREAD` (-4) - fetch current thread;
* `spy.FETCH_NULL` (-5) - fetch null constant (useful in some cases);

It is also possible to fetch classes - just pass strings containing fully qualified class names instread of integers.

Additional notes:

* remember that argument fetch can be done in various points, use `sdef.onXXX()` method to select proper stage;

* when instrumenting constructors, be sure that this reference (if used) can be fetched only at constructor return -
this is because at the beginning of a constructor this points to an uninitialized block of memory that does not
represent any object, so instrumented class won't load and will crash with class verifier error;

### Looking for classes and methods to be instrumented

    sdef = sdef.include(matcher1, matcher2, ...);

Using `include()` method administrator can add matching rules filtering which methods are to be instrumented. Methods
matching any of passed matchers will be included. Matchers can be defined using `spy.byXXX()` functions.

### Access to data in spy records

Spy processors and collectors access data stored in records using references that can have two forms:

* simple indexes (numbers) - will always refer to slot in current processing stage (stage processor has been added to);
these are simple integers; slot indexes start at 0;

* full references - contain both stage identifier and slot  number; these are strings with first character indicating
stage and followed by index, eg. `E0` means first slot in `ON_ENTER` stage;

The following characters are legal:

* `E` - `ON_ENTER`;
* `R` - `ON_RETURN`;
* `X` - `ON_EXCEPTION`;
* `S` - `ON_SUBMIT`;
* `C` - `ON_COLLECT`;


### Formatting strings

