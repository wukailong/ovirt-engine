package org.ovirt.engine.core.bll;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.ovirt.engine.core.bll.adbroker.AdActionType;
import org.ovirt.engine.core.bll.adbroker.BrokerUtils;
import org.ovirt.engine.core.bll.adbroker.LdapBroker;
import org.ovirt.engine.core.bll.adbroker.LdapBrokerUtils;
import org.ovirt.engine.core.bll.adbroker.LdapFactory;
import org.ovirt.engine.core.bll.adbroker.LdapReturnValueBase;
import org.ovirt.engine.core.bll.adbroker.LdapSearchByUserNameParameters;
import org.ovirt.engine.core.bll.adbroker.UserAuthenticationResult;
import org.ovirt.engine.core.bll.session.SessionDataContainer;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.PermissionSubject;
import org.ovirt.engine.core.common.action.LoginResult;
import org.ovirt.engine.core.common.action.LoginUserParameters;
import org.ovirt.engine.core.common.action.VdcLoginReturnValueBase;
import org.ovirt.engine.core.common.businessentities.AdUser;
import org.ovirt.engine.core.common.config.Config;
import org.ovirt.engine.core.common.config.ConfigValues;
import org.ovirt.engine.core.common.interfaces.IVdcUser;
import org.ovirt.engine.core.common.users.VdcUser;
import org.ovirt.engine.core.compat.StringHelper;
import org.ovirt.engine.core.dal.VdcBllMessages;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;
import org.ovirt.engine.core.dal.dbbroker.user_sessions;
import org.ovirt.engine.core.utils.threadpool.ThreadPoolUtil;

public abstract class LoginBaseCommand<T extends LoginUserParameters> extends CommandBase<T> {
    public LoginBaseCommand(T parameters) {
        super(parameters);
    }

    @Override
    protected VdcLoginReturnValueBase CreateReturnValue() {
        return new VdcLoginReturnValueBase();
    }

    @Override
    public VdcLoginReturnValueBase getReturnValue() {
        return (VdcLoginReturnValueBase) super.getReturnValue();
    }

    private void HandleAuthenticationError(List<VdcBllMessages> errorMessages) {
        // check if authentication failed due to password expiration
        LdapBroker adFactory =
                LdapFactory.getInstance(BrokerUtils.getLoginDomain(getParameters().getUserName(), getDomain()));

        if (adFactory == null) {
            addCanDoActionMessage(VdcBllMessages.USER_FAILED_TO_AUTHENTICATION_WRONG_AUTHENTICATION_METHOD);
            getReturnValue().setLoginResult(LoginResult.CantAuthenticate);
            log.errorFormat(getReturnValue().getCanDoActionMessages().get(0) + " : {0}", getParameters().getUserName());
            return;
        }

        LoginResult result = LoginResult.CantAuthenticate;
        if (!Config.<String> GetValue(ConfigValues.AuthenticationMethod).toUpperCase().equals("LDAP")) {
            // In case we're using LDAP+GSSAPI/Kerberos - and there was an
            // authentication error -
            // we cannot query information about the user - we can do it only in
            // local user
            Object tempVar = null;
            Object execResult =
                    adFactory.RunAdAction(
                            AdActionType.GetAdUserByUserName,
                            new LdapSearchByUserNameParameters(getParameters().getSessionId(),
                                    getDomain(),
                                    getParameters()
                                            .getUserName()));
            if (execResult != null) {
                LdapReturnValueBase adExecResult = (LdapReturnValueBase) execResult;
                tempVar = adExecResult.getReturnValue();
            }
            AdUser user = (AdUser) ((tempVar instanceof AdUser) ? tempVar : null);
            if (user != null && user.getPasswordExpired()) {
                // If the password is expired - report just the error to the user
                errorMessages.clear();
                errorMessages.add(VdcBllMessages.USER_PASSWORD_EXPIRED);
                result = LoginResult.PasswordExpired;
            }
        }
        // If for some reason the error messages list is still empty - add the general "user can't authenticate" message
        if (errorMessages.size() == 0) {
            errorMessages.add(VdcBllMessages.USER_FAILED_TO_AUTHENTICATE);
        }

        for (VdcBllMessages msg : errorMessages) {
            getReturnValue().getCanDoActionMessages().add(msg.name());
        }
        getReturnValue().setLoginResult(result);
        log.errorFormat(getReturnValue().getCanDoActionMessages().get(0) + " : {0}", getParameters().getUserName());
    }

    /**
     * Handles the user session.
     *
     * @param adUser
     *            The ad user.
     */
    protected void HandleUserSession(AdUser adUser) {
        if (!StringHelper.isNullOrEmpty(getParameters().getHttpSessionId())) {
            user_sessions user_sessions = new user_sessions("", "", new Date(), "", getParameters()
                    .getHttpSessionId(), adUser.getUserId());
            DbFacade.getInstance().getDbUserDAO().saveSession(user_sessions);
        }
    }

    public String getUserPassword() {
        return getParameters().getUserPassword();
    }

    public String getDomain() {
        return getParameters().getDomain();
    }

    @Override
    public AuditLogType getAuditLogTypeValue() {
        return getSucceeded() ? AuditLogType.USER_VDC_LOGIN : AuditLogType.USER_VDC_LOGIN_FAILED;
    }

    protected abstract UserAuthenticationResult authenticateUser();

    protected AdUser _adUser;

    @Override
    protected void executeCommand() {
        // add user session
        // todo : insert correct values of all arguments, separate
        HandleUserSession(_adUser);
        setActionReturnValue(getCurrentUser());
        getReturnValue().setLoginResult(LoginResult.Autheticated);
        // Permissions for this user might been changed since last login so
        // update his isAdmin flag accordingly
        updateUserData();
        setSucceeded(true);
    }

    @Override
    protected boolean canDoAction() {
        return isUserCanBeAuthenticated() && attachUserToSession();
    }

    protected boolean attachUserToSession() {
        boolean authenticated = true;
        if (!StringHelper.isNullOrEmpty(getParameters().getSessionId())) {
            SessionDataContainer.getInstance().setUser(getParameters().getSessionId(), getCurrentUser());
        } else if (!SessionDataContainer.getInstance().setUser(getCurrentUser())) {
            addCanDoActionMessage(VdcBllMessages.USER_CANNOT_LOGIN_SESSION_MISSING);
            authenticated = false;
        }
        return authenticated;
    }

    protected boolean isUserCanBeAuthenticated() {
        boolean authenticated = false;
        IVdcUser vdcUser = SessionDataContainer.getInstance().getUser();
        if (vdcUser == null) {
            boolean domainFound = false;
            List<String> vdcDomains = LdapBrokerUtils.getDomainsList();
            for (String domain : vdcDomains) {
                if (StringHelper.EqOp(domain.toLowerCase(), getDomain().toLowerCase())) {
                    domainFound = true;
                    break;
                }
            }
            if (!domainFound) {
                addCanDoActionMessage(VdcBllMessages.USER_CANNOT_LOGIN_DOMAIN_NOT_SUPPORTED);
                return false;
            }

            UserAuthenticationResult result = authenticateUser();
            // If no result object is returned from authentication - create a result object with general authentication
            // error
            if (result == null) {
                result = new UserAuthenticationResult(VdcBllMessages.USER_FAILED_TO_AUTHENTICATE);
            }
            _adUser = result.getUser();
            authenticated = result.isSuccessful();

            if ((!authenticated || _adUser == null)) {
                HandleAuthenticationError(result.getErrorMessages());
                authenticated = false;
            }
        } else {
            addCanDoActionMessage(VdcBllMessages.USER_IS_ALREADY_LOGGED_IN);
        }
        if (authenticated) {
            VdcUser currentUser = new VdcUser(_adUser);
            setCurrentUser(currentUser);
            // Persist the most updated version of the user, as received from AD, as this may
            // affect MLA later on
            authenticated = UserCommandBase.persistAuthenticatedUser(_adUser) != null;
        }
        return authenticated;
    }

    @Override
    protected boolean IsUserAutorizedToRunAction() {
        if (log.isDebugEnabled()) {
            log.debugFormat("IsUserAutorizedToRunAction: Login - no permission check");
        }
        return true;
    }

    @Override
    public List<PermissionSubject> getPermissionCheckSubjects() {
        // Not needed for admin operations.
        return Collections.emptyList();
    }

    private void updateUserData() {
        ThreadPoolUtil.execute(new Runnable() {
            @Override
            public void run() {
                DbFacade.getInstance().updateLastAdminCheckStatus(_adUser.getUserId());
            }
        });
    }
}
