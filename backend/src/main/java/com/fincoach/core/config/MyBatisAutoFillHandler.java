package com.fincoach.core.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 自动填充处理器
 * 替代 MySQL ON UPDATE CURRENT_TIMESTAMP，
 * 在 insert / update 时自动设置时间戳字段。
 */
@Slf4j
@Component
public class MyBatisAutoFillHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        fillIfHas(metaObject, "createTime", now);
        fillIfHas(metaObject, "createdAt", now);
        fillIfHas(metaObject, "updateTime", now);
        fillIfHas(metaObject, "updatedAt", now);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        fillIfHas(metaObject, "updateTime", now);
        fillIfHas(metaObject, "updatedAt", now);
    }

    private void fillIfHas(MetaObject metaObject, String fieldName, LocalDateTime value) {
        if (metaObject.hasSetter(fieldName)) {
            this.strictFillStrategy(metaObject, fieldName, () -> value);
        }
    }
}
