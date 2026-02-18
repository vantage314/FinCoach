package com.fincoach.core.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fincoach.core.repository.entity.FcJobStatusEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FcJobStatusMapper extends BaseMapper<FcJobStatusEntity> {
    @Select("SELECT * FROM fc_job_status WHERE job_name = #{jobName} FOR UPDATE")
    FcJobStatusEntity selectForUpdate(@Param("jobName") String jobName);
}
