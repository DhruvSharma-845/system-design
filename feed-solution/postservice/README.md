A service that handles post-related functionalities:

- Post Submission

# Local Deployment Steps

1. Add Java 21 in the path  
   i. MAC:
   ```
   export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home
   ```
2. Run the server  
   i. Using maven wrapper

   ```
   ./mvnw spring-boot:run
   ```

3. To call REST APIs from client, expose the service

```
kubectl -n feed port-forward svc/postservice 8080:8080
```
