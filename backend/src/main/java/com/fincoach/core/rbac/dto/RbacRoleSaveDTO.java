package com.fincoach.core.rbac.dto;

public class RbacRoleSaveDTO {
    private Long id;
    private String code;
    private String name;
    private Integer enabled;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getEnabled() { return enabled; }
    public void setEnabled(Integer enabled) { this.enabled = enabled; }
}
