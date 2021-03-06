package org.ovirt.engine.ui.uicommonweb.models.networks;

import org.ovirt.engine.core.common.businessentities.network.Network;
import org.ovirt.engine.core.common.queries.ConfigurationValues;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.StringHelper;
import org.ovirt.engine.ui.uicommonweb.dataprovider.AsyncDataProvider;
import org.ovirt.engine.ui.uicommonweb.models.EntityModel;
import org.ovirt.engine.ui.uicompat.ConstantsManager;
import org.ovirt.engine.ui.uicompat.PropertyChangedEventArgs;

public class NetworkGeneralModel extends EntityModel
{
    private final String ENGINE_NETWORK_NAME =
            (String) AsyncDataProvider.getConfigValuePreConverted(ConfigurationValues.ManagementNetwork);

    private String name;
    private String role;
    private Integer vlan;
    private Integer mtu;
    private String description;
    private Guid id;

    public NetworkGeneralModel() {
        setTitle(ConstantsManager.getInstance().getConstants().generalTitle());
        setHashName("general"); //$NON-NLS-1$
    }

    @Override
    protected void onEntityChanged() {
        super.onEntityChanged();

        if (super.getEntity() != null)
        {
            updateProperties();
        }
    }

    @Override
    protected void entityPropertyChanged(Object sender, PropertyChangedEventArgs e) {
        super.entityPropertyChanged(sender, e);

        updateProperties();
    }

    private void updateProperties() {
        Network extendedNetwork = (Network) getEntity();

        setName(extendedNetwork.getName());
        setId(extendedNetwork.getId());
        setDescription(extendedNetwork.getDescription());

        String role = ""; //$NON-NLS-1$

        if (ENGINE_NETWORK_NAME.equals(extendedNetwork.getName())) {
            role = role.concat(ConstantsManager.getInstance().getConstants().mgmgtNetworkRole());
        }

        if (extendedNetwork.isVmNetwork()) {
            if (!role.equals("")) //$NON-NLS-1$
            {
                role = role.concat(", "); //$NON-NLS-1$
            }
            role = role.concat(ConstantsManager.getInstance().getConstants().vmNetworkRole());
        }
        setRole(role);
        setVlan(extendedNetwork.getVlanId());

        if (extendedNetwork.getMtu() == 0) {
            setMtu(null);
        } else {
            setMtu(extendedNetwork.getMtu());
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String value) {
        if (!StringHelper.stringsEqual(name, value))
        {
            name = value;
            onPropertyChanged(new PropertyChangedEventArgs("Name")); //$NON-NLS-1$
        }
    }

    public String getRole() {
        return role;
    }

    public void setRole(String value) {
        if (!StringHelper.stringsEqual(role, value))
        {
            role = value;
            onPropertyChanged(new PropertyChangedEventArgs("Role")); //$NON-NLS-1$
        }
    }

    public Integer getVlan() {
        return vlan;
    }

    public void setVlan(Integer value) {
        if (vlan == null && value == null)
        {
            return;
        }
        if (vlan == null || !vlan.equals(value))
        {
            vlan = value;
            onPropertyChanged(new PropertyChangedEventArgs("Vlan")); //$NON-NLS-1$
        }
    }

    public Integer getMtu() {
        return mtu;
    }

    public void setMtu(Integer value) {
        if (mtu == null && value == null)
        {
            return;
        }
        if (mtu == null || !mtu.equals(value))
        {
            mtu = value;
            onPropertyChanged(new PropertyChangedEventArgs("Mtu")); //$NON-NLS-1$
        }
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String value)
    {
        if (!StringHelper.stringsEqual(description, value))
        {
            description = value;
            onPropertyChanged(new PropertyChangedEventArgs("Description")); //$NON-NLS-1$
        }
    }

    public Guid getId() {
        return id;
    }

    public void setId(Guid value) {
        if ((id == null && value != null) || (id != null && !id.equals(value)))
        {
            id = value;
            onPropertyChanged(new PropertyChangedEventArgs("Id")); //$NON-NLS-1$
        }
    }
}
