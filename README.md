# Textmining Public API

## Local Installation and Deployment

### Prerequisites

- [Textmining Utility][1]

### Logging

For logging to work, you need to set the following JVM options:

`HOSTNAME` and `LOGPATH`

as their values are used to determine the path of the log files "LOGPATH/logs/textmining_api_public-HOSTNAME.log".

### MongoDB

1. Provide the name of the MongoDB users collection that you created while installing the [Textmining Utility][1] in the `application.properties` file as follows:   
   `mongoDb.usersCollection=tm_users`
2. Insert a user into MongoDB the users collection:
   ```javascript
   db.getCollection("tm_users").insertOne({
   _id: "6296246134cb88685f3846d4",
   username: "root",
   password: "$2y$10$B9gMH2qcoOMhUGGiDN3ecev02zTz3fLjcW3h7fg0vejr1E2NbtZ/q",
   headerValue: "cm9vdDpyb290",
   })
   ```
   
#### Notes:    
1. The password is `root` and the header value is `root:root` encoded in Base64.
2. You'll have to provide the header value in the `Authorization` header of the requests that you send to the APIs:
![img.png](Basic%20Autorization%20Header.png)

### Starting the Project

Since this is a Spring Boot project, you can start it by running the main method of the `TextminingApiPublicApplication` class.


[1]: https://gitlab.ebi.ac.uk/literature-services/public-projects/textmining-utility