package com.clairvoyant.clarise.repository;


import com.clairvoyant.clarise.model.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SkillRepository extends JpaRepository<Skill, String> {

}
