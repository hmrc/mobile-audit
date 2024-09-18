
# mobile-audit

This provides an api that the mobile apps can use to send audit messages to the platform.

(This service replaced functionality that used to be provided by `native-apps-api-orchestration`)

The following services are exposed:

API
---

| *Task* | *Supported Methods* | *Description* |
|--------|----|----|
| ```/audit-event``` | POST | Send a single audit message to the platform. [More...](docs/audit-event.md) |
| ```/audit-events``` | POST | Send several audit messages to the platform. [More...](docs/audit-events.md) |


## Development Setup
- Run locally: `sbt run` which runs on port `8252` by default
- Run with test endpoints: `sbt 'run -Dplay.http.router=testOnlyDoNotUseInAppConf.Routes'`

##  Service Manager Profiles
The service can be run locally from Service Manager, using the following profiles:

| Profile Details                | Command                                                                                                           |
|--------------------------------|:------------------------------------------------------------------------------------------------------------------|
| MOBILE_AUDIT_ALL               | sm2 --start MOBILE_AUDIT_ALL                                                                               |


## Run Tests
- Run Unit Tests:  `sbt test`
- Run Integration Tests: `sbt it:test`
- Run Unit and Integration Tests: `sbt test it:test`
- Run Unit and Integration Tests with coverage report: `sbt clean compile coverage test it:test coverageReport dependencyUpdates`


### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
