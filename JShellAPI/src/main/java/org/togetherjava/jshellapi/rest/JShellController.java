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
        if(code == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Code is null");
        return service.sessionById(id).eval(code);
    }
    @PostMapping("/eval")
    public JShellResultWithId eval(@RequestBody String code) throws DockerException {
        JShellService jShellService = service.oneTimeSession();
        return new JShellResultWithId(jShellService.id(), jShellService.eval(code));
    }

    @GetMapping("/snippets/{id}")
    public List<String> snippets(@PathVariable String id) throws DockerException {
        if(!service.hasSession(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Id " + id + " not found");
        return service.sessionById(id).snippets();
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) throws DockerException {
        if(!service.hasSession(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Id " + id + " not found");
        service.deleteSession(id);
    }

    @Autowired
    public void setService(JShellSessionService service) {
        this.service = service;
    }
}
