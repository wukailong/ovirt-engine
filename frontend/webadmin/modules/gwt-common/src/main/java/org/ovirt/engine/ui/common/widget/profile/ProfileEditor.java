package org.ovirt.engine.ui.common.widget.profile;

import org.ovirt.engine.core.common.businessentities.network.VnicProfileView;
import org.ovirt.engine.ui.common.CommonApplicationMessages;
import org.ovirt.engine.ui.common.CommonApplicationTemplates;
import org.ovirt.engine.ui.common.widget.editor.ListModelTypeAheadListBoxEditor;

import com.google.gwt.core.client.GWT;

public class ProfileEditor extends ListModelTypeAheadListBoxEditor<Object> {

    public final static CommonApplicationMessages messages = GWT.create(CommonApplicationMessages.class);
    public final static CommonApplicationTemplates templates = GWT.create(CommonApplicationTemplates.class);

    public ProfileEditor() {
        super(
                new ListModelTypeAheadListBoxEditor.NullSafeSuggestBoxRenderer<Object>() {

                    @Override
                    public String getReplacementStringNullSafe(Object data) {
                        VnicProfileView profile = (VnicProfileView) data;
                        return (profile == VnicProfileView.EMPTY) ? messages.emptyProfile().asString()
                                : messages.profileAndNetworkSelected(profile.getName(), profile.getNetworkName())
                                        .asString();
                    }

                    @Override
                    public String getDisplayStringNullSafe(Object data) {
                        VnicProfileView profile = (VnicProfileView) data;
                        if (profile == VnicProfileView.EMPTY) {
                            return templates.typeAheadNameDescription(messages.emptyProfile().asString(),
                                    messages.emptyProfileDescription().asString()).asString();
                        }

                        String profileDescription = profile.getDescription();
                        String profileAndNetwork =
                                messages.profileAndNetwork(profile.getName(), profile.getNetworkName()).asString();

                        return templates.typeAheadNameDescription(profileAndNetwork,
                                profileDescription != null ? profileDescription : "").asString(); //$NON-NLS-1$
                    }

                });
    }

}
