# Read Me of the Configuration Service

[![Build Status](https://img.shields.io/travis/tomtom-international/configuration-service.svg?maxAge=3600)](https://travis-ci.org/tomtom-international/configuration-service)
[![Coverage Status](https://coveralls.io/repos/github/tomtom-international/configuration-service/badge.svg?branch=master&maxAge=3600)](https://coveralls.io/github/tomtom-international/configuration-service?branch=master)
[![License](http://img.shields.io/badge/license-APACHE2-blue.svg)]()
[![Release](https://img.shields.io/github/release/tomtom-international/configuration-service.svg?maxAge=3600)](https://github.com/tomtom-international/configuration-service/releases)

## Purpose

Are you running a service with multiple, varying clients? Or does your system need some
form of dynamic runtime configuration? The Configuration Service may help you in that case.

The Configuration Service is a service which selects a client configuration based on
a set of input criteria. The configuration consists of generic key/value pairs, returned
in either JSON or XML.

The configurations for clients are defined in a "configuration tree", a search tree, which is 
read by the service at start-up. 

The hierarchy of the search tree is decoupled from the format of the query criteria, so
you can rearrange the tree without affecting the clients.  

If the query can only be partially matched with the search tree, the best matching node
of the tree (which contains the key/value parameters) is returned. This allows you to 
specify partial subtrees with "default" or "fallback" configurations.

This also mean that if the top node of the tree specifies a configuration, the service will 
always return a configuration, if only the default configuration specified at the top node.

A concise description of the service is presented in the PDF file
"Configuration Service.pdf", found in the root directory of this source
repository.

## Build and Run

To build and run the REST API, copy the properties and configuration file and once into your
main resources directory, so Maven will copy them to your classpath if your run Tomcat or Jetty.

(Don't worry, the `.gitignore` file specifically excludes these file, so you won't accidentally
commit them to Git later.)

    cp src/test/resources/configuration.properties src/main/resources
    cp src/test/resources/example.json src/main/resources

And then type:

    mvn clean install
    mvn jetty:run           (alternatively, you can use: mvn tomcat7:run)

or, to view the test coverage, execute:

    mvn clean verify jacoco:report
    open target/site/jacoco/index.html

## Trying It Out

Try out if the web services work by entering the following URL in your web browser
(this should show you a HTML help page):

    http://localhost:8080/

Or use a tool like cURL:

    curl -X GET http://localhost:8080/

Try the following to fetch the entire configuration:

    curl -s -X GET http://localhost:8080/tree

Or one specific node (note that this does not apply the fallback search mechanism):

    curl -s -X GET http://localhost:8080/tree?levels=service,model,device&search=TPEG,P508

Or search for a closest match with the fallback search mechanism:

    curl -s -X GET http://localhost:8080/tree?levels=service/mode/deviceID&search=TPEG/P508/Device123
    curl -s -X GET http://localhost:8080/tree?levels=service/mode/deviceID&search=TPEG/P508/Device456
    curl -s -X GET http://localhost:8080/tree?levels=service/mode/deviceID&search=OTHER
    curl -s -X GET http://localhost:8080/tree?levels=service/mode/deviceID&search=TPEG
    curl -s -X GET http://localhost:8080/tree?levels=service/mode/deviceID&search=SYS

### JSON or XML

The service allows both JSON and XML, both for the configuration files, as well as
as the REST API responses.

To use XML or JSON configuration files, simply make sure the content is either parseable
as XML or JSON. The system accepts both formats.
 
To retrieve JSON bodies from the REST API, either omit the `Accept` header, or specify 
`Accept:application/json`. To get XML repsonses, specify `Accept:application/xml`. 

#### Example JSON Configuration File and Response

A JSON configuration file looks like this.
 
```json
{
  "modified": "2016-01-02T12:34:56Z",
  "levels": ["criterium-1"],
  "nodes": [
    {
      "name": "child-1",
      "parameters": [
        {
          "key": "key-1a",
          "value": "value-1a"
        },
        {
          "key": "key-1b",
          "value": "value-1b"
        }
      ]
    },
    {
      "name": "child-2",
      "parameters": [
        {
          "key": "key-2",
          "value": "value-2"
        }
      ]
    }
  ],
  "parameters": [
    {
      "key": "key-0",
      "value": "value-0"
    }
  ]
}

``` 

And a JSON search response for this tree using `GET /tree?levels=criterium-1&search=child-2` looks this:

```json
{
    "matched": "/child-2", 
    "parameters": [
        {
            "key": "key-2", 
            "value": "value-2"
        }
    ]
}
```
 
#### Example XML Configuration File

The same configuration file looks like this when provided as XML.
 
```xml
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<node>
    <levels>
        <level>criterium-1</level>
    </levels>
    <nodes>
        <node>
            <name>child-1</name>
            <parameters>
                <parameter>
                    <key>key-1a</key>
                    <value>value-1a</value>
                </parameter>
                <parameter>
                    <key>key-1b</key>
                    <value>value-1b</value>
                </parameter>
            </parameters>
        </node>
        <node>
            <name>child-2</name>
            <parameters>
                <parameter>
                    <key>key-2</key>
                    <value>value-2</value>
                </parameter>
            </parameters>
        </node>
    </nodes>
    <parameters>
        <parameter>
            <key>key-0</key>
            <value>value-0</value>
        </parameter>
    </parameters>
    <modified>2016-01-02T12:34:56Z</modified>
</node>
``` 
 
And, similarly, a XML search response for this three using `GET /parameter/tree?search=child-2` looks this:
 
 ```xml
<searchResult>
    <parameters>
        <parameter>
            <key>key-2</key>
            <value>value-2</value>
        </parameter>
    </parameters>
    <matched>/child-2</matched>
</searchResult>
```
  
### Multiple Search Queries in One Call

Or, to save a couple of roundtrips from the client to the server to fetch multiple
configurations, you can combine mutliple search queries into one by separating the sub-queries
by ',', like this:

    curl -s -X GET http://localhost:8080/tree?levels=service/model/deviceID&search=TPEG/P107;SYS

This would produce a JSON array of results for 2 independent queries. Especially for configuration trees
which are organized into 'product' subtrees, combining the queries for individual products may save
quite some calls.

### Reducing Data Usage

#### Using HTTP Header `If-Modified-Since`

You can also use the `If-Modified-Since` header to have the service return `304 NOT MODIFIED` if the configuration
was no newer than the supplied date. Note that the HTTP header must be of the format

    If-Modified-Since: Sun, 06 Nov 1994 08:49:37 GMT

The returned header in response looks like this:

    Last-Modified: Sun, 06 Nov 1994 08:49:37 GMT

This format is dictated by the W3C standard. 
The configuration file for the service itself uses the standard ISO notation, however:

    "modified" : "1994-11-06T08:49:37Z"

The time zone `Z` in this case denotes Zulu time, which equals GMT. Alternatively, `GMT` could
be used, or any other time zone.

The time provided in the request is compared to the `modified` property of the found node, or, if the node
has no such property, of its closest parent that has a `modified` property.

#### Using HTTP Header `If-None-Match`

You can also use `ETag`s to reduce data consumption. ETags may be thought of as the hash of the response: 
if two response are the same, they have the same `ETag` and if the `ETag`s differs, the responses differ.

The service returns an `ETag` value with each request and the caller may supply that `ETag` in the HTTP header 
`If-None-Match` header at a next request. If the response since last time is the same, the service will 
return HTTP status code `304 (NOT MODIFIED)` rather than providing the full response again.

The `ETag` is provided by the service in the HTTP as:

    ETag: “bb334669ed5a3fed2ad29aba0768d7586af5c515"

And the caller may provide this `ETag` in a subsequent request as:

    If-None-Match: "bb334669ed5a3fed2ad29aba0768d7586af5c515"
    
Failure to provide the correct format for the HTTP header (or accidentally use the ISO format) results in 
ignoring the header, which means a full result is always returned.

**Important note** Note that the `ETag` value *must* be enclosed in quotes according to the W3C standard.
Failure to do so treats the `ETag` value as non-matching (always returning a full response body). 

## Run Unit Tests

To run the unit tests, run

    mvn clean install

To run coverage tests for the API services against a local web server, run:

    mvn clean jetty:run
    cd src/test
    ./examples.sh

Total test coverage is around 80% at the moment (which includes running the `test.sh` test
script).

## Organization of Source Code

    src/main/java/com/tomtom/services/configuration
    |
    +-- deployment
    |   |
    |   +-- CorsFeature         Required to provide CORS, e.g. sample web page.
    |   +-- DeploymentModule    Binds resources to URLs and singletons.
    |   +-- StartupCheck        Provides safety net to check correct JVM version/settings at start-up.
    |
    +-- domain
    |  |
    |  +-- Node                 Domain objects. The entire configuration tree consists of nodes and
    |  +-- Parameter            parameters only.
    |                           
    +-- dto
    |  |
    |  +-- XyzDTO               Data transfer objects: (JSON) data objects used in API comms. (The DTO
    |                           objects are mutable DTO objects from SpeedTools verison 3.0.19+). 
    +-- implementation
    |   |                       Implementation of API methods. These use the SpeedTools web-services
    |   +-- XXXImpl             'processor' framework to allow near-linear scaling using Akka.
    |   +-- Tree                Tree data store for configuration items.
    |
    +-- resources               Property files. These are read by the SpeedTools framework.
    |   |
    |   +-- configuration-service.default.properties
    |   +-- configuration-service.properties

## Specifying the Default Configuration

You can specify a default configuration to be read at startup. This is configuration is specified
in the properties file called `configuration-service.properties`.

The format of the configuration is JSON and is specified as:

    { "name" : "some name",
    "modified" : {MODIFIED},
    "levels" : [ {LEVEL}, {LEVEL}, ... ],
    "nodes" : [ {NODE}, {NODE}, ... ],
    "parameters" : [ {PARAM}, {PARAM}, ...] }

Only for the root node, `{MODIFIED}` and `{LEVELS}` can be specified. 

`{MODIFIED}` has the format:

    "modified" : "2016-01-02T12:34:56Z"

`{LEVELS}` has the format:

    "levels" : ["name of level 1 criterium", "name of level 2 criterium", ...]
    
Note that obviously there must be at least as many named criteria as there are node levels in 
the specified configuration tree.
    
The name of the root node must be omitted (does not exist).

The format of child nodes is the same as the root node, except they have no modified property and
they must have a name.

The parameters `{PARAM}` are specified as:

    {"key" : "<keyname>", "value" : "<somevalue>"}

The value of `keyname` is a UTF8 regular expression and `somevalue` is a UTF8 string.
Elements of the input search path are matched against the `key` regular expression.
Note that constant strings are considered to be more exact matches than matches
against regular expressions, so they will prevail. For example, if there are 2 nodes
that match the string `SomeString`, one of which is the constat `SomeString` and the
other is a regular expression `Some.*`, the returned node will be the one with
`SomeString`, not `Some.*` (the exact match prevails).

Example:

```json
{
  "modified" : "2016-01-02T12:34:56Z",
  "levels" : ["service", "model", "deviceID" ],
  "nodes": [
    {
      "name": "TPEG",
      "nodes": [
        {
          "name": "P508",
          "parameters": [{"key": "radius", "value": "40"}, {"key": "interval", "value": "120"}]
        }, {
          "name": "P107",
          "nodes": [
            {
              "name": "Device[0-9]*",
              "parameters": [{"key": "radius", "value": "10"}, {"key": "interval", "value": "120"}]
            }, {
              "name": "Device123",
              "parameters": [{"key": "radius", "value": "80"}, {"key": "interval", "value": "60"}]
            }
          ]
        }
      ],
      "parameters": [{"key": "radius", "value": "25"}, {"key": "interval", "value": "120"}]
    }, {
      "name": "SYS",
      "parameters": [{"key": "demo", "value": "false"}, {"key": "sound", "value": "off"}]
    }
  ]
}
```

The location of the configuration can be specified as a URL, using the prefix `http:` or `https:`,
or as a text file, using the prefix `file:`, or as a file on the classpath, using `classpath:`.

Example lines of `application-configuration-data.properties`:

    ApplicationConfigurationData.startupConfigurationURI = http://some-server.com/example.json

or

    ApplicationConfigurationData.startupConfigurationURI = file::/full/path/to/example.json

or

    ApplicationConfigurationData.startupConfigurationURI = classpath::example.json

## Modularize and Re-Use Configurations Using Include Files

JSON does not specify a mechanism to modularize a message, for example using an include
mechanism. We've chosen to design such an include mechanism by specifying a reserved
key value called `include`, of which the value specifies the message body to insert.

Include files may be used to reduce the number of redundant configuration files in your setup.
For example, if many configurations share exactly the same settings for a subtree of the
configuration tree, you may simply create an include file fort that subtree.

You can specify these include files in a configuration by replacing any node with an attribute
called `"include" : "<URI of JSON to include>"`. The semantics are such that the key-value
pair is effectively replaced with the contents of JSON message to include.

For example, suppose the main file `example.json` contains this:

```json
{
  "modified" : "2016-01-02T12:34:56Z",
  "levels" : ["service", "model", "deviceID"],
  "nodes": [
    { "include" : "classpath::tpeg.json" },
    { "include" : "classpath::sys.json" }
  ]
}
```

And include file `tpeg.json` looks like this (note that `levels` can only be specified at
the root of the configuration, not here):

```json
{
  "name": "TPEG",
  "parameters": [ {"key":"radius", "value": "25"}, {"key":"interval", "value": "120"} ],
  "nodes": [
    { "include": "classpath::tpeg_p107.json" },
    { "include": "classpath::tpeg_p508.json" }
  ]
}
```


And include  file `tpeg_p107.json` looks like this:

```json
{
  "name": "P107",
  "nodes": [
    {
      "name": "Device[0-9]*",
      "parameters": [ {"key":"radius", "value": "10"}, {"key":"interval", "value": "120"} ]
    }, {
      "name": "Device123",
      "parameters": [ {"key":"radius", "value": "80"}, {"key":"interval", "value": "60"} ]
    }
  ]
}
```

And include file `tpeg_p508.json` looks like this:

```json
{
  "name": "P508",
  "nodes": [
    {
      "name": "Device1.*",
      "parameters": [ {"key":"radius", "value": "100"} ]
    }, {
      "name": "Device999",
      "parameters": [ {"key":"radius", "value": "200"} ]
    }
  ],
  "parameters": [ {"key":"radius", "value": "40"}, {"key":"interval", "value": "120"} ]
}
```

And include file `sys.json` looks like this:

```json
{
  "name": "SYS",
  "parameters": [
    {"key":"demo", "value": "false"},
    {"key":"sound", "value": "off"}
  ]
}
```

Then the resulting configuration looks like this:

```json
{
  "modified": "2016-01-02T12:34:56Z",
  "levels" : ["service", "model", "deviceID"],
  "nodes": [
    {
      "name": "TPEG",
      "nodes": [
        {
          "name": "P107",
          "nodes": [
            {
              "name": "Device[0-9]*",
              "parameters": [ { "key": "radius", "value": "10" }, { "key": "interval", "value": "120" } ]
            }, { 
              "name": "Device123", 
              "parameters": [ { "key": "radius", "value": "80" }, { "key": "interval", "value": "60" } ]
            }
          ]
        }, {
          "name": "P508",
          "nodes": [
            {
              "name": "Device1.*",
              "parameters": [ { "key": "radius", "value": "100" } ]
            }, {
              "name": "Device999",
              "parameters": [ { "key": "radius", "value": "200" }
              ]
            }
          ],
          "parameters": [ { "key": "radius", "value": "40" }, { "key": "interval", "value": "120" } ]
        }
      ],
      "parameters": [ { "key": "radius", "value": "25" }, { "key": "interval", "value": "120" } ]
    }, {
      "name": "SYS",
      "parameters": [ { "key": "demo", "value": "false" }, { "key": "sound", "value": "off" } ]
    }
  ]
}
```

This would replace the child node at the location of `"include"` with the contents of the
configuration specified in the URI.

Include files can be nested to any level (although the same include file obviously cannot be
included recursively).

This means you could separate config files into, for example:

    config files for service, which refer specific
        config files for specific device configurations, which refer specific
            config files for individual devices (perhaps for test purposes)

Note that include files do not have their own `modified` date. The modified date from the
root node is always used to determine the date/time of the entire configuration.

## Build Environment (Java 8)

The source uses Java JDK 1.8, so make sure your Java compiler is set to 1.8, for example
using something like (MacOSX):

    export JAVA_HOME=`/usr/libexec/java_home -v 1.8`

# License

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

# Using Git and `.gitignore`

It's good practice to set up a personal global `.gitignore` file on your machine which filters a number of files
on your file systems that you do not wish to submit to the Git repository. You can set up your own global
`~/.gitignore` file by executing:
`git config --global core.excludesfile ~/.gitignore`

In general, add the following file types to `~/.gitignore` (each entry should be on a separate line):
`*.com *.class *.dll *.exe *.o *.so *.log *.sql *.sqlite *.tlog *.epoch *.swp *.hprof *.hprof.index *.releaseBackup *~`

If you're using a Mac, filter:
`.DS_Store* Thumbs.db`

If you're using IntelliJ IDEA, filter:
`*.iml *.iws .idea/`

If you're using Eclips, filter:
`.classpath .project .settings .cache`

If you're using NetBeans, filter:
`nb-configuration.xml *.orig`

The local `.gitignore` file in the Git repository itself to reflect those file only that are produced by executing
regular compile, build or release commands, such as:
`target/ out/`

# Bug Reports and New Feature Requests

If you encounter any problems with this library, don't hesitate to use the `Issues` session to file your issues.
Normally, one of our developers should be able to comment on them and fix.

# Release Notes

### 1.0.0

* Initial release.
