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

import java.util.List;

@RequestMapping("jshell")
@RestController
public class JShellController {
    private JShellSessionService service;

    @PostMapping("/eval/{id}")
    public JShellResult eval(@PathVariable String id, @RequestBody String code) throws DockerException {
        validateId(id);
        if(code == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Code is null");
        return service.session(id).eval(code).orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "An operation is already running"));
    }
    @PostMapping("/eval")
    public JShellResultWithId eval(@RequestBody String code) throws DockerException {
        JShellService jShellService = service.session();
        return new JShellResultWithId(jShellService.id(), jShellService.eval(code).orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "An operation is already running")));
    }
    @PostMapping("/single-eval")
    public JShellResult singleEval(@RequestBody String code) throws DockerException {
        JShellService jShellService = service.oneTimeSession();
        return jShellService.eval(code).orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "An operation is already running"));
    }

    @GetMapping("/snippets/{id}")
    public List<String> snippets(@PathVariable String id) throws DockerException {
        validateId(id);
        if(!service.hasSession(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Id " + id + " not found");
        return service.session(id).snippets().orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "An operation is already running"));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) throws DockerException {
        validateId(id);
        if(!service.hasSession(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Id " + id + " not found");
        service.deleteSession(id);
    }

    @Autowired
    public void setService(JShellSessionService service) {
        this.service = service;
    }

    private static void validateId(String id) throws ResponseStatusException {
        if(!id.matches("[a-zA-Z0-9][a-zA-Z0-9_.-]+")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Id " + id + " doesn't match the regex [a-zA-Z0-9][a-zA-Z0-9_.-]+");
        }
    }
}
