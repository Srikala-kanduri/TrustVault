
package com.trustvault;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/permissions")
public class MyPermissionGrantsController {

    private final DocumentPermissionService permissionService;

    public MyPermissionGrantsController(
            DocumentPermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @GetMapping("/my-grants")
    public List<AccessGrantResponse> getMyGrants(
            Authentication authentication) {

        return permissionService.listAllGrants(
                authentication.getName());
    }
}
