audit
----
Send an audit message to the platform.
  
* **URL**

  `/mobile-audit/audit-event`

* **Method:**
  
  `POST`

*  **URL Params**

   **Required:**
 
   `journeyId=[journeyId]`
   
   The journey Id is supplied for logging and diagnostic purposes.
     
*  **JSON**

The contents of the audit message. The message will be forwarded to the platform audit service. 

The NINO of the user who is logged-in to the app will be added to the audit message before it is sent
to the platform audit service.

```json
{
  "auditType": "externalAuditEvent",
  "generatedAt": "2019-03-05T11:03:30.825Z",
  "transactionName": "transaction",
  "path": "audit-path",
  "detail": {
    "extraDetail1": "value 1",
    "extraDetail2": "value 2"
  }
}
```

* **Success Response:**

  * **Code:** 204 


