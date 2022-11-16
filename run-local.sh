#!/bin/bash
echo "Starting up accreditations-api locally ..."

export DB_HOST=localhost
export DB_PORT=5432
export DB_USERNAME=yieldstreet
export DB_PASSWORD=password
export KAFKA_BROKERS=localhost:9093
export KAFKA_TOPIC=accreditations
export HTTP_PORT=9998

java -jar ./accreditation-api/target/accreditation-api-1.0-SNAPSHOT.jar

