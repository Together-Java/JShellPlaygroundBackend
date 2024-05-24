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
            @RequestBody String code) throws DockerException {
        return service.session(id, startupScriptId)
            .eval(code)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                    "An operation is already running"));
    }

    @PostMapping("/eval")
    public JShellResultWithId eval(@RequestParam(required = false) StartupScriptId startupScriptId,
            @RequestBody String code) throws DockerException {
        JShellService jShellService = service.session(startupScriptId);
        return new JShellResultWithId(jShellService.id(),
                jShellService.eval(code)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                            "An operation is already running")));
    }

    @PostMapping("/single-eval")
    public JShellResult singleEval(@RequestParam(required = false) StartupScriptId startupScriptId,
            @RequestBody String code) throws DockerException {
        JShellService jShellService = service.oneTimeSession(startupScriptId);
        return jShellService.eval(code)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                    "An operation is already running"));
    }

    @GetMapping("/snippets/{id}")
    public List<String> snippets(
            @PathVariable
            @Pattern(regexp = ID_REGEX, message = "'id' doesn't match regex " + ID_REGEX) String id,
            @RequestParam(required = false) boolean includeStartupScript) throws DockerException {
        checkId(id);
        return service.session(id, null)
            .snippets(includeStartupScript)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                    "An operation is already running"));
    }

    @DeleteMapping("/{id}")
    public void delete(
            @PathVariable
            @Pattern(regexp = ID_REGEX, message = "'id' doesn't match regex " + ID_REGEX) String id)
            throws DockerException {
        checkId(id);
        service.deleteSession(id);
    }

    @GetMapping("/startup_script/{id}")
    public String startupScript(@PathVariable StartupScriptId id) {
        return startupScriptsService.get(id);
    }

    private void checkId(String id) {
        if (!id.matches(ID_REGEX)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Id " + id + " doesn't match regex " + ID_REGEX);
        }
    }
}
