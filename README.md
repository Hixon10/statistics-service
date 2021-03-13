# Description
This application calculates transaction statistics for last 60 sec. 

## Usage
1. Save transaction:
```
curl --request POST \
  --url http://localhost:8080/transactions \
  --header 'Content-Type: application/json' \
  --data '{
	"amount": 12.3,
	"timestamp": 1615483871859
}'
```

2. Get statistics
```
curl --request GET \
  --url http://localhost:8080/statistics
```

## Design assumptions
1. We can return stale statistics.
2. We cannot receive a transaction with timestamp from the future.

## Requirements
1. Java >11;
2. Gradle >6.8.*.

## How to build image
1. `./gradlew clean test bootBuildImage`

## How to run application
1. `docker run -it -p 8080:8080 statistics-service:0.0.1-SNAPSHOT`
