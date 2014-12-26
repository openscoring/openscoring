Openscoring [![Build Status](https://travis-ci.org/jpmml/openscoring.png?branch=master)](https://travis-ci.org/jpmml/openscoring)
===========

REST web service for scoring PMML models.

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

# Installation #

The project requires Java 1.7 or newer to run.

Enter the project root directory and build using [Apache Maven] (http://maven.apache.org/):
```
mvn clean install
```

The build produces an executable uber-JAR file `openscoring-server/target/server-executable-1.2-SNAPSHOT.jar`. The main class of the Openscoring application `org.openscoring.server.Main` can be automatically loaded and executed by specifying the `-jar` command-line option:
```
java -jar server-executable-1.2-SNAPSHOT.jar
```

By default, the REST web service is started at [http://localhost:8080/openscoring] (http://localhost:8080/openscoring/). The main class accepts a number of configuration options for URI customization and other purposes. Please specify `--help` for more information.

Additionally, the build produces an executable uber-JAR file `openscoring-client/target/client-executable-1.2-SNAPSHOT.jar` which contains a number of command-line client applications.

# REST API #

### Overview

Model collection REST API endpoints:

| HTTP method | Endpoint | Required role(s) | Description |
| ----------- | -------- | ---------------- | ----------- |
| GET | /model | - | Get all models |
| GET | /model/metrics | admin | Get the metrics of all models |
| POST | /model | admin | Deploy a model |

Model REST API endpoints:

| HTTP method | Endpoint | Required role(s) | Description |
| ----------- | -------- | ---------------- | ----------- |
| PUT | /model/${id} | admin | Deploy a model |
| GET | /model/${id} | admin | Download a model |
| GET | /model/${id}/metrics | admin | Get the metrics of a model |
| GET | /model/${id}/schema | - | Get the data schema information of a model |
| POST | /model/${id} | - | Evaluate a model in "single prediction" mode |
| POST | /model/${id}/batch | - | Evaluate a model in "batch prediction" mode |
| POST | /model/${id}/csv | - | Evaluate a model is CSV prediction mode |
| DELETE | /model/${id} | admin | Undeploy a model |

Some REST API endpoints require privileged access. By default, the Openscoring application grants the "admin" role to all HTTP requests that originate from the local network address.

### Model collection querying

##### GET /model

Gets the list of all models.

The response body is a JSON serialized form of a list of `org.openscoring.common.ModelResponse` objects.

Sample cURL invocation:
```
curl -X GET http://localhost:8080/openscoring/model
```

### Model deployment

##### PUT /model/${id}

Creates or updates a model.

The request body is a PMML document (indicated by content-type header `text/xml` or `application/xml`).

The response body is a JSON serialized form of an `org.openscoring.common.ModelResponse` object that represents the current state of the model.

Response status codes:
* 200 OK. The model was updated.
* 201 Created. A new model was created.
* 400 Bad Request. The request body is not a valid and/or supported PMML document.

Sample cURL invocation:
```
curl -X PUT --data-binary @DecisionTreeIris.pmml -H "Content-type: text/xml" http://localhost:8080/openscoring/model/DecisionTreeIris
```

The example PMML file `DecisionTreeIris.pmml` along with example JSON and CSV files is available in the `openscoring-server/etc` directory.

Sample response:
```json
{
	"id" : "DecisionTreeIris",
	"summary" : "Tree model"
}
```

### Model querying

##### GET /model/${id}

Downloads a model.

The response body is a PMML document.

Sample cURL invocation:
```
curl -X GET http://localhost:8080/openscoring/model/DecisionTreeIris
```

##### GET /model/${id}/metrics

Takes a snapshot of the metrics of a model.

The response body is a JSON serialized form of a 'com.codahale.metrics.MetricRegistry' object.

Sample cURL invocation:
```
curl -X GET http://localhost:8080/openscoring/model/DecisionTreeIris/metrics
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

##### GET /model/${id}/schema

Gets the data schema information of a model.

The response body is a JSON serialized form of an `org.openscoring.common.SchemaResponse` object.

Field definitions are retrieved from the [Mining Schema element] (http://www.dmg.org/v4-2/MiningSchema.html) of the PMML document. The active and group fields relate to the `arguments` attribute of the evaluation request, whereas the target and output fields relate to the `result` attribute of the evaluation response (see below).

Sample cURL invocation:
```
curl -X GET http://localhost:8080/openscoring/model/DecisionTreeIris/schema
```

Sample response:
```json
{
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
```

### Model evaluation

##### POST /model/${id}

Evaluates a model in "single prediction" mode.

The request body is a JSON serialized form of an `org.openscoring.common.EvaluationRequest` object.

The response body is a JSON serialized form of an `org.openscoring.common.EvaluationResponse` object.

Response status codes:
* 200 OK. The evaluation was successful.
* 404 Not Found. The requested model was not found.
* 500 Internal Server Error. The evaluation failed. This is most likely caused by missing or invalid input data.

Sample cURL invocation:
```
curl -X POST --data-binary @EvaluationRequest.json -H "Content-type: application/json" http://localhost:8080/openscoring/model/DecisionTreeIris
```

Sample request:
```json
{
	"id" : "example-001",
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
	"id" : "example-001",
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

The request body is a JSON serialized form of a list of `org.openscoring.common.EvaluationRequest` objects. The number of list elements is not restricted.

The response body is a JSON serialized form of a list of `org.openscoring.common.EvaluationResponse` objects.

Sample cURL invocation:
```
curl -X POST --data-binary @BatchEvaluationRequest.json -H "Content-type: application/json" http://localhost:8080/openscoring/model/DecisionTreeIris/batch
```

##### POST /model/${id}/csv

Evaluates a model in CSV mode.

The request body is a CSV document (indicated by content-type header `text/plain`). The data table must contain a data column for every active and group field. The ordering of data columns is not significant. They are mapped to fields by name.

The CSV document must conform to Tab-separated values (TSV) dialect or Microsoft Excel dialect.

The response body is a CSV document. The data table contains a data column for every target and output field.

The first data column can be employed for row identification purposes. It will be copied over from the request data table to the response data table if its name equals to "Id" (the comparison is case insensitive) and the number of rows did not change during the evaluation.

Response status codes:
* 200 OK. The evaluation was successful.
* 400 Bad request. The request body is not a valid and/or supported CSV document.
* 404 Not Found. The requested model was not found.
* 500 Internal Server Error. The evaluation failed. This is most likely caused by missing or invalid input data.

Sample cURL invocation:
```
curl -X POST --data-binary @input.csv -H "Content-type: text/plain" http://localhost:8080/openscoring/model/DecisionTreeIris/csv
```

Sample request:
```
Id,Sepal_Length,Sepal_Width,Petal_Length,Petal_Width
example-001,5.1,3.5,1.4,0.2
example-002,7,3.2,4.7,1.4
example-003,6.3,3.3,6,2.5
```

Sample response:
```
Id,Species,Predicted_Species,Probability_setosa,Probability_versicolor,Probability_virginica,Node_Id
example-001,setosa,setosa,1.0,0.0,0.0,2
example-002,versicolor,versicolor,0.0,0.9074074074074074,0.09259259259259259,6
example-003,virginica,virginica,0.0,0.021739130434782608,0.9782608695652174,7
```

### Model undeployment

##### DELETE /model/${id}

Deletes a model.

Response status codes:
* 204 No Content. The model was deleted.
* 404 Not Found. The requested model was not found.

Sample cURL invocation:
```
curl -X DELETE http://localhost:8080/openscoring/model/DecisionTreeIris
```

# Command-line client applications #

The following sequence of commands replays the life cycle of a model `DecisionTreeIris`:
```
java -cp client-executable-1.2-SNAPSHOT.jar org.openscoring.client.Deployer --model http://localhost:8080/openscoring/model/DecisionTreeIris --file DecisionTreeIris.pmml

java -cp client-executable-1.2-SNAPSHOT.jar org.openscoring.client.Evaluator --model http://localhost:8080/openscoring/model/DecisionTreeIris -XSepal_Length=5.1 -XSepal_Width=3.5 -XPetal_Length=1.4 -XPetal_Width=0.2

java -cp client-executable-1.2-SNAPSHOT.jar org.openscoring.client.CsvEvaluator --model http://localhost:8080/openscoring/model/DecisionTreeIris --input input.csv --output output.csv

java -cp client-executable-1.2-SNAPSHOT.jar org.openscoring.client.Undeployer --model http://localhost:8080/openscoring/model/DecisionTreeIris
```

# License #

Openscoring is dual-licensed under the [GNU Affero General Public License (AGPL) version 3.0] (http://www.gnu.org/licenses/agpl-3.0.html) and a commercial license.

# Additional information #

Please contact [info@openscoring.io] (mailto:info@openscoring.io)