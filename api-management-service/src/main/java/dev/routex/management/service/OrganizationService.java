package dev.routex.management.service;

import dev.routex.management.dto.requestDto.OrganizationRequest;
import dev.routex.management.dto.requestDto.OrganizationStatusRequest;
import dev.routex.management.dto.requestDto.TenantAdminRequest;
import dev.routex.management.dto.responseDto.OrganizationResponse;
import dev.routex.management.dto.responseDto.TenantAdminResponse;
import dev.routex.management.entity.ManagementUser;
import dev.routex.management.entity.Organization;
import dev.routex.management.entity.OrganizationStatus;
import dev.routex.management.entity.Role;
import dev.routex.management.exception.ApiException;
import dev.routex.management.repository.ManagementUserRepository;
import dev.routex.management.repository.OrganizationRepository;
import dev.routex.management.repository.RoleRepository;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.aspectj.weaver.ast.Or;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrganizationService {
    private final OrganizationRepository organizationRepository;
    private final ManagementUserRepository managementUserRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public OrganizationService(OrganizationRepository organizationRepository,
                               ManagementUserRepository managementUserRepository,
                               RoleRepository roleRepository,
                               PasswordEncoder passwordEncoder){
        this.organizationRepository
                = organizationRepository;
        this.managementUserRepository
                = managementUserRepository;
        this.roleRepository
                = roleRepository;
        this.passwordEncoder
                = passwordEncoder;
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


    @Transactional
    public TenantAdminResponse createTenantAdmin(
            Long organizationId,
            TenantAdminRequest request){
        long startedAt = System.nanoTime();
        Organization organization = getOrganization(organizationId);

        if(managementUserRepository.findByOrganizationId(organizationId).isPresent()){
            throw new ApiException(HttpStatus.CONFLICT,
                    "This organization already has a tenant administrator");
        }
        if(managementUserRepository.existsByUsername(request.getUsername())){
            throw new ApiException(HttpStatus.CONFLICT,
                    "Username already exist");
        }
        Role role = roleRepository.findByName("TENANT_ADMIN")
                .orElseThrow(() -> new ApiException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "TENANT_ADMIN role is missinng"
                ));
        ManagementUser user = new ManagementUser();
        user.setOrganization(organization);
        user.setRole(role);
        user.setUsername(request.getUsername());
        user.setPasswordHash(
                passwordEncoder.encode(
                        request.getPassword()
                )
        );
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setEnabled(request.isEnabled());
        managementUserRepository.save(user);

        return TenantAdminResponse.from(user);
    }

    @Transactional
    public  TenantAdminResponse getTenantAdmin(Long organizationId) {
        return TenantAdminResponse.from(getTenantAdminEntity(
                organizationId
        ));

    }

    @Transactional
    public TenantAdminResponse updateTenantAdmin(
            Long organizationId,
            TenantAdminRequest request
    ){
        long startedAt = System.nanoTime();
        ManagementUser user = getTenantAdminEntity(organizationId);

        if(!user.getUsername().equals(request.getUsername()) &&
           managementUserRepository.existsByUsername(request.getUsername())){
            throw new ApiException(HttpStatus.CONFLICT,"Username already exist");
        }

        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(
                request.getPassword()
        ));
        user.setEnabled(request.isEnabled());
        user.setUpdatedAt(LocalDateTime.now());

        return TenantAdminResponse.from(user);
    }

    @Transactional
    public void deleteTenantAdmin(Long id){
        long startedAt = System.nanoTime();

        ManagementUser user = getTenantAdminEntity(id);

        managementUserRepository.delete(user);
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
    public ManagementUser getTenantAdminEntity(Long organizationId){
        ManagementUser user = managementUserRepository.findByOrganizationId(organizationId)
                .orElseThrow(() -> new
                        ApiException(HttpStatus.CONFLICT,
                        "Tenant Administrator not found"));
        return user;
    }


}
