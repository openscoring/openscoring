Openscoring [![Build Status](https://travis-ci.org/jpmml/openscoring.png?branch=master)](https://travis-ci.org/jpmml/openscoring)
===========

REST web service for scoring PMML models.

# IMPORTANT #

![](https://github.com/jpmml/openscoring/blob/master/bulb.png) Are you happy with our solution to your model deployment problem? Please show your support to this exciting technology **by voting** for our Hadoop Summit 2015 talk ["Rapid deployment of predictive models across Big Data platforms"] (https://hadoopsummit.uservoice.com/forums/283261-data-science-and-hadoop/suggestions/7074084). Thank you!

# Features #

* Full support for PMML specification versions 3.0 through 4.2. The evaluation is handled by the [JPMML-Evaluator] (https://github.com/jpmml/jpmml-evaluator) library.
* Simple and powerful REST API:
  * Model deployment and undeployment.
  * Model evaluation in single prediction, batch prediction and CSV prediction modes.
  * Model metrics.
* High performance and high throughput:
  * Sub-millisecond response times.
  * Thread safe.
* Open, extensible architecture for easy integration with proprietary systems and services:
  * User authentication and authorization.
  * Metrics dashboards.

# Installation and Usage #

The project requires Java 1.7 or newer to run.

Enter the project root directory and build using [Apache Maven] (http://maven.apache.org/):
```
mvn clean install
```

The example PMML file `DecisionTreeIris.pmml` along with example JSON and CSV files can be found in the `openscoring-service/src/etc` directory.

### Server side

##### Standalone application

The build produces an executable uber-JAR file `openscoring-server/target/server-executable-1.2-SNAPSHOT.jar`. Change the working directory to `openscoring-server` and execute the following command:
```
java -jar target/server-executable-1.2-SNAPSHOT.jar
```

By default, the REST web service is started at [http://localhost:8080/openscoring] (http://localhost:8080/openscoring/). The main class `org.openscoring.server.Main` accepts a number of configuration options for URI customization and other purposes. Please specify `--help` for more information.

The working directory contains a sample Java logging configuration file `logging.properties.sample` that should be copied over to a new file `logging.properties` and customized to current needs. A Java logging configuration file can be imposed on the JVM by defining the `java.util.logging.config.file` system property:
```
java -Djava.util.logging.config.file=logging.properties -jar target/server-executable-1.2-SNAPSHOT.jar
```

Additionally, the working directory contains a sample Typesafe's Config configuration file `application.conf.sample` that should be copied over to a new file `application.conf` and customized to current needs. This local configuration file can be imposed on the JVM by defining the `config.file` system property:
```
java -Dconfig.file=application.conf -jar target/server-executable-1.2-SNAPSHOT.jar
```

The local configuration file overrides the default configuration that is defined in the reference REST web service configuration file `openscoring-service/src/main/reference.conf`. For example, the following configuration file selectively overrides the list-valued `modelRegistry.visitorClasses` property:
```
modelRegistry {
	visitorClasses = [
		"org.jpmml.model.visitors.LocatorNullifier" // Erases SAX Locator information from the PMML class model object, which will considerable reduce the memory consumption of deployed models
	]
}
```

##### Web application

The build produces a WAR file `openscoring-webapp/target/openscoring-webapp-1.2-SNAPSHOT.war`. This WAR file can be deployed using any Java web container.

The web application can be launced using [Jetty Maven Plugin] (http://eclipse.org/jetty/documentation/current/jetty-maven-plugin.html). Change the working directory to `openscoring-webapp` and execute the following command:
```
mvn jetty:run-war
```

### Client side

The build produces an executable uber-JAR file `openscoring-client/target/client-executable-1.2-SNAPSHOT.jar`. Change the working directory to `openscoring-client` and replay the life cycle of a sample `DecisionTreeIris` model (in "REST API", see below) by executing the following sequence of commands:
```
java -cp target/client-executable-1.2-SNAPSHOT.jar org.openscoring.client.Deployer --model http://localhost:8080/openscoring/model/DecisionTreeIris --file DecisionTreeIris.pmml

java -cp target/client-executable-1.2-SNAPSHOT.jar org.openscoring.client.Evaluator --model http://localhost:8080/openscoring/model/DecisionTreeIris -XSepal_Length=5.1 -XSepal_Width=3.5 -XPetal_Length=1.4 -XPetal_Width=0.2

java -cp target/client-executable-1.2-SNAPSHOT.jar org.openscoring.client.CsvEvaluator --model http://localhost:8080/openscoring/model/DecisionTreeIris --input input.csv --output output.csv

java -cp target/client-executable-1.2-SNAPSHOT.jar org.openscoring.client.Undeployer --model http://localhost:8080/openscoring/model/DecisionTreeIris
```

Additionally, this JAR file contains an application class `org.openscoring.client.DirectoryDeployer`, which monitors the specified directory for PMML file addition and removal events:
```
java -cp target/client-executable-1.2-SNAPSHOT.jar org.openscoring.client.DirectoryDeployer --model-collection http://localhost:8080/openscoring/model --dir pmml
```

# REST API #

### Overview

Model REST API endpoints:

| HTTP method | Endpoint | Required role(s) | Description |
| ----------- | -------- | ---------------- | ----------- |
| GET | /model | - | Get the summaries of all models |
| POST | /model | admin | Deploy a model |
| PUT | /model/${id} | admin | Deploy a model |
| GET | /model/${id} | - | Get the summary of a model |
| GET | /model/${id}/pmml | admin | Download a model as a PMML document |
| POST | /model/${id} | - | Evaluate a model in "single prediction" mode |
| POST | /model/${id}/batch | - | Evaluate a model in "batch prediction" mode |
| POST | /model/${id}/csv | - | Evaluate a model is CSV prediction mode |
| DELETE | /model/${id} | admin | Undeploy a model |

Metric REST API endpoints:

| HTTP method | Endpoint | Required role(s) | Description |
| ----------- | -------- | ---------------- | ----------- |
| GET | /metric/model | admin | Get the metrics of all models |
| GET | /metric/model/${id} | admin | Get the metrics of a model |

By default, the "admin" role is granted to all HTTP requests that originate from the local network address.

In case of an error (ie. response status codes 4XX or 5XX), the response body is a JSON serialized form of an `org.openscoring.common.SimpleResponse`  [(source)] (https://github.com/jpmml/openscoring/blob/master/openscoring-common/src/main/java/org/openscoring/common/SimpleResponse.java) object.

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

The response body is a JSON serialized form of an `org.openscoring.common.ModelResponse` [(source)] (https://github.com/jpmml/openscoring/blob/master/openscoring-common/src/main/java/org/openscoring/common/ModelResponse.java) object.

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

### Model querying

##### GET /model

Gets the summaries of all models.

The response body is a JSON serialized form of an `org.openscoring.common.BatchModelResponse` [(source)] (https://github.com/jpmml/openscoring/blob/master/openscoring-common/src/main/java/org/openscoring/common/BatchModelResponse.java) object.

Response status codes:
* 200 OK. The model collection was queried.

Sample cURL invocation:
```
curl -X GET http://localhost:8080/openscoring/model
```

##### GET /model/${id}

Gets the summary of a model.

The response body is a JSON serialized form of an `org.openscoring.common.ModelResponse` [(source)] (https://github.com/jpmml/openscoring/blob/master/openscoring-common/src/main/java/org/openscoring/common/ModelResponse.java) object.

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
	"summary" : "Tree model",
	"schema" : {
		"activeFields" : [
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
				"id" : "Predicted_Species",
				"dataType" : "string",
				"opType" : "categorical"
			},
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

Field definitions are retrieved from the [MiningSchema] (http://www.dmg.org/v4-2-1/MiningSchema.html) and [Output] (http://www.dmg.org/v4-2-1/Output.html) elements of the PMML document. The active and group fields relate to the `arguments` attribute of the evaluation request, whereas the target and output fields relate to the `result` attribute of the evaluation response (see below).

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

Evaluates a model in "single prediction" mode.

The request body is a JSON serialized form of an `org.openscoring.common.EvaluationRequest` [(source)] (https://github.com/jpmml/openscoring/blob/master/openscoring-common/src/main/java/org/openscoring/common/EvaluationRequest.java) object.

The response body is a JSON serialized form of an `org.openscoring.common.EvaluationResponse` [(source)] (https://github.com/jpmml/openscoring/blob/master/openscoring-common/src/main/java/org/openscoring/common/EvaluationResponse.java) object.

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
		"Predicted_Species" : "setosa",
		"Probability_setosa" : 1.0,
		"Probability_versicolor" : 0.0,
		"Probability_virginica" : 0.0,
		"Node_Id" : "2"
	}
}
```

##### POST /model/${id}/batch

Evaluates a model in "batch prediction" mode.

The request body is a JSON serialized form of an `org.openscoring.common.BatchEvaluationRequest` [(source)] (https://github.com/jpmml/openscoring/blob/master/openscoring-common/src/main/java/org/openscoring/common/BatchEvaluationRequest.java) object.

The response body is a JSON serialized form of an `org.openscoring.common.BatchEvaluationResponse` [(source)] (https://github.com/jpmml/openscoring/blob/master/openscoring-common/src/main/java/org/openscoring/common/BatchEvaluationResponse.java) object.

Response status codes:
* 200 OK. The evaluation was successful.
* 400 Bad Request. The evaluation failed permanently due to missing or invalid input data.
* 404 Not Found. The requested model was not found.
* 500 Internal Server Error. The evaluation failed temporarily.

Sample cURL invocation:
```
curl -X POST --data-binary @BatchEvaluationRequest.json -H "Content-type: application/json" http://localhost:8080/openscoring/model/DecisionTreeIris/batch
```

##### POST /model/${id}/csv

Evaluates a model in CSV mode.

The request body is a CSV document (indicated by content-type header `text/plain`). The data table must contain a data column for every active and group field. The ordering of data columns is not significant, because they are mapped to fields by name.

The response body is a CSV document. The data table contains a data column for every target and output field.

The CSV document must conform to Tab-separated values (TSV) dialect or Microsoft Excel dialect.

The first data column can be employed for row identification purposes. It will be copied over from the request data table to the response data table if its name equals to "Id" (the comparison is case insensitive) and the number of rows did not change during the evaluation.

Response status codes:
* 200 OK. The evaluation was successful.
* 400 Bad request. The evaluation failed permanently. The request body is not a valid and/or supported CSV document, or it contains cells with missing or invalid input data.
* 404 Not Found. The requested model was not found.
* 500 Internal Server Error. The evaluation failed temporarily.

Sample cURL invocation:
```
curl -X POST --data-binary @input.csv -H "Content-type: text/plain" http://localhost:8080/openscoring/model/DecisionTreeIris/csv
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
Id,Species,Predicted_Species,Probability_setosa,Probability_versicolor,Probability_virginica,Node_Id
record-001,setosa,setosa,1.0,0.0,0.0,2
record-002,versicolor,versicolor,0.0,0.9074074074074074,0.09259259259259259,6
record-003,virginica,virginica,0.0,0.021739130434782608,0.9782608695652174,7
```

### Model undeployment

##### DELETE /model/${id}

Deletes a model.

Response status codes:
* 204 No Content. The model was deleted.
* 403 Forbidden. The acting user does not have an "admin" role.
* 404 Not Found. The requested model was not found.
* 500 Internal Server Error. The undeployment failed temporarily.

Sample cURL invocation:
```
curl -X DELETE http://localhost:8080/openscoring/model/DecisionTreeIris
```

An HTTP PUT or DELETE method can be masked as an HTTP POST method by using the [HTTP method override mechanism] (https://jersey.java.net/apidocs/latest/jersey/org/glassfish/jersey/server/filter/HttpMethodOverrideFilter.html).

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

Gets the snapshot of the metrics of a model.

The response body is a JSON serialized form of a `com.codahale.metrics.MetricRegistry` object.

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
	"gauges" : { },
	"counters" : {
		"records" : {
			"count" : 1
		}
	},
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

Openscoring is dual-licensed under the [GNU Affero General Public License (AGPL) version 3.0] (http://www.gnu.org/licenses/agpl-3.0.html) and a commercial license.

# Additional information #

Please contact [info@openscoring.io] (mailto:info@openscoring.io)