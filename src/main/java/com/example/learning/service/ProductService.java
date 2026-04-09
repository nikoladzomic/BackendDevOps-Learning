package com.example.learning.service;

import com.example.learning.dto.*;
import java.util.List;

public interface ProductService {
    ProductDTO create(CreateProductRequest request);
    ProductDTO get(Long id);
    PagedResponse<ProductDTO> getAllFiltered(ProductFilterRequest filter);
    ProductDTO update(Long id, CreateProductRequest request);
    void delete(Long id);
    void setActiveStatus(Long id, boolean active);
}