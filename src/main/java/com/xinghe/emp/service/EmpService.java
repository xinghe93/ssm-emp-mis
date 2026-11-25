package com.xinghe.emp.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xinghe.emp.dto.EmpCreateRequest;
import com.xinghe.emp.dto.EmpQueryRequest;
import com.xinghe.emp.dto.EmpUpdateRequest;
import com.xinghe.emp.entity.Emp;
import java.util.List;

public interface EmpService {

    Emp create(EmpCreateRequest request);

    Emp update(EmpUpdateRequest request);

    int deleteById(Long id);

    int deleteBatch(List<Long> ids);

    Emp getById(Long id);

    IPage<Emp> findPage(EmpQueryRequest request);
}
