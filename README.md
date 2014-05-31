# FastForward &#187;

[![Build Status](https://travis-ci.org/spotify/ffwd.svg?branch=master)](https://travis-ci.org/spotify/ffwd)

A highly flexible, multi-protocol events and metrics forwarder capable of
merging and decorating events and metrics from a number of sources and
multiplexing them to number of event and metric consumers.

FFWD is a agent meant to run on a single host and receive metrics and events
using a set of standard protocols.
It is meant to run on a system and listen to the default port of its
supported protocols, this meshes well with any client that wants to send to it
in that they typically require zero configuration.

The following is an example client sending riemann events.

```python
import bernhard
client = bernhard.Client(transport=bernhard.UDPTransport)

client.send({'service': 'myservice', 'metric': 12, 'unit': 'B/s'})
```

FFWD takes care to add any _system-wide_ tags, like _site_, _host_ and the
_role_ of the machine it is sent from. See the __core -> attributes__ section
in the [Example Configuration](docs/simple.conf) for details.

This allows for decoration of the received metrics and events to make them
_semantic from the source_.

This is reminiscent of the concepts described in the
[Metrics 2.0 Specification](http://metrics20.org).

* [Usage](#usage)
* [Installation](#installation)
  * [Installing Plugins](#installing-plugins)
* [Contributing](#contributing)
* [Debugging](#debugging)
* [Terminology](#terminology)

Other focused topics.
* [Tunneling and multi-tenancy](docs/tunneling-and-multi-tenancy.md)
* [Writing Plugins](docs/writing-plugins.md)
* [Events and Metrics](docs/events-and-metrics.md)
  * [Input Data Structure](docs/events-and-metrics.md#input-data-structure)
  * [Output Data Structure](docs/events-and-metrics.md#output-data-structure)
* [Schemas](docs/schemas.md)
* [FFWD vs. collectd](docs/vs-collectd.md)
* [JSON Reference Protocol](docs/json-protocol.md)
  &mdash; Documentation about the JSON reference protocol.
* [Protobuf Protocol](docs/protobuf-protocol.md)
  &mdash; Documentation about the protobuf protocol.
* [Statistics](docs/statistics.md) &mdash; Documentation about internally
  generated statistics.

## Usage

The simplest possible use is to install all dependencies and run it using the
supplied example configuration.

```bash
$ bundle install
$ bin/ffwd -c docs/simple.conf
```

This will start up an instance that periodically reports statistics about
itself.

You can now send events and metrics to it using one of the enabled input
protocols.

E.g. the carbon protocol (requires [ffwd-carbon](/plugins/ffwd-carbon)) or the
built-in reference [JSON protocol](/lib/ffwd/plugin/json.rb).

```bash
$ echo "random.diceroll 4  `date +%s`" | nc -q0 localhost 2003
$ echo '{"type": "metric", "key": "random.diceroll", "value": 6}' | nc -q0 localhost 19000
```

You can try out more advanced protocols using the supplied sample client:
[docs/client-test.rb](docs/client-test.rb).

```bash
$ ruby docs/client-test.rb
```

If you have the log output plugin enabled, you should see these metrics written
to the log.

FFWD has support for multi-tenancy (multiple guests reporting into the same
FFWD agent).
For more information, see
[Tunneling and multi-tenancy](docs/tunneling-and-multi-tenancy.md)

## Installation

FFWD is available on rubygems so it can be installed through
[gem](https://rubygems.org).

```bash
$ gem install ffwd
```

### Installing plugins

FFWD uses plugins which has to be installed separately in order to use them.

You can list the plugins available on rubygems through gem.

```bash
$ gem search -r 'ffwd-*'
```

You can then install the plugin(s) you want.

```bash
$ gem install ffwd-<plugin>
```

You can check that the plugin is available to FFWD using the **--plugins**
command.

```bash
$ ffwd --plugins
Loaded Plugins:
  Plugin 'log'
    Source: from gem: ffwd-<version>
    Supports: output
    Description: ...
    Options:
      ...
  Plugin 'json'
    Source: from gem: ffwd-<version>
    Supports: input
    Description: ...
    Options:
      ...
  Plugin 'tunnel'
    Source: from gem: ffwd-tunnel-<version>
    Supports: input
    Description: ...
    Options:
      ...
...
```

At this point you will probably discover that FFWD does not support your
favorite plugin.
Reading our [writing plugins guide](docs/writing-plugins.md) should enable you
to remedy this.

## Statistics

FFWD reports internal statistics allowing for an insight into what is going on.

All statistics are reported as internally generated metrics with a rich set of
tags.

For details in which statistics are available, see [the Statistics
documentation](docs/statistics.md).

## Contributing

1. Fork FastForward (or a plugin) from
   [github](https://github.com/spotify/ffwd) and clone your fork.
2. Hack.
3. Verify code by running any existing test-suite; ```bundle exec rspec```.
   Try to include tests for your changes.
4. Push the branch back to GitHub.
5. Send a pull request to our upstream repo.

## Debugging

While the agent is running, it is possible to sniff all internal input and
output channels by enabling the debug component.

This is done by adding the following to your ffwd configuration.

```
:debug: {}
```

This will setup the component to listen on the default debug port (19001).

The traffic can now be sniffed with a tool like netcat.

```
$ nc localhost 19001
{"id":"core.output","type":"event","data": ...}
{"id":"tunnel.input/127.0.0.1:55606","type":"event","data": ...}
{"id":"core.output","type":"event","data": ...}
{"id":"tunnel.input/127.0.0.1:55606","type":"event","data": ...}
{"id":"tunnel.input/127.0.0.1:55606","type":"metric","data": ...}
...
```

*As an alternative to connecting directly to the debug socket, you can also use
the [**fwc**](#debugging-with-fwc) tool*.

Each line consists of a JSON object with the following fields.

**id** The id that the specified events can be grouped by, this indicates
which channel the traffic was sniffed of.

**type** The type of the *data* field.

**data** Data describing the sniffed event according to specified *type*.

### Debugging with fwc

**fwc** is a [small CLI tool](lib/fwc.rb) for connecting and analyzing FFWD
debug data.

It can be invoked with the **--raw** and/or **--summary** switch.

**--raw** will output everything received on the debug socket, but will also
attempt to throttle the output to protect the users terminal.

**--summary** will only output a summary of *what has been seen* on the various
channels.

The output will look something like the following.

```
<time> INFO: Summary Report:
<time> INFO:   core.input (event)
<time> INFO:     event-foo 2
1time> INFO:     event-bar 2
1time> INFO:   core.output (event)
1time> INFO:     event-foo 2
1time> INFO:     event-bar 2
```

The above says that the **core.input** channel has passed two events with the
keys **event-foo** and **event-bar**.

Similarly the **core.output** channel has passed the same set of events,
meaning that all of the events are expected to have been processed by output
plugins.

## Terminology

**Channel** &mdash; [The one way](lib/ffwd/channel.rb) to do synchronous
message passing between components.

**PluginChannel** &mdash; [An abstraction](lib/ffwd/plugin_channel.rb) on
top of two *Channel*'s, one dedicated to *metrics*, the other one to *events*.

**Core** &mdash; [The component](lib/ffwd/core.rb) in FFWD that ties
everything together.

The *core* component is also broken up into two other distinct parts to support
*virtual* cores when tunneling. These are.

* **CoreProcessor** Is responsible for running calculation engines for rates,
  histograms, etc...
* **CoreEmitter** Is responsible for emitting metrics and events, passing them
  either straight to the supplied output channel or into the supplied
  *CoreProcessor*.
