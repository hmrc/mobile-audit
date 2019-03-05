audit
----
Send an audit message to the platform.
  
* **URL**

  `/mobile-audit/audit-events`

* **Method:**
  
  `POST`

*  **URL Params**

   **Optional:**
 
   `journeyId=[journeyId]`
   
   The journey Id may be supplied for logging and diagnostic purposes.
     
*  **JSON**

The body should be an object containing an array of audit messages. Each of the messages will be forwarded to the platform audit service. 

The NINO of the user who is logged-in to the app will be added to each audit message before it is sent to the platform audit service.

```json
{
  "events": [
      {
        "auditType": "externalAuditEvent",
        "generatedAt": "2019-03-05T11:03:30.825Z",
        "transactionName": "transaction",
        "path": "audit-path",
        "detail": {
          "extraDetail1": "value 1",
          "extraDetail2": "value 2"
        }
      },{
        "auditType": "externalAuditEvent",
        "generatedAt": "2019-03-05T11:03:30.825Z",
        "transactionName": "transaction",
        "path": "audit-path",
        "detail": {
          "extraDetail1": "value 1",
          "extraDetail2": "value 2"
        }
      }
  ]
}
```

* **Success Response:**

  * **Code:** 204 


