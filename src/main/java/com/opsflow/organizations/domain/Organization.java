package com.opsflow.organizations.domain;

import com.opsflow.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "organizations")
public class Organization extends BaseEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "slogan", nullable = false, unique = true)
    private String slogan;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    public Organization() {
    }

    public Organization(String name, String slogan) {
        this.name = name;
        this.slogan = slogan;
        this.active = true;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSlogan() {
        return slogan;
    }

    public void setSlogan(String slug) {
        this.slogan = slug;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
