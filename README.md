# Read Me of the Configuration Service

[![Build Status](https://img.shields.io/travis/tomtom-international/configuration-service.svg?maxAge=3600)](https://travis-ci.org/tomtom-international/configuration-service)
[![Coverage Status](https://coveralls.io/repos/github/tomtom-international/configuration-service/badge.svg?branch=master&maxAge=3600)](https://coveralls.io/github/tomtom-international/configuration-service?branch=master)
[![License](http://img.shields.io/badge/license-APACHE2-blue.svg)]()
[![Release](https://img.shields.io/github/release/tomtom-international/configuration-service.svg?maxAge=3600)](https://github.com/tomtom-international/configuration-service/releases)

## Introduction

The Configuration Service provide a REST API service to dynamically get "application
configuration data", which consists of key-value pairs with any type of data. Typical
use of the service would be: 

* to dynamically get configuration data for devices, without hard-coding the configuration
in the device;

* to allow for dynamic service discovery of other services (returning key-value pairs which
contain the service name and its URI, for example);

* to allow dynamically moving clients into different configurations, such as moving devices
from production into beta-testing or debugging configurations;
 
* and many more.

A set of input criteria provided by the client determines what configuration data is returned.
The input criteria are essentially search terms into a configurations search tree of the server.

The return configuration data consists of generic key-value pairs, returned in either JSON 
or XML format.

The service reads its configurations search tree at start-up, is completely stateless and 
requires no database or disk. This improves the service reliability and reduces its operational
costs.

The hierarchy of the configurations search tree is decoupled from the format of the query criteria,
so you can rearrange the tree without affecting the clients.  

If the query can only be partially matched with the search tree, the best matching node
of the tree (which contains the key/value parameters) is returned. This allows you to 
specify partial subtrees with "default" or "fallback" configurations.

This also mean that if the top node of the tree specifies a configuration, the service will 
always return a configuration, if only the default configuration specified at the top node.

## API - A Short Reference Guide

First, let's have a look at the API of the service. The concepts around the search tree
and such are explained further down.

The API provides the following resources:

* `GET /`: return human-readable HTML help text, useful as a quick reference guide.

* `GET /version`: return the (POM) version of the service and the URI of the configuration 
file for the search tree.

* `GET /status`: return `200 OK` if and only if the service is correctly configured and running OK; 
it's used for monitoring purposes (and load balancers).

* `GET /tree/[/{level1}[/{level2[...]]]`: return a specific node from the search tree (hardly ever used).

* `GET /tree?levels={nameLevel1}[/{nameLevel2}[/...]]&search={level1}[/{level2}[/...]]`: query the search tree for a 
configuration, the most commonly used method.

The configuration of the service is fetched from a URI specified in the properties file called

    application-configuration-data.properties

Normally you would use the search capability of the service to find the best matching node, based on
hierarchical search criteria, which falls back to parent nodes for missing entries:

    GET /tree?levels={nameLevel1}[/{nameLevel2}[/...]]&search={level1}[/{level2}[/...]]

The search path is now provided as a query parameter and the order of the node levels is defined by
'levels' (each node level in the configuration has a name).
The returned result is the value of the leaf of the deepest node matching the search criteria:

    {"parameters" : [{"key": "{key1}", "value": "{value1}"}, ...],
    "matched" : "{path-of-deepest-node-that-matched}"}

The "matched" value indicates which node provided the parameters. This may be an exact match
of the search path in the query, or any node above it (if path as partially matched).

You can get multiple configurations at once by supplying more than 1 query string after 'search='
all separated by a `;`, like this:

    GET /tree?levels=level1/level2/...&search=query1;query2;...

The result of a multi-query request is a JSON array of results, with the elements in the same order
as the sub-queries that were specified.

You can use the `If-Modified-Since` HTTP header to have the service return `304 NOT MODIFIED`
if the configuration was no newer than the supplied date. Note that the HTTP header must be of the format:

    If-Modified-Since: Sun, 06 Nov 1994 08:49:37 GMT

You can also use the `If-None-Match` HTTP header to have the service return `304 NOT MODIFIED`
if the supplied ETag is the same for the returned data. The HTTP header must be of the format:

    ETag: "686897696a7c876b7e"

A less common use-case is to get specific individual nodes of the configuration. You can do this
by specifying a complete path into the search tree:

    GET /tree[/{level1}[/{level2[...]]]

Note that this does not `search` the tree, trying to match level names and using fallbacks.
It just returns a node if it exists or `404 NOT FOUND` if it doesn't.
The returned response looks like this:

    {"nodes": ["{node1}", "{node2}", ...],
    "parameters": [{"key": "{key1}", "value": "{value1}"]}, ...],  "name": "{node-name"}}

The `nodes` array is optional and lists the children nodes with search
terms one level below the specified node.

The `parameters` value is the optional leaf node of this node and lists the
search result (an array of key-value pairs).

Note that this is exactly the same format as the configuration file for the service.

**Return codes:**

* `200 - OK`: Successful call.
                          
* `304 - NOT MODIFIED`: Not modified since `If-Modified-Since` or `ETag` hasn't changed.
    
* `404 - NOT FOUND`: Node not found or no search result found.
                  
## A Simple Example

Consider a service which returns configuration data for a number of services,
like "personal settings" and "URLs" for service discovery. Most clients will get the same
configuration data, which are called the default configurations. But some
clients need to get different configurations.

The configurations search tree may be created like in the picture below. Notice how
the configuration may be located in leaf nodes or in intermediate nodes (which are
effectively default values for non-matching leaf node terms).

                                   +------+
     ROOT                          | ROOT |
     LEVEL                         +------+
                                     /  \               
                      +-------------+    +------------------+
     SERVICE          | "Settings"  |    | "URLs"           |
     LEVEL            |             |    |                  |
                      | color: blue |    | login: http://x1 |
                      | pet:   dog  |    | status:http://x2 |
                      +-------------+    +------------------+
                      /             \                 \
              +------------+   +--------------+   +------------------+      
     CLIENT   | "Fred"     |   | "Jane"       |   | "Fred"           |      
     LEVEL    |            |   |              |   |                  |
              | color: red |   | color: green |   | login: http://y1 |
              | pet:   cat |   | pet:   fish  |   | status:http://y2 |
              +------------+   +--------------+   +------------------+

Now, client may request their configurations providing 2 simple search
criteria: 

* the service name, "Settings" or "URLs", and

* a client ID, like a device ID, or in this case, a name, "Fred" or "Jane".

For example, requesting "settings" for "Fred" will return `color:red` and
`pet:cat`, whilst requesting them for "Bob" (not listed) returns the defaults
`color:blue` and `pet:dog`. 

The way you organize levels in the configurations search tree may have a
big impact in how complex the tree gets over time. For example, the same tree
may have been specified as: 

                                 +------+
     ROOT                        | ROOT |
     LEVEL                       +------+\_________
                                /      \           \          
                      +--------+      +--------+    +------+
     CLIENT           | "Fred" |      | "Jane" |    |  .*  |   .* means:
     LEVEL            |        |      |        |    |      |   if unmatched
                      |        |      +--------+    |      |
                      |        |       /      \     |      |
                      +--------+     Settings URLs  +------+\_______
                      /         \                       |           \
               +-------------+ +------------------+ +-------------+ +------------------+
     SERVICE   | "Settings"  | | "URLs"           | | "Settings"  | | "URLs"           |
     LEVEL     |             | |                  | |             | |                  |
               | color: red  | | login: http://y1 | | color: blue | | login: http://x1 |
               | pet:   cat  | | status:http://y2 | | pet:   Dog  | | status:http://x2 |
               +-------------+ +------------------+ +-------------+ +------------------+

This tree serves the same configurations as the first one, but its levels are
reversed: clients are selected first, then services. It now has 10 nodes, instead of 6. 

The good news is that the design of the service allows you to rearrange the node levels in the
configurations search tree without affecting the client. The client query remains exactly the
same.

A concise description of the service is presented in the PDF file
`Configuration Service.pdf`, found in the root directory of this source
repository.

## Build and Run

To build and run the REST API, copy the properties and configuration file and once into your
main resources directory, so Maven will copy them to your classpath if your run Tomcat or Jetty.

(Don't worry, the `.gitignore` file specifically excludes these file, so you won't accidentally
commit them to Git later.)

    cp src/external-resources/configuration.properties  src/main/resources
    cp src/external-resources/example.json              src/main/resources

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

    curl -s -X GET http://localhost:8080/tree?levels=service/model/device&search=TPEG/P508

Or search for a closest match with the fallback search mechanism:

    curl -s -X GET http://localhost:8080/tree?levels=service/mode/deviceID&search=TPEG/P508/Device123
    curl -s -X GET http://localhost:8080/tree?levels=service/mode/deviceID&search=TPEG/P508/Device456
    curl -s -X GET http://localhost:8080/tree?levels=service/mode/deviceID&search=OTHER
    curl -s -X GET http://localhost:8080/tree?levels=service/mode/deviceID&search=TPEG
    curl -s -X GET http://localhost:8080/tree?levels=service/mode/deviceID&search=SYS

Notica how you can change the order of search terms by modifying `levels`. These 2 commands get
the same configuration:

    curl -s -X GET http://localhost:8080/tree?levels=service/model/device&search=TPEG/P508/Device123
    curl -s -X GET http://localhost:8080/tree?levels=device/service/model&search=Device123/TPEG/P508


### JSON or XML

The service allows both JSON and XML, both for the configuration files, as well as
as the REST API responses.

To use XML or JSON configuration files, simply make sure the content is either parseable
as XML or JSON. The system accepts both formats.
 
To retrieve JSON bodies from the REST API, either omit the `Accept` header, or specify 
`Accept:application/json`. To get XML repsonses, specify `Accept:application/xml`. 

#### Recommended Practices

* Node names cannot contain the characters ',', ';' or '/' (as they have a special meaning
in the search query). The service will fail to start if it finds incorrect node names.

* The names and number of parameters can be different for every node, but normally
you would return the same number of parameters and they would have the same names.

* The `modified` attribute may be specified for any node. Normally, you should at least
provide a `modified` time for the root node, so clients can use the HTTP `If-Modified-Since`
header.

* The top-level node msut be nameless and may contain a set of default parameters.
If it specifies default parameters, searches will always return a result.

#### Example JSON Configuration File and Response

Below is an example of a configuration file for the service. Some remarks:


 
```json
{
  "modified": "2016-01-02T12:34:56Z",
  "levels": ["level-name"],
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
      "key": "key-default",
      "value": "value-default"
    }
  ]
}

``` 

And a JSON search response for this tree using `GET /tree?levels=level-name&search=child-2` looks this:

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
        <level>level-name</level>
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
by `,`, like this:

    curl -s -X GET http://localhost:8080/tree?levels=service/model/deviceID&search=TPEG/P107,SYS

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
`~/.gitignore_global` file by executing:
`git config --global core.excludesfile ~/.gitignore_global`

Note that running this command does not *create* the file, it just makes `git` use it. You need to create the
file in advance yourself (with a simple text editor).

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
