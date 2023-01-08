package org.togetherjava.jshellapi.rest;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.togetherjava.jshellapi.exceptions.DockerException;
import org.togetherjava.jshellapi.service.JShellService;

@RestController
public class JShellController {
    private final JShellService service = new JShellService();

    @GetMapping("/helloworld/{id}")
    public String helloWorld(@PathVariable long id) throws DockerException {
        return service.helloWorld(id);
    }
}
