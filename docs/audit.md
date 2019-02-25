audit
----
Send an audit message to the platform.
  
* **URL**

  `/mobile-audit`

* **Method:**
  
  `POST`

*  **URL Params**

   **Optional:**
 
   `journeyId=[journeyId]`
   
   The journey Id may be supplied for logging and diagnostic purposes.
     
*  **JSON**

The contents of the audit message. The NINO of the user who is logged-in to the app will be added to the audit message before it is sent
to the platform audit service.

```json

```

* **Success Response:**

  * **Code:** 204 


