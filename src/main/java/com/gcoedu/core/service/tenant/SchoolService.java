package com.gcoedu.core.service.tenant;

import com.gcoedu.core.domain.dto.tenant.SchoolDTO;
import com.gcoedu.core.domain.entity.publics.City;
import com.gcoedu.core.domain.entity.tenant.School;
import com.gcoedu.core.mapper.tenant.SchoolMapper;
import com.gcoedu.core.repository.publics.CityRepository;
import com.gcoedu.core.repository.tenant.SchoolRepository;
import com.gcoedu.core.service.PermissionService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class SchoolService {

    private final SchoolRepository schoolRepository;
    private final CityRepository cityRepository;
    private final SchoolMapper schoolMapper;
    private final PermissionService permissionService;

    public SchoolService(SchoolRepository schoolRepository, CityRepository cityRepository,
                         SchoolMapper schoolMapper, PermissionService permissionService) {
        this.schoolRepository = schoolRepository;
        this.cityRepository = cityRepository;
        this.schoolMapper = schoolMapper;
        this.permissionService = permissionService;
    }

    public List<SchoolDTO> findAll() {
        List<School> schools = schoolRepository.findAll();
        return permissionService.filterSchools(schools).stream()
                .map(schoolMapper::toDto)
                .collect(Collectors.toList());
    }

    public List<SchoolDTO> findByCityId(String cityId) {
        List<School> schools = schoolRepository.findByCityId(cityId);
        return permissionService.filterSchools(schools).stream()
                .map(schoolMapper::toDto)
                .collect(Collectors.toList());
    }

    public SchoolDTO findById(String id) {
        if (!permissionService.canAccessSchool(id)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado a esta escola.");
        }
        return schoolRepository.findById(id)
                .map(schoolMapper::toDto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Escola não encontrada"));
    }

    public SchoolDTO create(SchoolDTO dto) {
        com.gcoedu.core.domain.entity.publics.User user = permissionService.getCurrentUser();
        String userCityId = permissionService.getCurrentUserCityId();
        
        String targetCityId = dto.cityId();
        if (user != null && user.getRole() != com.gcoedu.core.domain.entity.publics.RoleEnum.ADMIN) {
            if (targetCityId != null && !targetCityId.equals(userCityId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado: Você só pode cadastrar escolas para o seu próprio município.");
            }
            targetCityId = userCityId;
        }

        School school = schoolMapper.toEntity(dto);
        if (school.getId() == null) {
            school.setId(java.util.UUID.randomUUID().toString());
        }
        if (targetCityId != null) {
            City city = cityRepository.findById(targetCityId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cidade não encontrada"));
            school.setCity(city);
        }
        School saved = schoolRepository.save(school);
        return schoolMapper.toDto(saved);
    }

    public SchoolDTO update(String id, SchoolDTO dto) {
        if (!permissionService.canAccessSchool(id)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado a esta escola.");
        }

        com.gcoedu.core.domain.entity.publics.User user = permissionService.getCurrentUser();
        String userCityId = permissionService.getCurrentUserCityId();
        String targetCityId = dto.cityId();

        if (user != null && user.getRole() != com.gcoedu.core.domain.entity.publics.RoleEnum.ADMIN) {
            if (targetCityId != null && !targetCityId.equals(userCityId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado: Você só pode alterar escolas para o seu próprio município.");
            }
        }

        School school = schoolRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Escola não encontrada"));
        
        schoolMapper.updateEntity(dto, school);
        
        String finalCityId = (user != null && user.getRole() != com.gcoedu.core.domain.entity.publics.RoleEnum.ADMIN) ? userCityId : targetCityId;

        if (finalCityId != null && (school.getCity() == null || !school.getCity().getId().equals(finalCityId))) {
            City city = cityRepository.findById(finalCityId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cidade não encontrada"));
            school.setCity(city);
        }
        
        School saved = schoolRepository.save(school);
        return schoolMapper.toDto(saved);
    }

    public void delete(String id) {
        if (!permissionService.canAccessSchool(id)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado a esta escola.");
        }

        if (!schoolRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Escola não encontrada");
        }
        try {
            schoolRepository.deleteById(id);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Não é possível excluir esta escola porque ela possui turmas, alunos ou professores vinculados.");
        }
    }
}
