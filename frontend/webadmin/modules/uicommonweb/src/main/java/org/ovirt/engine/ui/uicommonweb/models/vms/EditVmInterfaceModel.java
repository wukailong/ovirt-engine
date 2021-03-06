package org.ovirt.engine.ui.uicommonweb.models.vms;

import java.util.ArrayList;

import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.businessentities.VMStatus;
import org.ovirt.engine.core.common.businessentities.VmBase;
import org.ovirt.engine.core.common.businessentities.network.VmNetworkInterface;
import org.ovirt.engine.core.compat.Version;
import org.ovirt.engine.ui.uicommonweb.models.EntityModel;
import org.ovirt.engine.ui.uicompat.ConstantsManager;
import org.ovirt.engine.ui.uicompat.Event;
import org.ovirt.engine.ui.uicompat.EventArgs;
import org.ovirt.engine.ui.uicompat.PropertyChangedEventArgs;

public class EditVmInterfaceModel extends BaseEditVmInterfaceModel {

    private final VM vm;

    public static EditVmInterfaceModel createInstance(VmBase vmStatic, VM vm,
            Version clusterCompatibilityVersion,
            ArrayList<VmNetworkInterface> vmNicList,
            VmNetworkInterface nic,
            EntityModel sourceModel) {
        EditVmInterfaceModel instance =
                new EditVmInterfaceModel(vmStatic, vm, clusterCompatibilityVersion, vmNicList, nic, sourceModel);
        instance.init();
        return instance;
    }

    protected EditVmInterfaceModel(VmBase vmStatic, VM vm,
            Version clusterCompatibilityVersion,
            ArrayList<VmNetworkInterface> vmNicList,
            VmNetworkInterface nic,
            EntityModel sourceModel) {
        super(vmStatic,
                vm.getStatus(),
                vm.getStoragePoolId(),
                clusterCompatibilityVersion,
                vmNicList,
                nic,
                sourceModel);
        this.vm = vm;
    }

    protected void onPlugChange() {
        if (!isVmUp()) {
            return;
        }

        Boolean plug = isPluggedBeforeAndAfterEdit();

        if (plug) {
            getNicType().setChangeProhibitionReason(ConstantsManager.getInstance()
                    .getConstants()
                    .hotTypeUpdateNotPossible());
            getEnableMac().setChangeProhibitionReason(ConstantsManager.getInstance()
                    .getConstants()
                    .hotMacUpdateNotPossible());

            initSelectedType();
            getEnableMac().setEntity(false);
            initMAC();

        }

        getNicType().setIsChangable(!plug);
        getEnableMac().setIsChangable(!plug);
        getMAC().setIsChangable((Boolean) getEnableMac().getEntity() && !plug);

        updateProfileChangability();
        updateLinkChangability();
    }

    @Override
    protected void updateLinkChangability() {
        super.updateLinkChangability();
        if (!getLinked().getIsChangable()) {
            return;
        }

        boolean isPlugged = isPluggedBeforeAndAfterEdit();

        if (isVmUp() && hotUpdateSupported && isPlugged && selectedNetworkExternal()) {
            getLinked().setChangeProhibitionReason(ConstantsManager.getInstance()
                    .getConstants()
                    .hotLinkStateUpdateNotSupportedExternalNetworks());
            getLinked().setIsChangable(false);
            initLinked();
        }
    }

    @Override
    protected void updateProfileChangability() {
        super.updateProfileChangability();
        if (!getProfile().getIsChangable()) {
            return;
        }

        boolean isPlugged = isPluggedBeforeAndAfterEdit();

        if (isVmUp() && isPlugged) {
            if (!hotUpdateSupported) {
                getProfile().setChangeProhibitionReason(ConstantsManager.getInstance()
                        .getMessages()
                        .hotProfileUpdateNotSupported(getClusterCompatibilityVersion().toString()));
            } else if (selectedNetworkExternal()) {
                getProfile().setChangeProhibitionReason(ConstantsManager.getInstance()
                        .getConstants()
                        .hotNetworkUpdateNotSupportedExternalNetworks());
            } else {
                return;
            }

            getProfile().setIsChangable(false);
            getProfileBehavior().initSelectedProfile(getProfile(), getNic());
        }
    }

    boolean isVmUp() {
        return VMStatus.Up.equals(vm.getStatus());
    }

    @Override
    public void eventRaised(Event ev, Object sender, EventArgs args)
    {
        super.eventRaised(ev, sender, args);

        if (sender == getPlugged())
        {
            PropertyChangedEventArgs propArgs = (PropertyChangedEventArgs) args;
            if (propArgs.PropertyName.equals("Entity")) { //$NON-NLS-1$
                onPlugChange();
            }
        }
    }

    private boolean isPluggedBeforeAndAfterEdit() {
        return getNic().isPlugged() && (Boolean) getPlugged().getEntity();
    }
}
