package org.togetherjava.jshellapi.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.constraints.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import org.togetherjava.jshellapi.dto.JShellResult;
import org.togetherjava.jshellapi.dto.JShellResultWithId;
import org.togetherjava.jshellapi.exceptions.DockerException;
import org.togetherjava.jshellapi.service.JShellService;
import org.togetherjava.jshellapi.service.JShellSessionService;
import org.togetherjava.jshellapi.service.StartupScriptId;
import org.togetherjava.jshellapi.service.StartupScriptsService;

import java.util.List;

@RequestMapping("jshell")
@RestController
public class JShellController {
    private static final String ID_REGEX = "^[a-zA-Z0-9][a-zA-Z0-9_.-]+$";

    @Autowired
    private JShellSessionService service;
    @Autowired
    private StartupScriptsService startupScriptsService;

    @PostMapping("/eval/{id}")
    @Operation(summary = "Evaluate code in a JShell session",
            description = "Evaluate code in a JShell session, create a session from this id, or use an"
                    + " existing session if this id already exists.")
    @ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json",
            schema = @Schema(implementation = JShellResult.class))})
    public JShellResult eval(
            @Parameter(description = "id of the session, must follow the regex " + ID_REGEX)
            @Pattern(regexp = ID_REGEX, message = "'id' doesn't match regex " + ID_REGEX)
            @PathVariable String id,
            @Parameter(description = "id of the startup script to use")
            @RequestParam(required = false) StartupScriptId startupScriptId,
            @Parameter(description = "Java code to evaluate") @RequestBody String code)
            throws DockerException {
        return service.session(id, startupScriptId)
            .eval(code)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                    "An operation is already running"));
    }

    @PostMapping("/eval")
    @Operation(summary = "Evaluate code in a JShell session",
            description = "Evaluate code in a JShell session, creates a new session each time, with a random id")
    @ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json",
            schema = @Schema(implementation = JShellResultWithId.class))})
    public JShellResultWithId eval(
            @Parameter(description = "id of the startup script to use")
            @RequestParam(required = false) StartupScriptId startupScriptId,
            @Parameter(description = "Java code to evaluate") @RequestBody String code)
            throws DockerException {
        JShellService jShellService = service.session(startupScriptId);
        return new JShellResultWithId(jShellService.id(),
                jShellService.eval(code)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                            "An operation is already running")));
    }

    @PostMapping("/single-eval")
    @Operation(summary = "Evaluate code in JShell",
            description = "Evaluate code in a JShell session, creates a session that can only be used once, and has lower timeout")
    @ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json",
            schema = @Schema(implementation = JShellResult.class))})
    public JShellResult singleEval(
            @Parameter(description = "id of the startup script to use")
            @RequestParam(required = false) StartupScriptId startupScriptId,
            @Parameter(description = "Java code to evaluate") @RequestBody String code)
            throws DockerException {
        JShellService jShellService = service.oneTimeSession(startupScriptId);
        return jShellService.eval(code)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                    "An operation is already running"));
    }

    @GetMapping("/snippets/{id}")
    @Operation(summary = "Retreive all snippets from a JShell session")
    @ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json",
            schema = @Schema(implementation = List.class))})
    public List<String> snippets(
            @Parameter(description = "id of the session, must follow the regex " + ID_REGEX)
            @Pattern(regexp = ID_REGEX, message = "'id' doesn't match regex " + ID_REGEX)
            @PathVariable String id, @RequestParam(required = false) boolean includeStartupScript)
            throws DockerException {
        checkIdExists(id);
        return service.session(id, null)
            .snippets(includeStartupScript)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                    "An operation is already running"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a JShell session")
    public void delete(
            @Parameter(description = "id of the session, must follow the regex " + ID_REGEX)
            @Pattern(regexp = ID_REGEX, message = "'id' doesn't match regex " + ID_REGEX)
            @PathVariable String id) throws DockerException {
        checkIdExists(id);
        service.deleteSession(id);
    }

    @GetMapping("/startup_script/{id}")
    @Operation(summary = "Get a startup script")
    public String startupScript(@Parameter(description = "id of the startup script to fetch")
    @PathVariable StartupScriptId id) {
        return startupScriptsService.get(id);
    }

    private void checkIdExists(String id) {
        if (!id.matches(ID_REGEX)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Id " + id + " doesn't match regex " + ID_REGEX);
        }
    }
}
