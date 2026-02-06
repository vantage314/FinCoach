package com.fincoach.core.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fincoach.core.repository.entity.TransactionRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TransactionRecordMapper extends BaseMapper<TransactionRecord> {
}
