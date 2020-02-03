Openscoring [![Build Status](https://travis-ci.org/openscoring/openscoring.png?branch=master)](https://travis-ci.org/openscoring/openscoring)
===========

REST web service for scoring PMML models.

# Table of Contents #

- [Features](#features)
- [Prerequisites](#prerequisites)
- [Installation](#installation)
    + [Binary release](#binary-release)
    + [Source code snapshot](#source-code-snapshot)
- [Usage](#usage)
    + [Server side](#server-side)
        * [Advanced configuration](#advanced-configuration)
        * [Logging](#logging)
    + [Client side](#client-side)
- [REST API](#rest-api)
    + [Overview](#overview)
    + [Model deployment](#model-deployment)
        * [PUT /model/${id}](#put-modelid)
    + [Model querying](#model-querying)
        * [GET /model](#get-model)
        * [GET /model/${id}](#get-modelid)
        * [GET /model/${id}/pmml](#get-modelidpmml)
    + [Model evaluation](#model-evaluation)
        * [POST /model/${id}](#post-modelid)
        * [POST /model/${id}/batch](#post-modelidbatch)
        * [POST /model/${id}/csv](#post-modelidcsv)
    + [Model undeployment](#model-undeployment)
        * [DELETE /model/${id}](#delete-modelid)
    + [Metric querying](#metric-querying)
        * [GET /metric/model/${id}](#get-metricmodelid)
- [License](#license)
- [Additional information](#additional-information)

# Features #

* Full support for PMML specification versions 3.0 through 4.3. The evaluation is handled by the [JPMML-Evaluator](https://github.com/jpmml/jpmml-evaluator) library.
* Simple and powerful REST API:
  * Model deployment and undeployment.
  * Model evaluation in single prediction, batch prediction and CSV prediction modes.
  * Model metrics.
* High performance and high throughput:
  * Sub-millisecond response times.
  * Request and response compression using `gzip` and `deflate` encodings.
  * Thread safe.
* Open, extensible architecture for easy integration with proprietary systems and services:
  * User authentication and authorization.
  * Metrics dashboards.

# Prerequisites #

* Java 1.8 or newer.

# Installation #

### Binary release

Openscoring client and server uber-JAR files are distributed via the [GitHub releases page](https://github.com/openscoring/openscoring/releases), and the Openscoring webapp WAR file is distributed via the Maven Central repository.

This README file corresponds to latest source code snapshot. In order to follow its instructions as closely as possible, it's recommended to download the latest binary release.

The current version is **1.4.6** (3 February, 2020):

* [openscoring-client-executable-1.4.6.jar](https://github.com/openscoring/openscoring/releases/download/1.4.6/openscoring-client-executable-1.4.6.jar)
* [openscoring-server-executable-1.4.6.jar](https://github.com/openscoring/openscoring/releases/download/1.4.6/openscoring-server-executable-1.4.6.jar)
* [openscoring-webapp-1.4.6.war](http://search.maven.org/remotecontent?filepath=org/openscoring/openscoring-webapp/1.4.6/openscoring-webapp-1.4.6.war)

### Source code snapshot

Enter the project root directory and build using [Apache Maven](http://maven.apache.org/):
```
mvn clean install
```

The build produces two uber-JAR files and a WAR file:

* `openscoring-client/target/openscoring-client-executable-1.4-SNAPSHOT.jar`
* `openscoring-server/target/openscoring-server-executable-1.4-SNAPSHOT.jar`
* `openscoring-webapp/target/openscoring-webapp-1.4-SNAPSHOT.war`

# Usage #

The example PMML file `DecisionTreeIris.pmml` along with example JSON and CSV files can be found in the `openscoring-service/src/etc` directory.

### Server side

Launch the executable uber-JAR file:
```
java -jar openscoring-server-executable-${version}.jar
```

By default, the REST web service is started at [http://localhost:8080/openscoring](http://localhost:8080/openscoring/). The main class `org.openscoring.server.Main` accepts a number of configuration options for URI customization and other purposes. Please specify `--help` for more information.

```
java -jar openscoring-server-executable-${version}.jar --help
```

##### Advanced configuration

Copy the sample Typesafe's Config configuration file `openscoring-server/application.conf.sample` to a new file `application.conf`, and customize its content to current needs. Use the `config.file` system property to impose changes on the JVM:
```
java -Dconfig.file=application.conf -jar openscoring-server-executable-${version}.jar
```

The local configuration overrides the default configuration that is defined in the reference REST web service configuration file `openscoring-service/src/main/reference.conf`. For example, the following local configuration would selectively override the list-valued `networkSecurityContextFilter.trustedAddresses` property (treats any local or remote IP address as a trusted IP address):
```
networkSecurityContextFilter {
	trustedAddresses = ["*"]
}
```

##### Logging

Copy the sample Java Logging API configuration file `openscoring-server/logging.properties.sample` to a new file `logging.properties`, and customize its content to current needs. Use the `java.util.logging.config.file` system property to impose changes on the JVM:
```
java -Djava.util.logging.config.file=logging.properties -jar target/openscoring-server-executable-${version}.jar
```

### Client side

Replay the life cycle of a sample `DecisionTreeIris` model (in "REST API", see below) by launching the following Java application classes from the uber-JAR file:
```
java -cp openscoring-client-executable-${version}.jar org.openscoring.client.Deployer --model http://localhost:8080/openscoring/model/DecisionTreeIris --file DecisionTreeIris.pmml

java -cp openscoring-client-executable-${version}.jar org.openscoring.client.Evaluator --model http://localhost:8080/openscoring/model/DecisionTreeIris -XSepal_Length=5.1 -XSepal_Width=3.5 -XPetal_Length=1.4 -XPetal_Width=0.2

java -cp openscoring-client-executable-${version}.jar org.openscoring.client.CsvEvaluator --model http://localhost:8080/openscoring/model/DecisionTreeIris --input input.csv --output output.csv

java -cp openscoring-client-executable-${version}.jar org.openscoring.client.Undeployer --model http://localhost:8080/openscoring/model/DecisionTreeIris
```

The deployment and undeployment of models can be automated by launching the `org.openscoring.client.DirectoryDeployer` Java application class from the uber-JAR file, which listens for PMML file addition and removal events on the specified directory ("PMML directory watchdog"):
```
java -cp openscoring-client-executable-${version}.jar org.openscoring.client.DirectoryDeployer --model-collection http://localhost:8080/openscoring/model --dir pmml
```

# REST API #

### Overview

Model REST API endpoints:

| HTTP method | Endpoint | Required role(s) | Description |
| ----------- | -------- | ---------------- | ----------- |
| GET | /model | - | Get the summaries of all models |
| PUT | /model/${id} | admin | Deploy a model |
| GET | /model/${id} | - | Get the summary of a model |
| GET | /model/${id}/pmml | admin | Download a model as a PMML document |
| POST | /model/${id} | - | Evaluate data in "single prediction" mode |
| POST | /model/${id}/batch | - | Evaluate data in "batch prediction" mode |
| POST | /model/${id}/csv | - | Evaluate data in "CSV prediction" mode |
| DELETE | /model/${id} | admin | Undeploy a model |

Metric REST API endpoints:

| HTTP method | Endpoint | Required role(s) | Description |
| ----------- | -------- | ---------------- | ----------- |
| GET | /metric/model | admin | Get the metric sets of all models |
| GET | /metric/model/${id} | admin | Get the metric set of a model |

By default, the "admin" role is granted to all HTTP requests that originate from the local network address.

In case of an error (ie. response status codes 4XX or 5XX), the response body is a JSON serialized form of an `org.openscoring.common.SimpleResponse` [(source)](https://github.com/openscoring/openscoring/blob/master/openscoring-common/src/main/java/org/openscoring/common/SimpleResponse.java) object.

Java clients may use the following idiom to check if an operation succeeded or failed:
```java
ModelResponse response = ...;

// The error condition is encoded by initializing the "message" field and leaving all other fields uninitialized
String message = response.getMessage();
if(message != null){
	throw new RuntimeException(message);
}

// Proceed as usual
```

### Model deployment

##### PUT /model/${id}

Creates or updates a model.

The request body is a PMML document (indicated by content-type header `text/xml` or `application/xml`).

The response body is a JSON serialized form of an `org.openscoring.common.ModelResponse` [(source)](https://github.com/openscoring/openscoring/blob/master/openscoring-common/src/main/java/org/openscoring/common/ModelResponse.java) object.

Response status codes:
* 200 OK. The model was updated.
* 201 Created. A new model was created.
* 400 Bad Request. The deployment failed permanently. The request body is not a valid and/or supported PMML document.
* 403 Forbidden. The acting user does not have an "admin" role.
* 500 Internal Server Error. The deployment failed temporarily.

Sample cURL invocation:
```
curl -X PUT --data-binary @DecisionTreeIris.pmml -H "Content-type: text/xml" http://localhost:8080/openscoring/model/DecisionTreeIris
```

The same, using the `gzip` encoding:
```
curl -X PUT --data-binary @DecisionTreeIris.pmml.gz -H "Content-encoding: gzip" -H "Content-type: text/xml" http://localhost:8080/openscoring/model/DecisionTreeIris
```

### Model querying

##### GET /model

Gets the summaries of all models.

The response body is a JSON serialized form of an `org.openscoring.common.BatchModelResponse` [(source)](https://github.com/openscoring/openscoring/blob/master/openscoring-common/src/main/java/org/openscoring/common/BatchModelResponse.java) object.

Response status codes:
* 200 OK. The model collection was queried.

Sample cURL invocation:
```
curl -X GET http://localhost:8080/openscoring/model
```

##### GET /model/${id}

Gets the summary of a model.

The response body is a JSON serialized form of an `org.openscoring.common.ModelResponse` [(source)](https://github.com/openscoring/openscoring/blob/master/openscoring-common/src/main/java/org/openscoring/common/ModelResponse.java) object.

Response status codes:
* 200 OK. The model was queried.
* 404 Not Found. The requested model was not found.

Sample cURL invocation:
```
curl -X GET http://localhost:8080/openscoring/model/DecisionTreeIris
```

Sample response:
```json
{
	"id" : "DecisionTreeIris",
	"miningFunction" : "classification",
	"summary" : "Tree model",
	"properties" : {
		"created.timestamp" : "2015-03-17T12:41:35.933+0000",
		"accessed.timestamp" : "2015-03-21T09:35:58.582+0000",
		"file.size" : 4306,
		"file.md5sum" : "2d4698076ed807308c5ae40563b70345"
	},
	"schema" : {
		"inputFields" : [
			{
				"id" : "Sepal_Length",
				"name" : "Sepal length in cm",
				"dataType" : "double",
				"opType" : "continuous",
				"values" : [ "[4.3, 7.9]" ]
			},
			{
				"id" : "Sepal_Width",
				"name" : "Sepal width in cm",
				"dataType" : "double",
				"opType" : "continuous",
				"values" : [ "[2.0, 4.4]" ]
			},
			{
				"id" : "Petal_Length",
				"name" : "Petal length in cm",
				"dataType" : "double",
				"opType" : "continuous",
				"values" : [ "[1.0, 6.9]" ]
			},
			{
				"id" : "Petal_Width",
				"name" : "Petal width in cm",
				"dataType" : "double",
				"opType" : "continuous",
				"values" : [ "[0.1, 2.5]" ]
			}
		],
		"groupFields" : [],
		"targetFields" : [
			{
				"id" : "Species",
				"dataType" : "string",
				"opType" : "categorical",
				"values" : [ "setosa", "versicolor", "virginica" ]
			}
		],
		"outputFields" : [
			{
				"id" : "Probability_setosa",
				"dataType" : "double",
				"opType" : "continuous"
			},
			{
				"id" : "Probability_versicolor",
				"dataType" : "double",
				"opType" : "continuous"
			},
			{
				"id" : "Probability_virginica",
				"dataType" : "double",
				"opType" : "continuous"
			},
			{
				"id" : "Node_Id",
				"dataType" : "string",
				"opType" : "categorical"
			}
		]
	}
}
```

Field definitions are retrieved from the [MiningSchema](http://www.dmg.org/v4-2-1/MiningSchema.html) and [Output](http://www.dmg.org/v4-2-1/Output.html) elements of the PMML document. The input and group-by fields relate to the `arguments` attribute of the evaluation request, whereas the target and output fields relate to the `result` attribute of the evaluation response (see below).

##### GET /model/${id}/pmml

Downloads a model.

The response body is a PMML document.

Response status codes:
* 200 OK. The model was downloaded.
* 403 Forbidden. The acting user does not have an "admin" role.
* 404 Not Found. The requested model was not found.

Sample cURL invocation:
```
curl -X GET http://localhost:8080/openscoring/model/DecisionTreeIris/pmml
```

### Model evaluation

##### POST /model/${id}

Evaluates data in "single prediction" mode.

The request body is a JSON serialized form of an `org.openscoring.common.EvaluationRequest` [(source)](https://github.com/openscoring/openscoring/blob/master/openscoring-common/src/main/java/org/openscoring/common/EvaluationRequest.java) object.

The response body is a JSON serialized form of an `org.openscoring.common.EvaluationResponse` [(source)](https://github.com/openscoring/openscoring/blob/master/openscoring-common/src/main/java/org/openscoring/common/EvaluationResponse.java) object.

Response status codes:
* 200 OK. The evaluation was successful.
* 400 Bad Request. The evaluation failed permanently due to missing or invalid input data.
* 404 Not Found. The requested model was not found.
* 500 Internal Server Error. The evaluation failed temporarily.

Sample cURL invocation:
```
curl -X POST --data-binary @EvaluationRequest.json -H "Content-type: application/json" http://localhost:8080/openscoring/model/DecisionTreeIris
```

Sample request:
```json
{
	"id" : "record-001",
	"arguments" : {
		"Sepal_Length" : 5.1,
		"Sepal_Width" : 3.5,
		"Petal_Length" : 1.4,
		"Petal_Width" : 0.2
	}
}
```

Sample response:
```json
{
	"id" : "record-001",
	"result" : {
		"Species" : "setosa",
		"Probability_setosa" : 1.0,
		"Probability_versicolor" : 0.0,
		"Probability_virginica" : 0.0,
		"Node_Id" : "2"
	}
}
```

##### POST /model/${id}/batch

Evaluates data in "batch prediction" mode.

The request body is a JSON serialized form of an `org.openscoring.common.BatchEvaluationRequest` [(source)](https://github.com/openscoring/openscoring/blob/master/openscoring-common/src/main/java/org/openscoring/common/BatchEvaluationRequest.java) object.

The response body is a JSON serialized form of an `org.openscoring.common.BatchEvaluationResponse` [(source)](https://github.com/openscoring/openscoring/blob/master/openscoring-common/src/main/java/org/openscoring/common/BatchEvaluationResponse.java) object.

Response status codes:
* 200 OK. The evaluation was successful.
* 400 Bad Request. The evaluation failed permanently due to missing or invalid input data.
* 404 Not Found. The requested model was not found.
* 500 Internal Server Error. The evaluation failed temporarily.

Sample cURL invocation:
```
curl -X POST --data-binary @BatchEvaluationRequest.json -H "Content-type: application/json" http://localhost:8080/openscoring/model/DecisionTreeIris/batch
```

The evaluation is performed at "record" isolation level. If the evaluation of some `org.openscoring.common.EvaluationRequest` object fails, then the corresponding `org.openscoring.common.EvaluationResponse` object encodes the error condition (see above).

##### POST /model/${id}/csv

Evaluates data in "CSV prediction" mode.

The request body is a CSV document (indicated by content-type header `text/plain`). The data table must contain a data column for every input and group-by field. The ordering of data columns is not significant, because they are mapped to fields by name.

The CSV reader component detects the CSV dialect by probing `,`, `;` and `\t` as CSV delimiter characters. This detection functionality can be suppressed by supplying the value of the CSV delimiter character using the `delimiterChar` query parameter.

The response body is a CSV document. The data table contains a data column for every target and output field.

The first data column can be employed for row identification purposes. It will be copied over from the request data table to the response data table if its name equals to "Id" (the comparison is case insensitive) and the number of rows did not change during the evaluation.

Response status codes:
* 200 OK. The evaluation was successful.
* 400 Bad request. The evaluation failed permanently. The request body is not a valid and/or supported CSV document, or it contains cells with missing or invalid input data.
* 404 Not Found. The requested model was not found.
* 500 Internal Server Error. The evaluation failed temporarily.

Sample cURL invocation:
```
curl -X POST --data-binary @input.csv -H "Content-type: text/plain; charset=UTF-8" http://localhost:8080/openscoring/model/DecisionTreeIris/csv > output.csv
```

The same, using the `gzip` encoding:
```
curl -X POST --data-binary @input.csv.gz -H "Content-encoding: gzip" -H "Content-type: text/plain; charset=UTF-8" -H "Accept-encoding: gzip" http://localhost:8080/openscoring/model/DecisionTreeIris/csv > output.csv.gz
```

Sample request:
```
Id,Sepal_Length,Sepal_Width,Petal_Length,Petal_Width
record-001,5.1,3.5,1.4,0.2
record-002,7,3.2,4.7,1.4
record-003,6.3,3.3,6,2.5
```

Sample response:
```
Id,Species,Probability_setosa,Probability_versicolor,Probability_virginica,Node_Id
record-001,setosa,1.0,0.0,0.0,2
record-002,versicolor,0.0,0.9074074074074074,0.09259259259259259,6
record-003,virginica,0.0,0.021739130434782608,0.9782608695652174,7
```

The evaluation is performed at "all-records-or-nothing" isolation level. If the evaluation of some row fails, then the whole CSV document fails.

### Model undeployment

##### DELETE /model/${id}

Deletes a model.

The response body is a JSON serialized form of an `org.openscoring.common.SimpleResponse` [(source)](https://github.com/openscoring/openscoring/blob/master/openscoring-common/src/main/java/org/openscoring/common/SimpleResponse.java) object.

Response status codes:
* 200 OK. The model was deleted.
* 403 Forbidden. The acting user does not have an "admin" role.
* 404 Not Found. The requested model was not found.
* 500 Internal Server Error. The undeployment failed temporarily.

Sample cURL invocation:
```
curl -X DELETE http://localhost:8080/openscoring/model/DecisionTreeIris
```

An HTTP PUT or DELETE method can be masked as an HTTP POST method by using the [HTTP method override mechanism](https://jersey.java.net/apidocs/latest/jersey/org/glassfish/jersey/server/filter/HttpMethodOverrideFilter.html).

Sample cURL invocation that employs the `X-HTTP-Method-Override` request header:
```
curl -X POST -H "X-HTTP-Method-Override: DELETE" http://localhost:8080/openscoring/model/DecisionTreeIris
```

Sample cURL invocation that employs the `_method` query parameter:
```
curl -X POST http://localhost:8080/openscoring/model/DecisionTreeIris?_method=DELETE
```

### Metric querying

##### GET /metric/model/${id}

Gets the snapshot of the metric set of a model.

The response body is a JSON serialized form of an `org.openscoring.common.MetricSetResponse` [(source)](https://github.com/openscoring/openscoring/blob/master/openscoring-common/src/main/java/org/openscoring/common/MetricSetResponse.java) object.

Response status codes:
* 200 OK. The evaluation was successful.
* 403 Forbidden. The acting user does not have an "admin" role.
* 404 Not Found. The requested model was not found.

Sample cURL invocation:
```
curl -X GET http://localhost:8080/openscoring/metric/model/DecisionTreeIris
```

Sample response:
```json
{
	"version" : "3.0.0",
	"counters" : {
		"records" : {
			"count" : 1
		}
	},
	"gauges" : { },
	"histograms" : { },
	"meters" : { },
	"timers" : {
		"evaluate" : {
			"count" : 1,
			"max" : 0.008521913,
			"mean" : 0.008521913,
			"min" : 0.008521913,
			"p50" : 0.008521913,
			"p75" : 0.008521913,
			"p95" : 0.008521913,
			"p98" : 0.008521913,
			"p99" : 0.008521913,
			"p999" : 0.008521913,
			"stddev" : 0.0,
			"m15_rate" : 0.19237151525464488,
			"m1_rate" : 0.11160702915400945,
			"m5_rate" : 0.17797635419760474,
			"mean_rate" : 0.023793073545863026,
			"duration_units" : "seconds",
			"rate_units" : "calls/second"
		}
	}
}
```

# License #

Openscoring is licensed under the terms and conditions of the [GNU Affero General Public License, Version 3.0](https://www.gnu.org/licenses/agpl-3.0.html).
For a quick summary of your rights ("Can") and obligations ("Cannot" and "Must") under AGPLv3, please refer to [TLDRLegal](https://tldrlegal.com/license/gnu-affero-general-public-license-v3-(agpl-3.0)).

If you would like to use Openscoring in a proprietary software project, then it is possible to enter into a licensing agreement which makes it available under the terms and conditions of the [BSD 3-Clause License](https://opensource.org/licenses/BSD-3-Clause) instead.
Please initiate the conversation by submitting the [Request for Quotation](https://openscoring.io/rfq/) web form, or sending an e-mail.

# Additional information #

Openscoring is developed and maintained by Openscoring Ltd, Estonia.

Interested in using [Java PMML API](https://github.com/jpmml) or [Openscoring REST API](https://github.com/openscoring) software in your company? Please contact [info@openscoring.io](mailto:info@openscoring.io)
