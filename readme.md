### Solving the functional requirements

- Implemented a new controller, service and repository for the transactions.
- The controller exposes one endpoint to create transfers. The transfer is treated like a resource that will be created.
- The transfer payload is immediately validated by javax validators, so if it is invalid (e.g. zero or negative amount, null account Ids...) the request is immediately denied.
- Before we do any actions, we check that the from and to accounts aren't the same and that both exist.
- When creating a transfer it gets saved on the database with a status pending.
- This is to allow the recording of all transfers (so they can be audited); and also, in case the microservice crashes before the money is actually transferred, then the transfer is not lost, and can be picked up later.
- After registering the transfer in the database, we lock both accounts to make the query to modify the balances.
- This query is supposed to increment and decrement on both accounts, and change the status of the transaction to completed within the same database transaction.
- We take advantage of the database to guarantee acidity on that transaction, so all of those actions will be committed as one, or none at all.
- We don't run the risk of the microservice crashing in between those actions and leaving the accounts in an inconsistent state.
- In case the query tries to violate any constraint (e.g. the account balance should be greater than 0), then none of the changes are committed. An exception will be thrown, which we catch on our code. In that case we set the transfer status to failed. And we throw another exception to warn the user it wasn't successful.
- It might happen the microservice crashes before setting the transfer status to failed. In that case it will be left pending and the microservice will try again later. No harm done whether it still hits the same constraint violation or if it's successful.
- There is also an ApplicationRunner to check on startup for pending transactions, and process them. Right now, with the in-memory database, it's never going to find any pending transactions on startup. I just wanted to show how it I imagined it.

### Good practises and testing

- To process a transfer we need to lock simultaneously two objects. This can lead to a deadlock because the order they get synchronized depends on the transfer.
- To avoid deadlocks I added an arbitration lock. A thread needs to acquire this lock before acquiring the locks of any accounts. This synchronizes only the action of locking the accounts (removing the possibility of a deadlock), but still allowing multiple threads to process transfers (as long as they involve different accounts altogether).
- Besides clean code, there are a few rules I like to follow, and any deviations from this will need to be well justified:
  - Each controller only owns one resource
  - Each controller only calls one service
  - Each service only calls one repository
  - If a service needs data from another repository then it gets it through the service that owns it.
- The domain entities should never be exposed, and we should use DTOs for transporting data in and out of our system. The Account object was already being exposed and I just let it be, but I created a DTO for the transfer.
- Unit tests were implemented for the service (not for the in-memory database). They should run in isolation, therefore use mocks for everything except the class under test.
- Unit tests also follow a few rules:
  - Mock objects always have the Mock suffix
  - The test object is always called testObj
  - The result of a call to testObj is always called testResult
  - The tests always have the arrange, act, assert structure
- Integration tests are done against the controller and need the application to be running. There are no mocks, and the database gets called.
- I also did some multi-threaded integration tests to check against deadlocks. One of the tests, transfers money back and forth in the same two accounts. Before adding the arbitration lock this would fail most times, indicating a deadlock. After I added the lock I never saw it failing again.

### Extra work

- The in-memory database needs to be replaced by an SQL database, and we should use hibernate (or some other implementation of jpa) to abstract it.
- We can also introduce a tool such as liquibase or flyway for database version control
- Other types of testing are also necessary for such a critical piece of software such as stress or security testing
- The error messages are just generic phrases right now. Error codes should be added to this response so that the frontend application can translate them into a message in the correct language for the user 
- Containerization needs to happen in order to shift this product into production. We can get Docker to containerize the microservice and all its dependencies, and Kubernetes to orchestrate all the services in our system, and spool up more of then whenever we need to scale horizontally.
- CI/CD also need to be taken care of, to integrate all the changes made my the developers, test them and deploy them to the other environments, up to production.
- Authorization and authentication are completely missing, and would need to be implemented
- More documentation is also necessary, especially on the API side, with OpenAPI, for example.