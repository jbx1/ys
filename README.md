# Yield Street - Take Home Challenge

## User Accreditation API

This project provides the implementation needed for the Yieldstreet Take Home challenge.

## Quickstart

### Pre-requisites:

- Java 17
- Maven 3.6 or later
- Docker and docker-compose version 20.10

### Build and Run

From the root folder of the repository:

1. Build the project

`cd accreditation-api`

`mvn clean install`

2. Run the service together with its dependencies (Kafka, Zookeeper and Postgresql)

`cd ..`

`chmod 755 *.sh`

`./start.sh`

**It takes a couple of minutes for all the services to start up. Please be patient!**

3. Once all the services are up and running, you will be able to see the OpenAPI documentation on:

http://localhost:9999/swagger-ui/index.html

4. You can also see the logs of the accreditation-api service by typing:

`./logs.sh`

Note that Kafka takes longer to start than the actual API service itself. For this reason you might see the service
trying to establish a connection and failing repeatedly for a number of seconds until it establishes the connection
successfully.

5. To stop the services you can use the script:

`./stop.sh`

### Running it locally without Docker

The build script generates an executable JAR file at: `~/accreditation-api/target/accreditation-api-1.0-SNAPSHOT.jar`
This is a "fat jar" containing all dependencies inside it, and can be executed with `java -jar`.

A script has also been included to start the API in this mode if required, at the root of the repository.
It includes all the respective environment variables to connect to the same services (Kafka and PostgreSQL) as defined
in the docker-compose definition. If you want to change any configuration, such as IP addresses or ports, you can do it in that by editing the file.

`./run-local.sh`

Note that in this case it is configured to listen on port 9998 (not 9999) so that it does not conflict with the other
API service exposed through docker-compose. Again, you can change it by editing `run-local.sh`

You can test whether it is running by going to:
http://localhost:9998/swagger-ui/index.html

### Technologies Used

- OpenAPI 3.0
- Spring Boot as the main IoC framework.
- Spring Data and JPA to write to the database.
- FlywayDB for database migration scripts.
- PostgreSQL as the relational database.
- Apache Kafka for the audit trail.

## Answers to Questions

### a) Architectural Patterns

The REST API is implemented as a stateless microservice. It is built on Spring Boot, and uses a Postgresql database to
determine the current state
of each accreditation request. Each request that updates the database is also sent to a Kafka topic to keep a full
history of the changes.

The REST API was designed in a Contract First fashion, starting from the Open API definition and using the OpenAPI
Codegen Maven Plugin to
generate the classes representing the requests and responses. Basic syntactical validations are also enforced
declaratively at this level,
such as formats of UUIDs, regular expressions for MIME type strings, and non-empty strings for user IDs, document name
and content.
This approach ensures that there is a single version of truth for the API, including up-to-date documentation and
validations.

The admin endpoints are implemented by a separate Controller from that of the user endpoint. This separation of concerns
makes the distinction of
responsibilities clearer, and it would be easier to make specific security checks at controller level, reflecting these
roles.

The business logic is then implemented into two separate services, one for admin and one for the user. Each service
makes use of JPA entities and the associated repositories to.

Each admin operation is implemented within one database transaction boundary. This means that if anything goes wrong
midway, including sending the message to the Kafka topic, the transaction would be rolled back.

The scheduled task that changes old Confirmed requests to Expired is implemented as a Spring Boot scheduled task. The
updates resulting from this
expiration process are also sent over to the Kafka topic. So, by replaying the history of messages, one can rebuild an
exact replica of the database.

### b) Concurrent updates to accreditation status

Updates to the database are performed within the database transaction boundary. The default configuration of PostgreSQL
uses a READ COMMITTED transaction isolation method. This means that if there are two parallel transactions on the same
record, only one will go through while the other waits until the data is committed. We take advantage of this behaviour
in the following ways:

1. We send the Kafka message within the database transaction, and commit after it is sent. This way if sending to the
   Kafka topic fails, we will be able to roll back the transaction.
2. We use a Stamped Lock approach, such that the WHERE clause of the UPDATE statement also includes the last known
   update timestamp (the stamp) of the record.
   If this stamp changes, it means that the record was updated concurrently in the short timeframe between the start of
   transaction and the update statement. In this case the update statement would not match and the operation stops
   there.
3. As part of the query outlined in point 2, to get the details of an accreditation request, we also check the current
   status of the accreditation request and compare it with the new one. If they are the same (potentially because
   another admin updated it), no change is needed, and we treat it as an idempotent request (returning the same result).

### c) Scaling of the microservice

1. The API is already designed to operate in a horizontally scalable architecture. Since the service itself is
   self-contained and stateless, more instances of it can be created.

2. The User API (which only performs queries) is intentionally segregated from the Admin API implementation. This way,
   if necessary, it can be moved to its own process and scaled accordingly. This follows the same paradigm of the
   Command Query Responsibility Segregqation (CQRS) model, where commands (updates to the data) are handled separately
   from queries.

3. To a certain extent, local caching (such as Ehcache) within the User API can also be used, to avoid hitting the
   database repeatedly until there really is an update. The process hosting the User API can be a consumer for the same
   Kafka topic,
   receiving update notifications, to know when to invalidate the cache and reload a fresh version of the data from the
   database.

4. If the database itself becomes a bottleneck, another step could be to connect User API instances to read-only
   secondary replicas, leaving the primary server for the updates needed by the Admin API. Alternatively one could
   consider changing the database to an Eventual Consistent implementation, such as Cassandra. In this case, the data
   can be sharded easily based on the User ID. 




