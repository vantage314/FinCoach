package com.fincoach.core.healthv2.dto.admin;

import lombok.Data;

import java.util.List;

@Data
public class AdminUserListDTO {
    private List<AdminUserDTO> items;
    private long total;
    private int page;
    private int size;
}
