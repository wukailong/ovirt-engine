package org.ovirt.engine.ui.webadmin.widget.table.column;

import org.ovirt.engine.core.common.businessentities.StorageDomain;
import org.ovirt.engine.core.common.businessentities.StorageType;

import com.google.gwt.resources.client.ImageResource;

public class StorageDomainSharedStatusColumn extends WebAdminImageResourceColumn<StorageDomain> {

    @Override
    public ImageResource getValue(StorageDomain sp) {
        setEnumTitle(sp.getStorageDomainSharedStatus());
        switch (sp.getStorageDomainSharedStatus()) {
        case Unattached:
            if (sp.getStorageType() == StorageType.GLANCE) {
                return getApplicationResources().openstackImage();
            } else {
                return getApplicationResources().tornChainImage();
            }
        case Active:
            return getApplicationResources().upImage();
        case InActive:
            return getApplicationResources().downImage();
        case Mixed:
            return getApplicationResources().upalertImage();
        case Locked:
            return getApplicationResources().lockImage();
        default:
            return getApplicationResources().downImage();
        }
    }

}
