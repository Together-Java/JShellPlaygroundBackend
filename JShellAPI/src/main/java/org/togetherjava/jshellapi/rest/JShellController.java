package org.togetherjava.jshellapi.rest;

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
    private JShellSessionService service;
    private StartupScriptsService startupScriptsService;

    @PostMapping("/eval/{id}")
    public JShellResult eval(@PathVariable String id,
            @RequestParam(required = false) StartupScriptId startupScriptId,
            @RequestBody String code) throws DockerException {
        validateId(id);
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
    public List<String> snippets(@PathVariable String id,
            @RequestParam(required = false) boolean includeStartupScript) throws DockerException {
        validateId(id);
        if (!service.hasSession(id))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Id " + id + " not found");
        return service.session(id, null)
            .snippets(includeStartupScript)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                    "An operation is already running"));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) throws DockerException {
        validateId(id);
        if (!service.hasSession(id))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Id " + id + " not found");
        service.deleteSession(id);
    }

    @GetMapping("/startup_script/{id}")
    public String startupScript(@PathVariable StartupScriptId id) {
        return startupScriptsService.get(id);
    }

    @Autowired
    public void setService(JShellSessionService service) {
        this.service = service;
    }

    @Autowired
    public void setStartupScriptsService(StartupScriptsService startupScriptsService) {
        this.startupScriptsService = startupScriptsService;
    }

    private static void validateId(String id) throws ResponseStatusException {
        if (!id.matches("[a-zA-Z0-9][a-zA-Z0-9_.-]+")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Id " + id + " doesn't match the regex [a-zA-Z0-9][a-zA-Z0-9_.-]+");
        }
    }
}
