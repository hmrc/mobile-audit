
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


### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
