package dev.routex.management.service;

import dev.routex.management.dto.requestDto.OrganizationRequest;
import dev.routex.management.dto.requestDto.OrganizationStatusRequest;
import dev.routex.management.dto.requestDto.TenantAdminRequest;
import dev.routex.management.dto.responseDto.OrganizationResponse;
import dev.routex.management.dto.responseDto.TenantAdminResponse;
import dev.routex.management.entity.Organization;
import dev.routex.management.entity.OrganizationStatus;
import dev.routex.management.exception.ApiException;
import dev.routex.management.repository.OrganizationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.aspectj.weaver.ast.Or;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrganizationService {
    private final OrganizationRepository organizationRepository;

    public OrganizationService(OrganizationRepository organizationRepository){
        this.organizationRepository
                = organizationRepository;
    }

    @Transactional
    public OrganizationResponse create(OrganizationRequest request){
        long startedAt = System.nanoTime();
        checkUnique(request.getName(), request.getSlug(), null);

        Organization organization = new Organization();
        organization.setName(request.getName());
        organization.setSlug(request.getSlug());
        organization.setStatus(OrganizationStatus.PENDING);
        organization.setCreatedAt(LocalDateTime.now());
        organization.setUpdatedAt(LocalDateTime.now());

        organizationRepository.save(organization);

//        auditService.record(organization,
//                "CREATE_ORGANIZATION",
//                "ORGANIZATION",
//                organization.getId().toString(),
//                startedAt,
//                "Organization created in PENDING status"
//        );
        return OrganizationResponse.from(organization);
    }
    @Transactional(readOnly = true)
    public List<OrganizationResponse> findAll(){
        List<OrganizationResponse> responses = new ArrayList<>();
        for(Organization organization :
                organizationRepository.findAll()){
            responses.add(OrganizationResponse.from(organization));
        }
        return responses;
    }
    @Transactional(readOnly = true)
    public OrganizationResponse findById(Long id){
        return OrganizationResponse.from(getOrganization(id));
    }

    @Transactional
    public OrganizationResponse update(Long id, OrganizationRequest request){
        long startedAt = System.nanoTime();
        Organization organization = getOrganization(id);

        checkUnique(request.getName(),request.getSlug(),id);

        organization.setName(request.getName());
        organization.setSlug(request.getSlug());
        organization.setUpdatedAt(LocalDateTime.now());

        return OrganizationResponse.from(organization);
    }

    @Transactional
    public OrganizationResponse changeStatus(Long id, OrganizationStatusRequest request){
        long startedAt = System.nanoTime();

        Organization organization = getOrganization(id);

        if(request.getStatus() == OrganizationStatus.DELETED){
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Use DELETE to soft delete an organization");
        }
        organization.setStatus(request.getStatus());
        organization.setUpdatedAt(LocalDateTime.now());

        return OrganizationResponse.from(organization);
    }

    @Transactional
    public void delete(Long id){
        long startedAt = System.nanoTime();

        Organization organization = getOrganization(id);
        organization.setStatus(OrganizationStatus.DELETED);
        organization.setDeletedAt(LocalDateTime.now());
        organization.setUpdatedAt(LocalDateTime.now());
    }


    private Organization getOrganization(Long id){
        return organizationRepository.
                findByIdAndStatusNot(id,OrganizationStatus.DELETED)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,"Organization not found!"));
    }

    private void checkUnique(String name, String slug, Long currentId) {
        if(organizationRepository.existsByNameIgnoreCase(name)) {
            Organization current = currentId == null ?
                    null : organizationRepository.findById(currentId).orElse(null);

            if(current == null || !current.getName().equalsIgnoreCase(name)) {
                throw new ApiException(HttpStatus.CONFLICT, "Organization name already exists");
            }
        }

        if(organizationRepository.existsBySlugIgnoreCase(slug)) {
            Organization current = currentId == null ?
                    null : organizationRepository.findById(currentId).orElse(null);

            if(current == null || !current.getSlug().equalsIgnoreCase(slug)) {
                throw new ApiException(HttpStatus.CONFLICT, "Organization slug already exists");
            }
        }
    }
}
