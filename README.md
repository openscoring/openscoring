# Getting started

Check out the codebase and build the project using Apache Maven2:
```
mvn clean package
```

Change the working directory to `server` and start the embedded Jetty web server:
```
mvn jetty:run
```

Congratulations! You now have a local openscoring web service running at: http://localhost:8080/openscoring/

# REST API

### PUT - Deploy a model

Deploy the contents of the PMML file `helloworld.pmml` as a model `helloworld`:
```
curl -X PUT --data-binary @helloworld.pmml -H "Content-type: text/xml" http://localhost:8080/openscoring/model/helloworld
```

### POST - Perform the evaluation

Send the contents of the file JSON file `helloworld.json` for evaluation to the model `helloworld`:
```
curl -X POST --data-binary @helloworld.json -H "Content-type: application/json" http://localhost:8080/openscoring/model/helloworld
```

The request body is the JSON serialized form of an `org.openscoring.server.ModelRequest` object:
```
{
	"parameters" :
		{
			"key" : "value"
		}
}
```

The response body is the JSON serialized form of an `org.openscoring.server.ModelResponse` object:
```
{
	"result" : "Hello World!"
}
```

### DELETE - Undeploy a model

Undeploy the model `helloworld`:
```
curl -X DELETE http://localhost:8080/openscoring/model/helloworld
```