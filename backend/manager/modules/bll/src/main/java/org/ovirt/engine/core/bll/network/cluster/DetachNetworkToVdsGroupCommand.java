package org.ovirt.engine.core.bll.network.cluster;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.ovirt.engine.core.bll.ValidationResult;
import org.ovirt.engine.core.bll.VdsGroupCommandBase;
import org.ovirt.engine.core.bll.utils.PermissionSubject;
import org.ovirt.engine.core.bll.validator.NetworkValidator;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.VdcObjectType;
import org.ovirt.engine.core.common.action.AttachNetworkToVdsGroupParameter;
import org.ovirt.engine.core.common.businessentities.VmTemplate;
import org.ovirt.engine.core.common.businessentities.network.Network;
import org.ovirt.engine.core.common.businessentities.network.NetworkCluster;
import org.ovirt.engine.core.common.businessentities.network.VmNetworkInterface;
import org.ovirt.engine.core.common.errors.VdcBllMessages;
import org.ovirt.engine.core.compat.Guid;

public class DetachNetworkToVdsGroupCommand<T extends AttachNetworkToVdsGroupParameter> extends
        VdsGroupCommandBase<T> {
    public DetachNetworkToVdsGroupCommand(T parameters) {
        super(parameters);
    }

    @Override
    protected void executeCommand() {
        getNetworkClusterDAO().remove(getParameters().getVdsGroupId(), getParameters().getNetwork().getId());
        setSucceeded(true);
    }

    @Override
    protected boolean canDoAction() {
        DetachNetworkValidator validator =
                new DetachNetworkValidator(getParameters().getNetwork(), getParameters().getNetworkCluster());
        return validate(validator.notManagementNetwork())
                && validate(validator.clusterNetworkNotUsedByVms())
                && validate(validator.clusterNetworkNotUsedByTemplates());
    }

    @Override
    public AuditLogType getAuditLogTypeValue() {
        return getSucceeded() ? AuditLogType.NETWORK_DETACH_NETWORK_TO_VDS_GROUP
                : AuditLogType.NETWORK_DETACH_NETWORK_TO_VDS_GROUP_FAILED;
    }

    public String getNetworkName() {
        return getParameters().getNetwork().getName();
    }

    @Override
    protected void setActionMessageParameters() {
        addCanDoActionMessage(VdcBllMessages.VAR__ACTION__DETACH);
        addCanDoActionMessage(VdcBllMessages.VAR__TYPE__NETWORK);
    }

    @Override
    public List<PermissionSubject> getPermissionCheckSubjects() {
        Guid networkId = getParameters().getNetwork() == null ? null : getParameters().getNetwork().getId();
        return Collections.singletonList(new PermissionSubject(networkId,
                VdcObjectType.Network,
                getActionType().getActionGroup()));
    }

    private class DetachNetworkValidator extends NetworkValidator {

        private NetworkCluster networkCluster;

        public DetachNetworkValidator(Network network, NetworkCluster networkCluster) {
            super(network);
            this.networkCluster = networkCluster;
        }

        public ValidationResult clusterNetworkNotUsedByVms() {
            return networkNotUsed(getVmStaticDAO().getAllByGroupAndNetworkName(networkCluster.getClusterId(),
                    network.getName()),
                    VdcBllMessages.VAR__ENTITIES__VMS);
        }

        public ValidationResult clusterNetworkNotUsedByTemplates() {
            List<VmTemplate> templatesUsingNetwork = new ArrayList<VmTemplate>();
            for (VmTemplate template : getVmTemplateDAO().getAllForVdsGroup(networkCluster.getClusterId())) {
                for (VmNetworkInterface nic : getVmNetworkInterfaceDao().getAllForTemplate(template.getId())) {
                    if (network.getName().equals(nic.getNetworkName())) {
                        templatesUsingNetwork.add(template);
                    }
                }
            }
            return networkNotUsed(templatesUsingNetwork, VdcBllMessages.VAR__ENTITIES__VM_TEMPLATES);
        }
    }
}
