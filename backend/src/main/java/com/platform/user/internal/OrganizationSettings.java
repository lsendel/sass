package com.platform.user.internal;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;

/**
 * Organization settings configuration
 */
@Entity
@Table(name = "organization_settings")
public class OrganizationSettings {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "setting_key", nullable = false)
    private String settingKey;

    @Column(name = "setting_value")
    private String settingValue;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    protected OrganizationSettings() {}

    public OrganizationSettings(UUID organizationId, String settingKey, String settingValue) {
        this.id = UUID.randomUUID();
        this.organizationId = organizationId;
        this.settingKey = settingKey;
        this.settingValue = settingValue;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    // Getters
    public UUID getId() { return id; }
    public UUID getOrganizationId() { return organizationId; }
    public String getSettingKey() { return settingKey; }
    public String getSettingValue() { return settingValue; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    // Setters
    public void setSettingValue(String settingValue) {
        this.settingValue = settingValue;
        this.updatedAt = Instant.now();
    }
}