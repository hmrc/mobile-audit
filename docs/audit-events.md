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

The body should be an array of audit messages. Each of the messages will be forwarded to the platform audit service. 

The NINO of the user who is logged-in to the app will be added to each audit message before it is sent to the platform audit service.

```json

```

* **Success Response:**

  * **Code:** 204 


