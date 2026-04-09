package com.example.learning.service;

import com.example.learning.dto.CategoryDTO;
import com.example.learning.dto.CreateCategoryRequest;
import java.util.List;

public interface CategoryService {
    CategoryDTO create(CreateCategoryRequest request);
    CategoryDTO get(Long id);
    List<CategoryDTO> getAll();
    CategoryDTO update(Long id, CreateCategoryRequest request);
    void delete(Long id);
}