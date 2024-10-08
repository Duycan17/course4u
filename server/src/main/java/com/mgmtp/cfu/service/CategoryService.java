package com.mgmtp.cfu.service;

import com.mgmtp.cfu.dto.categorydto.CategoryDTO;

import com.mgmtp.cfu.dto.coursedto.CourseRequest;
import com.mgmtp.cfu.entity.Category;

import java.util.List;

import java.util.Set;

public interface CategoryService {
    List<CategoryDTO> getAvailableCategories();
    List<Category> findOrCreateNewCategory(List<CourseRequest.CategoryCourseRequestDTO> category);

    Set<CategoryDTO> findAllByCourseId(Long id);
}
