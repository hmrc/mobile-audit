### What is this API for?

The API provides endpoints to allow the mobile applications to send audit events to the platform.

## Event structure

The client will send events as json objects containing the following elements:

 *Name* | *Optional* | *Description* |
|--------|----|----|
| `auditType` | no | This will be used as the audit type of the event sent to datastream. |
| `generatedAt` | yes | An ISO-8601-formatted date string. If this is not present, the system will use the date/time that the event was received. |
| `transactionName` | yes | If this is not present then a default value of "explicitAuditEvent" will be used. This value will be added to the set of tags passed to datastream. |
| `path` | yes | If this is not present then the value of the `auditType` element will be use. This value will be added to the set of tags passed to datastream. |
| `detail` | yes | A map of extra details that will be included in the event sent to datastream. All the values in the map must be strings. |

The service will generate an id for the datastream event and add the standard headers from the request to the tags.

The api calls are "fire-and-forget". They will always return `204 No Body` responses even if some error occurred trying to forward the event, since there is no
reasonable action the client can take if they fail.