Openscoring [![Build Status](https://travis-ci.org/jpmml/openscoring.png?branch=master)](https://travis-ci.org/jpmml/openscoring)
===========

REST web service for scoring PMML models.

# Installation #

Enter the project root directory and build using [Apache Maven] (http://maven.apache.org/):
```
mvn clean install
```

The build produces an executable uber-JAR file `server/target/server-executable-1.1-SNAPSHOT.jar`. The main class of the Openscoring application `org.openscoring.server.Main` can be automatically loaded and executed by specifying the `-jar` command-line option:
```
java -jar server-executable-1.1-SNAPSHOT.jar
```

By default, the REST web service is started at [http://localhost:8080/openscoring] (http://localhost:8080/openscoring/). The main class accepts a number of configuration options for URI customization and other purposes. Please specify `--help` for more information.

Additionally, the build produces an executable uber-JAR file `client/target/client-executable-1.1-SNAPSHOT.jar` which contains a number of command-line client applications.

# REST API #

Methods that modify the state of the model registry (i.e. PUT and DELETE) require the "admin" role. By default, the Openscoring application grants this role to all HTTP requests that originate from a local network address.

### PUT - Deploy a model

Deploy the contents of the PMML file `DecisionTreeIris.pmml` as a model `DecisionTreeIris`:
```
curl -X PUT --data-binary @DecisionTreeIris.pmml -H "Content-type: text/xml" http://localhost:8080/openscoring/model/DecisionTreeIris
```

The example PMML file `DecisionTreeIris.pmml` along with example JSON and CSV files is available in the `server/etc` directory.

### GET - Obtain model information

##### Get the list of deployed models

Obtain the list of deployed models:
```
curl -X GET http://localhost:8080/openscoring/model
```

The response body is the JSON serialized form of a list of model identifiers:
```
[
	"DecisionTreeIris"
]
```

##### Get the summary of a deployed model

Obtain the description of the "public interface" of the model `DecisionTreeIris`:
```
curl -X GET http://localhost:8080/openscoring/model/DecisionTreeIris
```

The response body is the JSON serialized form of an `org.openscoring.common.SummaryResponse` object:
```
{
	"activeFields" : ["Sepal.Length", "Sepal.Width", "Petal.Length", "Petal.Width"],
	"groupFields" : []
	"targetFields" : ["Species"],
	"outputFields" : ["Predicted_Species", "Probability_setosa", "Probability_versicolor", "Probability_virginica", "Node_Id"]
}
```

Field definitions are retrieved from the [Mining Schema element] (http://www.dmg.org/v4-1/MiningSchema.html) of the PMML document. The execution fails with HTTP status code "500 Internal Server Error" if the PMML document contains an unsupported model type.

##### Get the metrics of a deployed model

Obtain the metrics of the model `DecisionTreeIris`:
```
curl -X GET http://localhost:8080/openscoring/model/DecisionTreeIris/metrics
```

The metrics are implemented using the [Coda Hale Metrics] (http://metrics.codahale.com/) library. The response body is the JSON serialized form of an `com.codahale.metrics.MetricRegistry` object:
```
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

### POST - Perform model evaluation

The evaluation can be performed either in single prediction mode or in batch prediction mode (see below). On average, the batch prediction mode is expected to provide better throughput.

##### Single prediction mode

Send the contents of the JSON file `EvaluationRequest.json` for evaluation to the model `DecisionTreeIris`:
```
curl -X POST --data-binary @EvaluationRequest.json -H "Content-type: application/json" http://localhost:8080/openscoring/model/DecisionTreeIris
```

The request body is the JSON serialized form of an `org.openscoring.common.EvaluationRequest` object:
```
{
	"id" : "example-001",
	"arguments" : {
		"Sepal.Length" : 5.1,
		"Sepal.Width" : 3.5,
		"Petal.Length" : 1.4,
		"Petal.Width" : 0.2
	}
}
```

The response body is the JSON serialized form of an `org.openscoring.common.EvaluationResponse` object:
```
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

##### Batch prediction mode

Send the contents of the JSON file `BatchEvaluationRequest.json` for evaluation to the model `DecisionTreeIris` (please note `/batch` at the end of the URL):
```
curl -X POST --data-binary @BatchEvaluationRequest.json -H "Content-type: application/json" http://localhost:8080/openscoring/model/DecisionTreeIris/batch
```

The request body is the JSON serialized form of a list of `org.openscoring.common.EvaluationRequest` objects. The number of list elements is not restricted.

The response body is the JSON serialized form of a list of `org.openscoring.common.EvaluationResponse` objects.

##### CSV prediction mode

Send the contents of the CSV file `input.csv` for evaluation to model `DecisionTreeIris` (please note `/csv` at the end of the path component of the URL):
```
curl -X POST --data-binary @input.csv -H "Content-type: text/plain" "http://localhost:8080/openscoring/model/DecisionTreeIris/csv?idColumn=Id"
```

The request body is a CSV document containing active fields:
```
Id,Sepal.Length,Sepal.Width,Petal.Length,Petal.Width
example-001,5.1,3.5,1.4,0.2
example-002,7,3.2,4.7,1.4
example-003,6.3,3.3,6,2.5
```

The response body is a CSV document containing target and output fields:
```
Id,Species,Predicted_Species,Probability_setosa,Probability_versicolor,Probability_virginica,Node_Id
example-001,setosa,setosa,1.0,0.0,0.0,2
example-002,versicolor,versicolor,0.0,0.9074074074074074,0.09259259259259259,6
example-003,virginica,virginica,0.0,0.021739130434782608,0.9782608695652174,7
```

### DELETE - Undeploy a model

Undeploy the model `DecisionTreeIris`:
```
curl -X DELETE http://localhost:8080/openscoring/model/DecisionTreeIris
```

# Command-line client applications #

The following sequence of commands replays the life cycle of a model `DecisionTreeIris`:
```
java -cp client-executable-1.1-SNAPSHOT.jar org.openscoring.client.Deployer --model http://localhost:8080/openscoring/model/DecisionTreeIris --file DecisionTreeIris.pmml

java -cp client-executable-1.1-SNAPSHOT.jar org.openscoring.client.Evaluator --model http://localhost:8080/openscoring/model/DecisionTreeIris -XSepal.Length=5.1 -XSepal.Width=3.5 -XPetal.Length=1.4 -XPetal.Width=0.2

java -cp client-executable-1.1-SNAPSHOT.jar org.openscoring.client.CsvEvaluator --model http://localhost:8080/openscoring/model/DecisionTreeIris --input input.csv --output output.csv --id-column Id

java -cp client-executable-1.1-SNAPSHOT.jar org.openscoring.client.Undeployer --model http://localhost:8080/openscoring/model/DecisionTreeIris
```

# License #

Openscoring is dual-licensed under the [GNU Affero General Public License (AGPL) version 3.0] (http://www.gnu.org/licenses/agpl-3.0.html) and a commercial license.

# Additional information #

Please contact [info@openscoring.io] (mailto:info@openscoring.io)