package org.ovirt.engine.core.bll.storage;

import java.util.List;

import org.ovirt.engine.core.bll.ValidationResult;
import org.ovirt.engine.core.common.businessentities.StoragePoolStatus;
import org.ovirt.engine.core.common.businessentities.StorageType;
import org.ovirt.engine.core.common.businessentities.VDSGroup;
import org.ovirt.engine.core.common.businessentities.StoragePool;
import org.ovirt.engine.core.common.config.Config;
import org.ovirt.engine.core.common.config.ConfigValues;
import org.ovirt.engine.core.common.errors.VdcBllMessages;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;
import org.ovirt.engine.core.dao.VdsGroupDAO;

/**
 * CanDoAction validation methods for storage pool handling
 */
public class StoragePoolValidator {
    private StoragePool storagePool;

    public StoragePoolValidator(StoragePool storagePool) {
        this.storagePool = storagePool;
    }

    /**
     * Checks in case the DC is of POSIX type that the compatibility version matches. In case there is mismatch, a
     * proper canDoAction message will be added
     *
     * @return The result of the validation
     */
    public ValidationResult isPosixDcAndMatchingCompatiblityVersion() {
        if (storagePool.getStorageType() == StorageType.POSIXFS
                && !Config.<Boolean> GetValue
                        (ConfigValues.PosixStorageEnabled, storagePool.getcompatibility_version().toString())) {
            return new ValidationResult(VdcBllMessages.DATA_CENTER_POSIX_STORAGE_NOT_SUPPORTED_IN_CURRENT_VERSION);
        }
        return ValidationResult.VALID;
    }

    /**
     * Checks in case the DC is of GLUSTER type that the compatibility version matches.
     * In case there is mismatch, a
     * proper canDoAction message will be added
     *
     * @return true if the version matches
     */
    public ValidationResult isGlusterDcAndMatchingCompatiblityVersion() {
        if (storagePool.getStorageType() == StorageType.GLUSTERFS
                && !Config.<Boolean> GetValue
                        (ConfigValues.GlusterFsStorageEnabled, storagePool.getcompatibility_version().toString())) {
            return new ValidationResult(VdcBllMessages.DATA_CENTER_GLUSTER_STORAGE_NOT_SUPPORTED_IN_CURRENT_VERSION);
        }
        return ValidationResult.VALID;
    }

    protected VdsGroupDAO getVdsGroupDao() {
        return DbFacade.getInstance().getVdsGroupDao();
    }

    public ValidationResult isNotLocalfsWithDefaultCluster() {
        if (storagePool.getStorageType() == StorageType.LOCALFS && containsDefaultCluster()) {
            return new ValidationResult(VdcBllMessages.ACTION_TYPE_FAILED_STORAGE_POOL_WITH_DEFAULT_VDS_GROUP_CANNOT_BE_LOCALFS);
        }
        return ValidationResult.VALID;
    }

    protected boolean containsDefaultCluster() {
        List<VDSGroup> clusters = getVdsGroupDao().getAllForStoragePool(storagePool.getId());
        boolean hasDefaultCluster = false;
        for (VDSGroup cluster : clusters) {
            if (cluster.getId().equals(VDSGroup.DEFAULT_VDS_GROUP_ID)) {
                hasDefaultCluster = true;
                break;
            }
        }
        return hasDefaultCluster;
    }

    public ValidationResult isUp() {
        if (storagePool == null || storagePool.getStatus() != StoragePoolStatus.Up) {
            return new ValidationResult(VdcBllMessages.ACTION_TYPE_FAILED_IMAGE_REPOSITORY_NOT_FOUND);
        }

        return ValidationResult.VALID;
    }

}
