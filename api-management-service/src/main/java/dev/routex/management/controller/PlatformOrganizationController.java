package dev.routex.management.controller;

import dev.routex.management.dto.requestDto.OrganizationRequest;
import dev.routex.management.dto.requestDto.OrganizationStatusRequest;
import dev.routex.management.dto.requestDto.TenantAdminRequest;
import dev.routex.management.dto.responseDto.OrganizationResponse;
import dev.routex.management.dto.responseDto.TenantAdminResponse;
import dev.routex.management.service.OrganizationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/platform/organizations")
public class PlatformOrganizationController {
    private final OrganizationService organizationService;

    public PlatformOrganizationController(OrganizationService organizationService){
        this.organizationService = organizationService;
    }
    // This controller is for Platform Admin
    // CRUD -> Organization and Tenant Admin

    @PostMapping
    public ResponseEntity<OrganizationResponse> createOrganization(
            @Valid @RequestBody OrganizationRequest request
            ){
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(organizationService.create(request));
    }
    @GetMapping
    public ResponseEntity<List<OrganizationResponse>> findAll(){
        return ResponseEntity.ok(organizationService.findAll());
    }
    @GetMapping("/{id}")
    public ResponseEntity<OrganizationResponse> find(
            @PathVariable Long id
    ){
        return ResponseEntity.ok(organizationService.findById(id));
    }
    @PutMapping("/{id}")
    public ResponseEntity<OrganizationResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody OrganizationRequest organizationRequest
    ){
        return ResponseEntity.ok(organizationService.update(id,organizationRequest));
    }
    @PatchMapping("/{id}/status")
    public ResponseEntity<OrganizationResponse> changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody OrganizationStatusRequest status
    ){
        return  ResponseEntity.ok(organizationService.changeStatus(id,status));
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id){
        organizationService.delete(id);
        return ResponseEntity.noContent().build();
    }


}
