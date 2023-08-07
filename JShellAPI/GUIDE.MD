## Endpoints
### Eval
Everything works with sessions, the code is evaluated on a session which has a certain lifetime.
```
POST   /eval/{id}           body=code   -> JShellResult         params=startupScriptId:id
POST   /eval                body=code   -> JShellResultWithId   params=startupScriptId:id
POST   /single-eval         body=code   -> JShellResult         params=startupScriptId:id
```
- The first one takes an id, create a session from this id, or use an existing session if this id already exists.
- The second one creates a new session each time, with a random id, and so it returns the generated id, in order to be reused.
- The third one creates a session that can only be used once, not only that, but this session is called a one-time session and has lower timeout.
- the optional parameter startupScriptId may be added to specify the startup script id.

#### Response
In all three, a string containing code to be evaluated needs to be supplied, and a JShellResult will be returned containing the result :
```java
record JShellResult(
        SnippetStatus status,
        SnippetType type,
        int id,
        String source,
        String result,
        JShellExceptionResult exception,
        boolean stdoutOverflow,
        String stdout,
        List<String> errors){
}
enum SnippetStatus {
    VALID, RECOVERABLE_DEFINED, RECOVERABLE_NOT_DEFINED, REJECTED, ABORTED
}
enum SnippetType {
    ADDITION, MODIFICATION
}
record JShellExceptionResult(
        String exceptionClass,
        String exceptionMessage) {
}
record JShellResultWithId(
        String id,
        JShellResult result) {
}
```
### Other endpoints
```
GET    /snippets/{id}       body=none   -> List<String>         params=includeStartupScript:boolean
DELETE /{id}                body=none   -> none
GET    /startup_script/{id} body=none   -> String
```
- The first one will retrieve all snippets of a given session
- The second one will forcibly delete the given session
  - the optional parameter includeStartupScript may be supplied to indicate if the startup script should be returned, by default false
- The third one will return the startup script for the corresponding startup script id
### Errors
if a problem happens, by default a 5XX error will be thrown, except in those cases :
- Given [id](#Identifiers) is invalid : 400 Bad Request.
- No session found with the given id : 404 Not Found.
- An operation is already running on this session : 409 Conflict.
- A session needs to be created, but the maximum number of session is already reached : 429 Too Many Requests.
## Model
Docker containers are used to encapsulate JShell evaluations.
Each container is called a session, and can be reused assuming an identifier is used.
The id of one-time sessions isn't shared.
The endpoints may create, reuse or rarely delete sessions, deleting their related containers.
### Timeouts
Sessions have two timeouts, a per-eval timeout and a per-session timeout.
#### Eval timeout
This timeout is rather short, and limits the time an eval can last, in order for example to prevent infinite loops to use infinite CPU time.
This timeout happens during the container itself, and so may be subject to security concern.
#### Session timeout
This timeout is rather long, unless for one-time session, and limits the lifetime of a session.
It is reset each time an interaction is done with a session.
Once the timeout happens, a session isn't deleted immediately, but rather after a certain time, when a session killer checks the dead sessions.
When a session dies, the container is deleted.
##### One-time session timeout
A one-time session may have a timeout just a bit longer than an Eval timeout.
### Identifiers
The identifier must match the following regex :
```regexp
[a-zA-Z0-9][a-zA-Z0-9_.-]+
```
A random identifier is simply a random uuid.
### Startup script
A startup script can be used, so it's easier to use jshell.
They are special scripts that are automatically evaluated at the launch of a session, to change the startup script of a session, the session must be deleted.
The startup scripts are the two first scripts of a session, the first being the imports, the second the rest of the startup script.
The following startup scripts id can be used :
- EMPTY : no startup script
- CUSTOM_DEFAULT : contains basic imports, print methods and range method
## Configuration
Properties can be defined in resources/application.properties
### jshellapi.regularSessionTimeoutSeconds
The timeout of a regular session, in seconds, see [Session timeout](#Session-timeout).
### jshellapi.oneTimeSessionTimeoutSeconds
The timeout of a one-time session, in seconds, see [One-time session timeout](#One-time-session-timeout).
### jshellapi.evalTimeoutSeconds
The timeout of an evaluation, in seconds, see [Eval timeout](#Eval-timeout)
### jshellapi.maxAliveSessions
The maximum number of alive sessions, see [Errors](#Errors)
### jshellapi.dockerMaxRamMegaBytes
The maximum ram allocated per container, in megabytes.
### jshellapi.dockerCPUsUsage
The cpu configuration of each container, see [--cpus option of docker](https://docs.docker.com/config/containers/resource_constraints/#cpu).
### jshellapi.schedulerSessionKillScanRate
The rate at which the session killer will check and delete session, in seconds, see [Session timeout](#Session-timeout).