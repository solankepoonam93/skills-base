package com.clairvoyant.clarise.service.impl;

import com.clairvoyant.clarise.dto.*;
import com.clairvoyant.clarise.model.*;
import com.clairvoyant.clarise.repository.*;
import com.clairvoyant.clarise.service.SkillAssessmentService;
import liquibase.pro.packaged.S;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class SkillAssessmentServiceImpl implements SkillAssessmentService {

    public static final String ASSESSMENT_TYPE_COMPLETED = "completed";

    public static final String ASSESSMENT_STATUS_SELFASSESSMENT = "Self Assessment";

    @Autowired
    private SkillAssessmentRepository skillAssessmentRepository;

    @Autowired
    private SkillCategoryRepository skillCategoryRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private AssessmentTypeRepository assessmentTypeRepository;

    @Autowired
    private AssessmentStatusRepository assessmentStatusRepository;

    @Autowired
    private AssessmentRepository assessmentRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Get SkillAssessment details based on list of categoryIds.
     *
     * @param categoryIds list of categoryIds.
     * @return SkillCategoryResponse.
     */
    @Override
    public SkillCategoryResponse getSkillAssessmentDetails(List<String> categoryIds) {
        SkillCategoryResponse skillCategoryResponse = new SkillCategoryResponse();
        skillCategoryResponse.setCategoryList(new ArrayList<>());
        for (String categoryId : categoryIds) {
            List<SkillCategory> skillCategoryList = skillCategoryRepository.findByCategoryIdAndIsActive(categoryId, true);
            if (skillCategoryList == null) return null;

            CategoryList categoryList = new CategoryList();
            categoryList.setCategoryId(skillCategoryList.get(0).getCategory().getId());
            categoryList.setCategoryName(skillCategoryList.get(0).getCategory().getCatName());
            categoryList.setSkillCategoryList(new ArrayList<>());

            for (SkillCategory skillCategoryObject : skillCategoryList) {
                SkillCategoryList skillCategory = new SkillCategoryList();
                skillCategory.setSkillCategoryId(skillCategoryObject.getId());
                skillCategory.setSkillName(skillCategoryObject.getSkill().getSkillName());
                categoryList.getSkillCategoryList().add(skillCategory);

            }
            skillCategoryResponse.getCategoryList().add(categoryList);
        }

        return skillCategoryResponse;
    }

    /**
     * Save or Update the Assessment details in Assessment & SkillAssessment Tables.     *
     *
     * @param assessmentDto input assessmentDto object from UI.
     */
    @Override
    public void saveSkillAssessmentDetails(AssessmentDto assessmentDto) {
        //For now creating AssessmentType with Completed
        AssessmentType newAssessmentType = new AssessmentType();
        newAssessmentType.setName(ASSESSMENT_TYPE_COMPLETED);
        AssessmentType assessmentType = assessmentTypeRepository.save(newAssessmentType);

        //For now creating AssessmentStatus with SelfAssessment
        AssessmentStatus newAssessmentStatus = new AssessmentStatus();
        newAssessmentStatus.setName(ASSESSMENT_STATUS_SELFASSESSMENT);
        AssessmentStatus assessmentStatus = assessmentStatusRepository.save(newAssessmentStatus);

        User user = new User();
        user.setId(assessmentDto.getUserId());

        Assessment assessment = new Assessment();
        assessment.setAssessmentStatus(assessmentStatus);
        assessment.setAssessmentType(assessmentType);
        assessment.setUser(user);
        assessment.setComments(assessmentDto.getComments());

        Assessment assessmentResponse = assessmentRepository.save(assessment);

        for (AssessmentCategoryList assessmentCategoryList : assessmentDto.getAssessmentCategoryList()) {
            for (SkillCategorySelected skllCategorySelected : assessmentCategoryList.getSkillCategorySelected()) {
                SkillAssessment skillAssessment = new SkillAssessment();
                skillAssessment.setAssessment(assessmentResponse);

                SkillsRating skillsRating = new SkillsRating();
                skillsRating.setId(skllCategorySelected.getSelectedSkillRatingId());
                skillAssessment.setSkillsRating(skillsRating);

                SkillCategory skillCategory = new SkillCategory();
                skillCategory.setId(skllCategorySelected.getSkillCategoryId());
                skillAssessment.setSkillCategory(skillCategory);

                skillAssessmentRepository.save(skillAssessment);
            }
        }
    }

    /**
     * Get saved skill assessment details based on assessmentId.
     *
     * @param assessmentId assessmentId for which we are trying to get SkillAssessmentdetails.
     * @return AssessmentDto with skill,category,skill assessment details.
     */
    @Override
    public AssessmentDto getSavedSkillAssessmentDetails(String assessmentId) {
        List<SkillAssessment> skillssessmentList = skillAssessmentRepository.findByAssessmentId(assessmentId);

        AssessmentDto assessmentDto = new AssessmentDto();
        assessmentDto.setUserId(skillssessmentList.get(0).getAssessment().getUser().getId());
        assessmentDto.setComments(skillssessmentList.get(0).getAssessment().getComments());

        //Storing categoryId with List of SkillCategorySelected in a map
        Map<String, List<SkillCategorySelected>> skillCategorySelectedMap = new HashMap<>();
        for (SkillAssessment skillAssessmentObject : skillssessmentList) {
            String cat_Id = skillAssessmentObject.getSkillCategory().getCategory().getId();

            SkillCategorySelected skillCategorySelected = new SkillCategorySelected();
            skillCategorySelected.setSkillCategoryId(skillAssessmentObject.getSkillCategory().getId());
            skillCategorySelected.setSelectedSkillRatingId(skillAssessmentObject.getSkillsRating().getId());
            skillCategorySelected.setSkillName(skillAssessmentObject.getSkillCategory().getSkill().getSkillName());

            if (skillCategorySelectedMap.containsKey(cat_Id)) {
                List<SkillCategorySelected> skillCategorySelectedList = skillCategorySelectedMap.get(cat_Id);
                skillCategorySelectedList.add(skillCategorySelected);
                skillCategorySelectedMap.put(cat_Id, skillCategorySelectedList);
            } else {
                List<SkillCategorySelected> skillCategorySelectedList = new ArrayList<>();
                skillCategorySelectedList.add(skillCategorySelected);
                skillCategorySelectedMap.put(cat_Id, skillCategorySelectedList);
            }
        }

        // Trying to fetch category details from SkillAssessment and storing in Set inorder to avoid duplicate categories
        Set<AssessmentCategoryList> categoryListHashSet = new HashSet<>();
        for (SkillAssessment skillAssessment : skillssessmentList) {

            AssessmentCategoryList assessmentCategoryListObject = new AssessmentCategoryList();
            String category_Id = skillAssessment.getSkillCategory().getCategory().getId();
            assessmentCategoryListObject.setCategoryId(category_Id);
            assessmentCategoryListObject.setCategoryName(skillAssessment.getSkillCategory().getCategory().getCatName());

            categoryListHashSet.add(assessmentCategoryListObject);
        }

        // Trying to fetch List of skillCategorySelected from map based on categoryId and setting to AssessmentCategoryList.
        for(AssessmentCategoryList assessmentCategoryListObject: categoryListHashSet) {
            if (skillCategorySelectedMap.containsKey(assessmentCategoryListObject.getCategoryId())) {
                assessmentCategoryListObject.setSkillCategorySelected(skillCategorySelectedMap
                        .get(assessmentCategoryListObject.getCategoryId()));
            }
        }

        assessmentDto.setAssessmentCategoryList(new ArrayList<>(categoryListHashSet));

        return assessmentDto;
    }
}


































