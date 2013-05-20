# Getting started

Build the project using Apache Maven2:
```
mvn clean package
```

Change the working directory to `server` and start the embedded Jetty web server:
```
mvn jetty:run
```

Congratulations! You now have an openscoring web service running at [localhost:8080] (http://localhost:8080/openscoring/)

# REST API

### PUT - Deploy a model

Deploy the contents of the PMML file `DecisionTreeIris.pmml` as a model `DecisionTreeIris`:
```
curl -X PUT --data-binary @DecisionTreeIris.pmml -H "Content-type: text/xml" http://localhost:8080/openscoring/model/DecisionTreeIris
```

For a list of sample PMML files please take a look at [JPMML R/Rattle support module] (https://github.com/jpmml/jpmml/tree/master/pmml-rattle/src/test/resources/pmml) or [JPMML KNIME support module] (https://github.com/jpmml/jpmml/tree/master/pmml-knime/src/test/resources/pmml).

### POST - Perform the evaluation

Send the contents of the file JSON file `EvaluationRequest.json` for evaluation to the model `DecisionTreeIris`:
```
curl -X POST --data-binary @EvaluationRequest.json -H "Content-type: application/json" http://localhost:8080/openscoring/model/DecisionTreeIris
```

The request body is the JSON serialized form of an `org.openscoring.common.EvaluationRequest` object:
```
{
	"parameters" : {
		"Sepal.Length" : "5.1",
		"Sepal.Width" : "3.5",
		"Petal.Length" : "1.4",
		"Petal.Width" : "0.2"
	}
}
```

The response body is the JSON serialized form of an `org.openscoring.common.EvaluationResponse` object:
```
{
	"result" :
		{
			"Species" : "setosa"
		}
}
```

### DELETE - Undeploy a model

Undeploy the model `DecisionTreeIris`:
```
curl -X DELETE http://localhost:8080/openscoring/model/DecisionTreeIris
```