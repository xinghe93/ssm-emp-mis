package com.xinghe.emp.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PaginationProbeMapper {

    @Select("SELECT n FROM (SELECT 1 AS n UNION ALL SELECT 2 UNION ALL SELECT 3) AS probe_values ORDER BY n")
    List<Integer> selectValues(Page<Integer> page);
}
