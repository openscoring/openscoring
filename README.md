# Getting started

##### Build and installation

Get the source code from the project page. Enter the root directory and build using [Apache Maven] (http://maven.apache.org/):
```
mvn clean install
```

##### Running in Maven mode (test)

Change the working directory to `server` and start the embedded Jetty web server:
```
mvn jetty:run
```

This will start the openscoring web service at [localhost:8080] (http://localhost:8080/openscoring/)

##### Running in standalone mode (production)

The deployable WAR file `server-1.0-SNAPSHOT.war` is located in build directory `server/target`. Among other deployment options, this file can be directly executed using Jetty Runner.

Download the latest version of Jetty Runner from the [Maven Central Repository] (http://repo1.maven.org/maven2/org/eclipse/jetty/jetty-runner/). Currently, this is the version `9.0.3.v20130506`. Execute the Jetty Runner JAR file by first specifying the web application context path `/openscoring` and second the name of the WAR file:
```
java -jar jetty-runner-9.0.3.v20130506.jar --path /openscoring server-1.0-SNAPSHOT.war
```

Again, this will start the openscoring web service at [localhost:8080] (http://localhost:8080/openscoring).

Please see the [Jetty Runner documentation] (http://www.eclipse.org/jetty/documentation/current/jetty-runner.html) for more configuration options such as using the secure HTTPS connector.

# REST API

### PUT - Deploy a model

Deploy the contents of the PMML file `DecisionTreeIris.pmml` as a model `DecisionTreeIris`:
```
curl -X PUT --data-binary @DecisionTreeIris.pmml -H "Content-type: text/xml" http://localhost:8080/openscoring/model/DecisionTreeIris
```

For a list of sample PMML files please take a look at [JPMML R/Rattle support module] (https://github.com/jpmml/jpmml/tree/master/pmml-rattle/src/test/resources/pmml) or [JPMML KNIME support module] (https://github.com/jpmml/jpmml/tree/master/pmml-knime/src/test/resources/pmml).

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

##### Get the description of a deployed model

Obtain the description of the public interface of the model `DecisionTreeIris`:
```
curl -X GET http://localhost:8080/openscoring/model/DecisionTreeIris
```

The response body is the JSON serialized form of an `org.openscoring.common.SummaryResponse` object:
```
{
	"activeFields" : ["Sepal.Length", "Sepal.Width", "Petal.Length", "Petal.Width"],
	"predictedFields" : ["Species"],
	"outputFields" : ["Predicted_Species", "Probability_setosa", "Probability_versicolor", "Probability_virginica"]
}
```

Field definitions are retrieved from the [Mining Schema element] (http://www.dmg.org/v4-1/MiningSchema.html) of the PMML document. The execution fails with HTTP status code "500 Internal Server Error" if the PMML document contains an unsupported model type.

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
	"parameters" : {
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
	"result" :
		{
			"Species" : "setosa",
			"Predicted_Species" : "setosa",
			"Probability_setosa" : 1.0,
			"Probability_versicolor" : 0.0,
			"Probability_virginica" : 0.0
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

### DELETE - Undeploy a model

Undeploy the model `DecisionTreeIris`:
```
curl -X DELETE http://localhost:8080/openscoring/model/DecisionTreeIris
```