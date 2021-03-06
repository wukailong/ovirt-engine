package org.ovirt.engine.core.common.businessentities;

import java.io.Serializable;
import java.util.Map;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.ovirt.engine.core.common.utils.ObjectUtils;
import org.ovirt.engine.core.common.validation.annotation.ValidI18NName;
import org.ovirt.engine.core.common.validation.annotation.ValidVdsGroup;
import org.ovirt.engine.core.common.validation.group.CreateEntity;
import org.ovirt.engine.core.common.validation.group.UpdateEntity;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.Version;

@ValidVdsGroup(groups = { CreateEntity.class })

public class VDSGroup extends IVdcQueryable implements Serializable, BusinessEntity<Guid>, HasStoragePool<Guid>, Nameable, Commented {

    private static final long serialVersionUID = 5659359762655478095L;

    public static final Guid DEFAULT_VDS_GROUP_ID = new Guid("99408929-82CF-4DC7-A532-9D998063FA95");

    private Guid id;

    @NotNull(message = "VALIDATION.VDS_GROUP.NAME.NOT_NULL", groups = { CreateEntity.class, UpdateEntity.class })
    @Size(min = 1, max = BusinessEntitiesDefinitions.CLUSTER_NAME_SIZE, message = "VALIDATION.VDS_GROUP.NAME.MAX",
            groups = {
            CreateEntity.class, UpdateEntity.class })
    @ValidI18NName(message = "VALIDATION.VDS_GROUP.NAME.INVALID", groups = { CreateEntity.class, UpdateEntity.class })
    private String name;

    @Size(max = BusinessEntitiesDefinitions.GENERAL_MAX_SIZE)
    private String description;

    private String comment;

    @Size(max = BusinessEntitiesDefinitions.CLUSTER_CPU_NAME_SIZE)
    private String cpu_name;

    private Guid storagePoolId;

    @Size(max = BusinessEntitiesDefinitions.DATACENTER_NAME_SIZE)
    private String storagePoolName;

    private int max_vds_memory_over_commit;

    private boolean enableBallooning;

    private boolean countThreadsAsCores;

    @Size(max = BusinessEntitiesDefinitions.GENERAL_VERSION_SIZE)
    private String compatibility_version;

    private Version compatVersion;

    private boolean transparentHugepages;

    @NotNull(message = "VALIDATION.VDS_GROUP.MigrateOnError.NOT_NULL")
    private MigrateOnErrorOptions migrateOnError;

    private boolean virtService;

    private boolean glusterService;

    private boolean tunnelMigration;

    private String emulatedMachine;

    private boolean trustedService;

    private Guid clusterPolicyId;

    private String clusterPolicyName;

    private Map<String, String> clusterPolicyProperties;
    private boolean detectEmulatedMachine;

    public VDSGroup() {
        migrateOnError = MigrateOnErrorOptions.YES;
        name = "";
        virtService = true;
    }

    @Override
    public Guid getId() {
        return id;
    }

    @Override
    public void setId(Guid value) {
        id = value;
    }

    public void setvds_group_id(Guid value) {
        setId(value);
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String value) {
        name = value;
    }

    public String getdescription() {
        return description;
    }

    public void setdescription(String value) {
        description = value;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String value) {
        comment = value;
    }

    public String getcpu_name() {
        return this.cpu_name;
    }

    public void setcpu_name(String value) {
        this.cpu_name = value;
    }

    @Override
    public Guid getStoragePoolId() {
        return storagePoolId;
    }

    @Override
    public void setStoragePoolId(Guid storagePool) {
        this.storagePoolId = storagePool;
    }

    public String getStoragePoolName() {
        return this.storagePoolName;
    }

    public void setStoragePoolName(String value) {
        this.storagePoolName = value;
    }

    public int getmax_vds_memory_over_commit() {
        return this.max_vds_memory_over_commit;
    }

    public void setmax_vds_memory_over_commit(int value) {
        this.max_vds_memory_over_commit = value;
    }

    public boolean getCountThreadsAsCores() {
        return this.countThreadsAsCores;
    }

    public void setCountThreadsAsCores(boolean value) {
        this.countThreadsAsCores = value;
    }

    public Version getcompatibility_version() {
        return compatVersion;
    }

    public boolean getTransparentHugepages() {
        return this.transparentHugepages;
    }

    public void setTransparentHugepages(boolean value) {
        this.transparentHugepages = value;
    }

    public void setcompatibility_version(Version value) {
        compatibility_version = value.getValue();
        compatVersion = value;
    }

    @Override
    public Object getQueryableId() {
        return getId();
    }

    public void setMigrateOnError(MigrateOnErrorOptions migrateOnError) {
        this.migrateOnError = migrateOnError;
    }

    public MigrateOnErrorOptions getMigrateOnError() {
        return migrateOnError;
    }

    public void setVirtService(boolean virtService) {
        this.virtService = virtService;
    }

    public boolean supportsVirtService() {
        return virtService;
    }

    public void setGlusterService(boolean glusterService) {
        this.glusterService = glusterService;
    }

    public boolean supportsGlusterService() {
        return glusterService;
    }

    public boolean isTunnelMigration() {
        return tunnelMigration;
    }

    public void setTunnelMigration(boolean value) {
        tunnelMigration = value;
    }

    public String getEmulatedMachine() {
        return emulatedMachine;
    }

    public void setEmulatedMachine(String emulatedMachine) {
        this.emulatedMachine = emulatedMachine;
    }

    public void setTrustedService(boolean trustedService) {
        this.trustedService = trustedService;
    }

    public boolean supportsTrustedService() {
        return trustedService;
    }

    public Guid getClusterPolicyId() {
        return clusterPolicyId;
    }

    public void setClusterPolicyId(Guid clusterPolicyId) {
        this.clusterPolicyId = clusterPolicyId;
    }

    public String getClusterPolicyName() {
        return clusterPolicyName;
    }

    public void setClusterPolicyName(String clusterPolicyName) {
        this.clusterPolicyName = clusterPolicyName;
    }

    public Map<String, String> getClusterPolicyProperties() {
        return clusterPolicyProperties;
    }

    public void setClusterPolicyProperties(Map<String, String> clusterPolicyProperties) {
        this.clusterPolicyProperties = clusterPolicyProperties;
    }

    public boolean isEnableBallooning() {
        return enableBallooning;
    }

    public void setEnableBallooning(boolean enableBallooning) {
        this.enableBallooning = enableBallooning;
    }

    public void setDetectEmulatedMachine(boolean detectEmulatedMachine) {
        this.detectEmulatedMachine = detectEmulatedMachine;
    }

    public boolean isDetectEmulatedMachine() {
        return detectEmulatedMachine;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((compatVersion == null) ? 0 : compatVersion.hashCode());
        result = prime * result + ((compatibility_version == null) ? 0 : compatibility_version.hashCode());
        result = prime * result + ((cpu_name == null) ? 0 : cpu_name.hashCode());
        result = prime * result + ((description == null) ? 0 : description.hashCode());
        result = prime * result + max_vds_memory_over_commit;
        result = prime * result + (countThreadsAsCores ? 1231 : 1237);
        result = prime * result + ((migrateOnError == null) ? 0 : migrateOnError.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((storagePoolId == null) ? 0 : storagePoolId.hashCode());
        result = prime * result + ((storagePoolName == null) ? 0 : storagePoolName.hashCode());
        result = prime * result + (transparentHugepages ? 1231 : 1237);
        result = prime * result + (virtService ? 1231 : 1237);
        result = prime * result + (glusterService ? 1231 : 1237);
        result = prime * result + (tunnelMigration ? 1231 : 1237);
        result = prime * result + (emulatedMachine == null ? 0 : emulatedMachine.hashCode());
        result = prime * result + (trustedService ? 1231 : 1237);
        result = prime * result + ((clusterPolicyName == null) ? 0 : clusterPolicyName.hashCode());
        result = prime * result + (clusterPolicyProperties == null ? 0 : clusterPolicyProperties.hashCode());
        result = prime * result + (enableBallooning ? 1231 : 1237);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        VDSGroup other = (VDSGroup) obj;
        return (ObjectUtils.objectsEqual(id, other.id)
                && ObjectUtils.objectsEqual(compatVersion, other.compatVersion)
                && ObjectUtils.objectsEqual(compatibility_version, other.compatibility_version)
                && ObjectUtils.objectsEqual(cpu_name, other.cpu_name)
                && ObjectUtils.objectsEqual(description, other.description)
                && max_vds_memory_over_commit == other.max_vds_memory_over_commit
                && countThreadsAsCores == other.countThreadsAsCores
                && migrateOnError == other.migrateOnError
                && ObjectUtils.objectsEqual(name, other.name)
                && ObjectUtils.objectsEqual(storagePoolId, other.storagePoolId)
                && ObjectUtils.objectsEqual(storagePoolName, other.storagePoolName)
                && transparentHugepages == other.transparentHugepages
                && virtService == other.virtService
                && glusterService == other.glusterService
                && tunnelMigration == other.tunnelMigration
                && ObjectUtils.objectsEqual(emulatedMachine, other.emulatedMachine)
                && trustedService == other.trustedService
                && ObjectUtils.objectsEqual(clusterPolicyId, other.clusterPolicyId)
                && ObjectUtils.objectsEqual(clusterPolicyName, other.clusterPolicyName)
                && ObjectUtils.objectsEqual(clusterPolicyProperties, other.clusterPolicyProperties)
                && enableBallooning == other.enableBallooning
                && detectEmulatedMachine == other.detectEmulatedMachine);
    }

}
