package com.clairvoyant.clarise.service.impl;

import com.clairvoyant.clarise.dto.UserDesignationDto;
import com.clairvoyant.clarise.model.*;
import com.clairvoyant.clarise.repository.UserDesignationRepository;
import com.clairvoyant.clarise.service.UserDesignationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class UserDesignationServiceImpl implements UserDesignationService {

    @Autowired
    private UserDesignationRepository designationRepository;

    @Override
    public List<UserDesignationMapping> addOrUpdateUserDesignation(UserDesignationDto userDesignationDto) {

        List<UserDesignationMapping> userDesignationMapping= designationRepository.findByUserId(userDesignationDto.getUserId());

        if(Objects.isNull(userDesignationMapping) || userDesignationMapping.size()==0)
            newUserDesignationMapping(userDesignationDto.getUserId() , userDesignationDto.getDesignationId());

        return designationRepository.findAll();
    }

    @Override
    public List<User> allUsers(String designationId) {

        List<UserDesignationMapping> userDesignationMappings = designationRepository.findByDesignationIdAndIsActive(designationId,false);

        return userDesignationMappings.stream().map(userDesignationMapping -> userDesignationMapping.getUser()).collect(Collectors.toList());
    }



    public void newUserDesignationMapping(String userId , String designationId)
    {
        UserDesignationMapping userDesignationMapping=new UserDesignationMapping();
        User user=new User();
        Designation designation=new Designation();

        user.setId(userId);
        designation.setId(designationId);

        userDesignationMapping.setUser(user);
        userDesignationMapping.setDesignation(designation);

        designationRepository.save(userDesignationMapping);

    }




}
