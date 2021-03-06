package org.ovirt.engine.ui.frontend;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.ovirt.engine.core.common.action.LoginUserParameters;
import org.ovirt.engine.core.common.action.VdcActionParametersBase;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.action.VdcReturnValueBase;
import org.ovirt.engine.core.common.businessentities.DbUser;
import org.ovirt.engine.core.common.errors.VdcFault;
import org.ovirt.engine.core.common.queries.VdcQueryParametersBase;
import org.ovirt.engine.core.common.queries.VdcQueryReturnValue;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.ui.frontend.communication.UserCallback;
import org.ovirt.engine.ui.frontend.communication.VdcOperation;
import org.ovirt.engine.ui.frontend.communication.VdcOperationCallback;
import org.ovirt.engine.ui.frontend.communication.VdcOperationCallbackList;
import org.ovirt.engine.ui.frontend.communication.VdcOperationManager;
import org.ovirt.engine.ui.uicompat.ConstantsManager;
import org.ovirt.engine.ui.uicompat.Event;
import org.ovirt.engine.ui.uicompat.EventArgs;
import org.ovirt.engine.ui.uicompat.EventDefinition;
import org.ovirt.engine.ui.uicompat.FrontendActionAsyncResult;
import org.ovirt.engine.ui.uicompat.FrontendMultipleActionAsyncResult;
import org.ovirt.engine.ui.uicompat.FrontendMultipleQueryAsyncResult;
import org.ovirt.engine.ui.uicompat.IFrontendActionAsyncCallback;
import org.ovirt.engine.ui.uicompat.IFrontendMultipleActionAsyncCallback;
import org.ovirt.engine.ui.uicompat.IFrontendMultipleQueryAsyncCallback;
import org.ovirt.engine.ui.uicompat.UIConstants;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.user.client.rpc.StatusCodeException;
import com.google.inject.Inject;
/**
 * The {@code Frontend} class is the interface between the front-end and the back-end. It manages the operations
 * that are being sent to the back-end and returns the results back to the callers. This is class is designed as a
 * singleton class.
 *
 * There are several deprecated static methods that allow static access to the singleton, usage of those methods is
 * purely for legacy purposes and is discouraged for new development.
 */
public class Frontend {
    /**
     * The logger.
     */
    private static Logger logger = Logger.getLogger(Frontend.class.getName());

    /**
     * The events handler.
     */
    private IFrontendEventsHandler eventsHandler;

    /**
     * The loginHandler.
     */
    private FrontendLoginHandler loginHandler;

    /**
     * Action error translator.
     */
    private ErrorTranslator canDoActionErrorsTranslator = null;

    /**
     * VDSM error translator.
     */
    private ErrorTranslator vdsmErrorsTranslator = null;

    /**
     * The set of query types others are subscribed to.
     */
    private final Set<VdcQueryType> subscribedQueryTypes = new HashSet<VdcQueryType>();

    /**
     * The query start event definition.
     */
    private final EventDefinition queryStartedEventDefinition = new EventDefinition("QueryStarted", //$NON-NLS-1$
            Frontend.class);

    /**
     * The query start event.
     */
    Event queryStartedEvent = new Event(queryStartedEventDefinition);

    /**
     * The query complete event definition.
     */
    private final EventDefinition queryCompleteEventDefinition = new EventDefinition("QueryComplete", //$NON-NLS-1$
            Frontend.class);
    /**
     * The query complete event.
     */
    Event queryCompleteEvent = new Event(queryCompleteEventDefinition);

    /**
     * The {@code frontendFailureEvent} event.
     */
    Event frontendFailureEvent = new Event("FrontendFailure", Frontend.class); //$NON-NLS-1$

    /**
     * The {@code frontendNotLoggedInEvent} event.
     */
    Event frontendNotLoggedInEvent = new Event("NotLoggedIn", Frontend.class); //$NON-NLS-1$

    /**
     * The context the current operation is run against.
     */
    private String currentContext;

    /**
     * The currently logged in user.
     */
    private DbUser currentUser;

    /**
     * The current login password.
     */
    private String currentPassword;

    /**
     * Should queries be filtered.
     */
    private boolean filterQueries;

    /**
     * UI constants, messages to show the user in the UI.
     */
    private UIConstants constants = null;

    /**
     * The {@code VdcOperationManager}.
     */
    private final VdcOperationManager operationManager;

    /**
     * GWT scheduler.
     */
    private Scheduler scheduler;

    /**
     * The instance of the {@code Frontend} class.
     */
    private static Frontend instance;

    /**
     * Constructor.
     * @param manager The {@code VdcOperationManger} to associate with this object.
     * @param applicationErrors The application error messages, we can use to translate application errors.
     * @param vdsmErrors The VDSM error messages, we can use to translate VDSM errors.
     */
    @Inject
    public Frontend(final VdcOperationManager manager, final AppErrors applicationErrors,
            final VdsmErrors vdsmErrors) {
        this.operationManager = manager;
        setAppErrorsTranslator(new ErrorTranslator(applicationErrors));
        setVdsmErrorsTranslator(new ErrorTranslator(vdsmErrors));
        instance = this;
    }

    /**
     * Gets the {@code VdcOperationManager} associated with this {@code Frontend}.
     * @return The operation manager.
     */
    public VdcOperationManager getOperationManager() {
        return operationManager;
    }

    /**
     * Get an instance of the {@code Frontend} class. This is here to support legacy code, the appropriate way to
     * get an instance is have it injected by the IoC framework.
     * @return {@code Frontend} instance.
     */
    public static Frontend getInstance() {
        return instance;
    }

    /**
     * Run a query against the back-end. This method is deprecated, the preferred method of running queries against
     * the back-end is, have a copy of Frontend injected into your class, and use the {@code runQuery} method.
     *
     * @param queryType The type of the query.
     * @param parameters The parameters of the query.
     * @param callback The callback to call when the query completes.
     */
    @Deprecated
    public static void RunQuery(final VdcQueryType queryType,
            final VdcQueryParametersBase parameters,
            final AsyncQuery callback) {
        RunQuery(queryType, parameters, callback, false);
    }

    /**
     * Static version
     * Run a query against the back-end. This method is deprecated, the preferred method of running queries against
     * the back-end is, have a copy of Frontend injected into your class, and use the {@code runQuery} method.
     *
     * @param queryType The type of the query.
     * @param parameters The parameters of the query.
     * @param callback The callback to call when the query completes.
     * @param isPublic Determine if the query is public or not.
     */
    @Deprecated
    public static void RunQuery(final VdcQueryType queryType,
            final VdcQueryParametersBase parameters,
            final AsyncQuery callback, final boolean isPublic) {
        getInstance().runQuery(queryType, parameters, callback, isPublic);
    }

    /**
     * Run a query against the back-end.
     *
     * @param queryType The type of the query.
     * @param parameters The parameters of the query.
     * @param callback The callback to call when the query completes.
     * @param isPublic Determine if the query is public or not.
     */
    public void runQuery(final VdcQueryType queryType,
            final VdcQueryParametersBase parameters,
            final AsyncQuery callback, final boolean isPublic) {
        initQueryParamsFilter(parameters);
        final VdcOperation<VdcQueryType, VdcQueryParametersBase> operation =
                new VdcOperation<VdcQueryType, VdcQueryParametersBase>(queryType, parameters, isPublic,
                new VdcOperationCallback<VdcOperation<VdcQueryType, VdcQueryParametersBase>, VdcQueryReturnValue>() {

            @Override
            public void onSuccess(final VdcOperation<VdcQueryType, VdcQueryParametersBase> operation,
                    final VdcQueryReturnValue result) {
                try {
                    if (!result.getSucceeded()) {
                        logger.log(Level.WARNING, "Failure while invoking runQuery [" //$NON-NLS-1$
                                + result.getExceptionString() + "]"); //$NON-NLS-1$
                        if (getEventsHandler() != null) {
                            ArrayList<VdcQueryReturnValue> failedResult = new ArrayList<VdcQueryReturnValue>();
                            failedResult.add(result);
                            // getEventsHandler().runQueryFailed(failedResult);
                            String errorMessage = result.getExceptionString();
                            handleNotLoggedInEvent(errorMessage);
                        }
                        if (callback.isHandleFailure()) {
                            callback.getDel().onSuccess(callback.getModel(), result);
                        }
                    } else {
                        callback.setOriginalReturnValue(result);
                        if (callback.getConverter() != null) {
                            callback.getDel().onSuccess(callback.getModel(),
                                    callback.getConverter().Convert(result.getReturnValue(), callback));
                        } else {
                            callback.getDel().onSuccess(callback.getModel(), result);
                        }
                    }
                } finally {
                    raiseQueryCompleteEvent(queryType, callback.getContext());
                }
            }

            @Override
            public void onFailure(final VdcOperation<VdcQueryType, VdcQueryParametersBase> operation,
                    final Throwable caught) {
                try {
                    if (ignoreFailure(caught)) {
                        return;
                    }
                    logger.log(Level.SEVERE, "Failed to execute runQuery: " + caught, caught); //$NON-NLS-1$
                    getEventsHandler().runQueryFailed(null);
                    failureEventHandler(caught);
                    if (callback.isHandleFailure()) {
                        callback.getDel().onSuccess(callback.getModel(), null);
                    }
                } finally {
                    raiseQueryCompleteEvent(queryType, callback.getContext());
                }
            }
        });
        // raise the query started event.
        raiseQueryStartedEvent(queryType, callback.getContext());
        if (isPublic) {
            getInstance().getOperationManager().addPublicOperation(operation);
        } else {
            getInstance().getOperationManager().addOperation(operation);
        }
    }

    /**
     * Static version.
     * Run a query that does not require the user to be logged in.
     * @param queryType The type of query.
     * @param parameters The parameter of the query.
     * @param callback The callback to when the query completes.
     */
    @Deprecated
    public static void RunPublicQuery(final VdcQueryType queryType,
            final VdcQueryParametersBase parameters,
            final AsyncQuery callback) {
        getInstance().runPublicQuery(queryType, parameters, callback);
    }

    /**
     * Run a query that does not require the user to be logged in.
     * @param queryType The type of query.
     * @param parameters The parameter of the query.
     * @param callback The callback to when the query completes.
     */
    public void runPublicQuery(final VdcQueryType queryType,
        final VdcQueryParametersBase parameters,
        final AsyncQuery callback) {
        runQuery(queryType, parameters, callback, true);
    }

    /**
     * Static version, deprecated.
     * Run multiple queries in a single request to the back-end. Do not supply a context.
     * @param queryTypeList A list of {@code VdcQueryType}s.
     * @param queryParamsList A list of parameters associated with each query.
     * @param callback The callback to call when the query completes.
     */
    @Deprecated
    public static void RunMultipleQueries(final List<VdcQueryType> queryTypeList,
            final List<VdcQueryParametersBase> queryParamsList,
            final IFrontendMultipleQueryAsyncCallback callback) {
        RunMultipleQueries(queryTypeList, queryParamsList, callback, null);
    }

    /**
     * Static version, deprecated.
     * Run multiple queries in a single request to the back-end.
     * @param queryTypeList A list of {@code VdcQueryType}s.
     * @param queryParamsList A list of parameters associated with each query.
     * @param callback The callback to call when the query completes.
     * @param context The context to run the queries in.
     */
    @Deprecated
    public static void RunMultipleQueries(final List<VdcQueryType> queryTypeList,
            final List<VdcQueryParametersBase> queryParamsList,
            final IFrontendMultipleQueryAsyncCallback callback,
            final String context) {
        getInstance().runMultipleQueries(queryTypeList, queryParamsList, callback, context);
    }

    /**
     * Run multiple queries in a single request to the back-end.
     * @param queryTypeList A list of {@code VdcQueryType}s.
     * @param queryParamsList A list of parameters associated with each query.
     * @param callback The callback to call when the query completes.
     * @param context The context to run the queries in.
     */
    public void runMultipleQueries(final List<VdcQueryType> queryTypeList,
            final List<VdcQueryParametersBase> queryParamsList,
            final IFrontendMultipleQueryAsyncCallback callback,
            final String context) {
        VdcOperationCallbackList<VdcOperation<VdcQueryType, VdcQueryParametersBase>,
            List<VdcQueryReturnValue>> multiCallback = new VdcOperationCallbackList<VdcOperation<VdcQueryType,
            VdcQueryParametersBase>, List<VdcQueryReturnValue>>() {

            @Override
            public void onSuccess(final List<VdcOperation<VdcQueryType, VdcQueryParametersBase>> operationList,
                    final List<VdcQueryReturnValue> resultObject) {
                logger.finer("Succesful returned result from runMultipleQueries!"); //$NON-NLS-1$
                FrontendMultipleQueryAsyncResult f =
                        new FrontendMultipleQueryAsyncResult(queryTypeList, queryParamsList, resultObject);
                callback.executed(f);
                raiseQueryCompleteEvent(queryTypeList, context);
            }

            @Override
            public void onFailure(final List<VdcOperation<VdcQueryType, VdcQueryParametersBase>> operationList,
                    final Throwable caught) {
                try {
                    if (ignoreFailure(caught)) {
                        return;
                    }
                    logger.log(Level.SEVERE, "Failed to execute runMultipleQueries: " + caught, caught); //$NON-NLS-1$
                    FrontendMultipleQueryAsyncResult f =
                            new FrontendMultipleQueryAsyncResult(queryTypeList, queryParamsList, null);
                    failureEventHandler(caught);
                    callback.executed(f);
                } finally {
                    raiseQueryCompleteEvent(queryTypeList, context);
                }
            }
        };
        List<VdcOperation<?, ?>> operationList = new ArrayList<VdcOperation<?, ?>>();
        for (int i = 0; i < queryTypeList.size(); i++) {
            VdcQueryParametersBase parameters = queryParamsList.get(i);
            parameters.setRefresh(false); // Why do we do this?
            initQueryParamsFilter(parameters);
            operationList.add(new VdcOperation<VdcQueryType, VdcQueryParametersBase>(queryTypeList.get(i),
                    parameters, multiCallback));
        }
        raiseQueryStartedEvent(queryTypeList, context);
        getInstance().getOperationManager().addOperationList(operationList);
    }

    /**
     * Static version deprecated.
     * Run an action of the specified action type using the passed in parameters. No object state.
     * @param actionType The action type of the action to perform.
     * @param parameters The parameters of the action.
     * @param callback The callback to call when the action is completed.
     */
    @Deprecated
    public static void RunAction(final VdcActionType actionType,
            final VdcActionParametersBase parameters,
            final IFrontendActionAsyncCallback callback) {
        RunAction(actionType, parameters, callback, null);
    }

    /**
     * Static version deprecated.
     * Run an action of the specified action type using the passed in parameters, also pass in a state object.
     * @param actionType The action type of the action to perform.
     * @param parameters The parameters of the action.
     * @param callback The callback to call when the action is completed.
     * @param state The state object.
     */
    @Deprecated
    public static void RunAction(final VdcActionType actionType,
            final VdcActionParametersBase parameters,
            final IFrontendActionAsyncCallback callback,
            final Object state) {
        RunAction(actionType, parameters, callback != null ? callback : NULLABLE_ASYNC_CALLBACK, state, true);
    }

    /**
     * Static version deprecated.
     * {@code RunAction} without callback.
     * @param actionType The action type of the action to run.
     * @param parameters The parameters to the action.
     * @param showErrorDialog Whether to show a pop-up dialog with the error or not.
     */
    @Deprecated
    public static void RunAction(final VdcActionType actionType, final VdcActionParametersBase parameters,
            final boolean showErrorDialog) {
        RunAction(actionType, parameters, Frontend.NULLABLE_ASYNC_CALLBACK, null, showErrorDialog);
    }

    /**
     * Static version deprecated.
     * {@code RunAction} without callback.
     * @param actionType The action type of the action to run.
     * @param parameters The parameters to the action.
     */
    @Deprecated
    public static void RunAction(final VdcActionType actionType, final VdcActionParametersBase parameters) {
        RunAction(actionType, parameters, Frontend.NULLABLE_ASYNC_CALLBACK);
    }

    /**
     * Static version deprecated.
     * Run an action of the specified action type using the passed in parameters, also pass in a state object.
     * @param actionType The action type of the action to perform.
     * @param parameters The parameters of the action.
     * @param callback The callback to call when the action is completed.
     * @param state The state object.
     * @param showErrorDialog Whether to show a pop-up dialog with the error or not.
     */
    @Deprecated
    public static void RunAction(final VdcActionType actionType,
            final VdcActionParametersBase parameters,
            final IFrontendActionAsyncCallback callback,
            final Object state,
            final boolean showErrorDialog) {
        getInstance().runAction(actionType, parameters, callback, state, showErrorDialog);
    }

    /**
     * Run an action of the specified action type using the passed in parameters, also pass in a state object.
     * @param actionType The action type of the action to perform.
     * @param parameters The parameters of the action.
     * @param callback The callback to call when the action is completed.
     * @param state The state object.
     * @param showErrorDialog Whether to show a pop-up dialog with the error or not.
     */
    public void runAction(final VdcActionType actionType,
            final VdcActionParametersBase parameters,
            final IFrontendActionAsyncCallback callback,
            final Object state,
            final boolean showErrorDialog) {
        VdcOperation<VdcActionType, VdcActionParametersBase> operation = new VdcOperation<VdcActionType,
                VdcActionParametersBase>(actionType, parameters, new VdcOperationCallback<VdcOperation<VdcActionType,
                        VdcActionParametersBase>, VdcReturnValueBase>() {

            @Override
            public void onSuccess(final VdcOperation<VdcActionType, VdcActionParametersBase> operation,
                    final VdcReturnValueBase result) {
                logger.finer("Frontend: sucessfully executed runAction, determining result!"); //$NON-NLS-1$
                handleActionResult(actionType, parameters, result,
                        callback != null ? callback : NULLABLE_ASYNC_CALLBACK, state, showErrorDialog);
            }

            @Override
            public void onFailure(final VdcOperation<VdcActionType, VdcActionParametersBase> operation,
                    final Throwable caught) {
                if (ignoreFailure(caught)) {
                    return;
                }
                logger.log(Level.SEVERE, "Failed to execute runAction: " + caught, caught); //$NON-NLS-1$
                failureEventHandler(caught);
                FrontendActionAsyncResult f = new FrontendActionAsyncResult(actionType, parameters, null, state);
                if (callback != null) {
                    callback.executed(f);
                }
            }
        });
        getInstance().getOperationManager().addOperation(operation);
    }

    /**
     * Identical to 5 parameter RunMultipleAction, but isRunOnlyIfAllCanDoPass is false.
     * @param actionType The action type of the actions to run.
     * @param parameters The parameters to the actions.
     * @param callback The callback to call after the operation happens.
     * @param state A state object.
     */
    @Deprecated
    public static void RunMultipleAction(final VdcActionType actionType,
            final ArrayList<VdcActionParametersBase> parameters,
            final IFrontendMultipleActionAsyncCallback callback,
            final Object state) {
        RunMultipleAction(actionType, parameters, false, callback, state);
    }

    /**
     * RunMultipleActions without passing in a callback or state.
     * @param actionType The type of action to perform.
     * @param parameters The parameters of the action.
     */
    @Deprecated
    public static void RunMultipleAction(final VdcActionType actionType,
            final ArrayList<VdcActionParametersBase> parameters) {
        RunMultipleAction(actionType, parameters, null, null);
    }

    /**
     * Overloaded method for {@link #RunMultipleActions(VdcActionType, List, IFrontendActionAsyncCallback, Object)} with
     * state = null.
     * @param actionType The action type of the actions.
     * @param parameters A list of parameters, once for each action.
     * @param successCallback The callback to call on success.
     */
    @Deprecated
    public static void RunMultipleActions(final VdcActionType actionType,
            final List<VdcActionParametersBase> parameters,
            final IFrontendActionAsyncCallback successCallback) {
        RunMultipleActions(actionType, parameters, successCallback, null);
    }

    /**
     * Static version.
     * Run multiple actions using the same {@code VdcActionType}.
     * @param actionType The action type.
     * @param parameters The list of parameters.
     * @param isRunOnlyIfAllCanDoPass A flag to only run the actions if all can be completed.
     * @param callback The callback to call when the operation completes.
     * @param state The state.
     */
    @Deprecated
    public static void RunMultipleAction(final VdcActionType actionType,
            final ArrayList<VdcActionParametersBase> parameters,
            final boolean isRunOnlyIfAllCanDoPass,
            final IFrontendMultipleActionAsyncCallback callback,
            final Object state) {
        getInstance().runMultipleAction(actionType, parameters, isRunOnlyIfAllCanDoPass, callback, state);
    }

    /**
     * Run multiple actions using the same {@code VdcActionType}.
     * @param actionType The action type.
     * @param parameters The list of parameters.
     * @param isRunOnlyIfAllCanDoPass A flag to only run the actions if all can be completed.
     * @param callback The callback to call when the operation completes.
     * @param state The state.
     */
    public void runMultipleAction(final VdcActionType actionType,
            final List<VdcActionParametersBase> parameters,
            final boolean isRunOnlyIfAllCanDoPass,
            final IFrontendMultipleActionAsyncCallback callback,
            final Object state) {
        VdcOperationCallbackList<VdcOperation<VdcActionType, VdcActionParametersBase>, List<VdcReturnValueBase>>
        multiCallback = new VdcOperationCallbackList<VdcOperation<VdcActionType, VdcActionParametersBase>,
        List<VdcReturnValueBase>>() {
            @Override
            public void onSuccess(final List<VdcOperation<VdcActionType, VdcActionParametersBase>> operationList,
                    final List<VdcReturnValueBase> resultObject) {
                logger.finer("Frontend: successfully executed runMultipleAction, determining result!"); //$NON-NLS-1$

                ArrayList<VdcReturnValueBase> failed = new ArrayList<VdcReturnValueBase>();

                for (VdcReturnValueBase v : resultObject) {
                    if (!v.getCanDoAction()) {
                        failed.add(v);
                    }
                }

                if (!failed.isEmpty()) {
                    translateErrors(failed);
                    getEventsHandler().runMultipleActionFailed(actionType, failed);
                }

                if (callback != null) {
                    callback.executed(new FrontendMultipleActionAsyncResult(actionType,
                            parameters, resultObject, state));
                }
            }

            @Override
            public void onFailure(final List<VdcOperation<VdcActionType, VdcActionParametersBase>> operation,
                    final Throwable caught) {
                if (ignoreFailure(caught)) {
                    return;
                }
                logger.log(Level.SEVERE, "Failed to execute runMultipleAction: " + caught, caught); //$NON-NLS-1$
                failureEventHandler(caught);

                if (callback != null) {
                    callback.executed(new FrontendMultipleActionAsyncResult(actionType, parameters, null,
                            state));
                }
            }
        };
        List<VdcOperation<?, ?>> operationList = new ArrayList<VdcOperation<?, ?>>();
        for (VdcActionParametersBase parameter: parameters) {
            VdcOperation<VdcActionType, VdcActionParametersBase> operation = new VdcOperation<VdcActionType,
                    VdcActionParametersBase>(actionType, parameter, multiCallback);
            operationList.add(operation);
        }
        if (operationList.isEmpty()) {
            //Someone called run multiple actions with a single action without parameters. The backend will return
            //an empty return value as there are no parameters, so we can skip the round trip to the server and return
            //it ourselves.
            if (scheduler == null) {
                scheduler = Scheduler.get();
            }
            scheduler.scheduleDeferred(new ScheduledCommand() {
                @Override
                public void execute() {
                    if (callback != null) {
                        List<VdcReturnValueBase> emptyResult = new ArrayList<VdcReturnValueBase>();
                        callback.executed(new FrontendMultipleActionAsyncResult(actionType,
                                parameters, emptyResult, state));
                    }
                }
            });
        } else {
            getOperationManager().addOperationList(operationList);
        }
    }

    /**
     * Overloaded method for {@link #RunMultipleActions(VdcActionType, List, List, Object)} with state = null.
     * @param actionType
     *            the action to be repeated.
     * @param parameters
     *            the parameters of each action.
     * @param callbacks A list of callbacks.
     */
    public static void RunMultipleActions(VdcActionType actionType,
            List<VdcActionParametersBase> parameters,
            List<IFrontendActionAsyncCallback> callbacks) {
        RunMultipleActions(actionType, parameters, callbacks, null);
    }

    /**
     * A convenience method that calls {@link #RunMultipleActions(VdcActionType, List, List, Object)} with just a single
     * callback to be called when all actions have succeeded.
     *
     * @param actionType
     *            the action to be repeated.
     * @param parameters
     *            the parameters of each action.
     * @param successCallback
     *            the callback to be executed when all actions have succeeded.
     * @param state State object
     */
    @Deprecated
    public static void RunMultipleActions(final VdcActionType actionType,
            final List<VdcActionParametersBase> parameters,
            final IFrontendActionAsyncCallback successCallback,
            final Object state) {

        if (parameters == null || parameters.isEmpty()) {
            return;
        }
        int n = parameters.size();
        IFrontendActionAsyncCallback[] callbacks = new IFrontendActionAsyncCallback[n];
        callbacks[n - 1] = successCallback;
        RunMultipleActions(actionType, parameters,
                new LinkedList<IFrontendActionAsyncCallback>(Arrays.asList(callbacks)),
                state);
    }

    /**
     * A convenience method that calls
     * {@link #RunMultipleActions(List, List, List, IFrontendActionAsyncCallback, Object)} with just the one
     * VdcActionType for all actions.
     *
     * @param actionType
     *            the action to be repeated.
     * @param parameters
     *            the parameters of each action.
     * @param callbacks
     *            the callback to be executed upon the success of each action.
     * @param state
     *            the state.
     */
    public static void RunMultipleActions(VdcActionType actionType,
            List<VdcActionParametersBase> parameters,
            List<IFrontendActionAsyncCallback> callbacks,
            Object state) {

        if (parameters == null || parameters.isEmpty()) {
            return;
        }
        VdcActionType[] actionTypes = new VdcActionType[parameters.size()];
        Arrays.fill(actionTypes, actionType);
        RunMultipleActions(new LinkedList<VdcActionType>(Arrays.asList(actionTypes)),
                parameters,
                callbacks,
                null,
                state);
    }

    /**
     * Static version.
     * This method allows us to run a transaction like set of actions. If one
     * fails the rest do not get executed.
     * @param actionTypes The list of actions to execute.
     * @param parameters The list of parameters, must match the number of actions.
     * @param callbacks The list of callbacks, the number must match the number of actions.
     * @param failureCallback The callback to call in case of failure.
     * @param state The state.
     */
    @Deprecated
    public static void RunMultipleActions(final List<VdcActionType> actionTypes,
            final List<VdcActionParametersBase> parameters,
            final List<IFrontendActionAsyncCallback> callbacks,
            final IFrontendActionAsyncCallback failureCallback,
            final Object state) {
        getInstance().runMultipleActions(actionTypes, parameters, callbacks, failureCallback, state);
    }

    /**
     * This method allows us to run a transaction like set of actions. If one
     * fails the rest do not get executed.
     * @param actionTypes The list of actions to execute.
     * @param parameters The list of parameters, must match the number of actions.
     * @param callbacks The list of callbacks, the number must match the number of actions.
     * @param failureCallback The callback to call in case of failure.
     * @param state The state.
     */
    public void runMultipleActions(final List<VdcActionType> actionTypes,
            final List<VdcActionParametersBase> parameters,
            final List<IFrontendActionAsyncCallback> callbacks,
            final IFrontendActionAsyncCallback failureCallback,
            final Object state) {
        if (actionTypes.isEmpty() || parameters.isEmpty() || callbacks.isEmpty())
        {
            return;
        }

        runAction(actionTypes.get(0), parameters.get(0),
            new IFrontendActionAsyncCallback() {
                @Override
                public void executed(final FrontendActionAsyncResult result) {
                    VdcReturnValueBase returnValue = result.getReturnValue();
                    boolean success = returnValue != null && returnValue.getSucceeded();
                    if (success || failureCallback == null) {
                        IFrontendActionAsyncCallback callback = callbacks.get(0);
                        if (callback != null) {
                            callback.executed(result);
                        }
                        actionTypes.remove(0);
                        parameters.remove(0);
                        callbacks.remove(0);
                        runMultipleActions(actionTypes, parameters, callbacks, failureCallback, state);
                    } else {
                        failureCallback.executed(result);
                    }
                }
            }, state, true);
    }

    /**
     * Static version.
     * ASynchronous user login.
     * @param userName The name of the user.
     * @param password The password of the user.
     * @param domain The domain to check for the user.
     * @param isAdmin If the user an admin user.
     * @param callback The callback to call when the operation is finished.
     */
    @Deprecated
    public static void LoginAsync(final String userName,
            final String password,
            final String domain,
            final boolean isAdmin,
            final AsyncQuery callback) {
        getInstance().loginAsync(userName, password, domain, isAdmin, callback);
    }

    /**
     * ASynchronous user login.
     * @param userName The name of the user.
     * @param password The password of the user.
     * @param domain The domain to check for the user.
     * @param isAdmin If the user an admin user.
     * @param callback The callback to call when the operation is finished.
     */
    public void loginAsync(final String userName,
            final String password,
            final String domain,
            final boolean isAdmin,
            final AsyncQuery callback) {
        logger.finer("Frontend: Invoking async Login."); //$NON-NLS-1$
        LoginUserParameters params = new LoginUserParameters(userName, password, domain, null, null, null);
        VdcActionType action = isAdmin ? VdcActionType.LoginAdminUser : VdcActionType.LoginUser;
        VdcOperation<VdcActionType, LoginUserParameters> loginOperation =
            new VdcOperation<VdcActionType, LoginUserParameters>(action, params, true, //Public action.
                    new VdcOperationCallback<VdcOperation<VdcActionType, LoginUserParameters>,
                    VdcReturnValueBase>() {

                @Override
                public void onSuccess(final VdcOperation<VdcActionType, LoginUserParameters> operation,
                        final VdcReturnValueBase result) {
                    logger.finer("Succesful returned result from Login."); //$NON-NLS-1$
                    getInstance().setLoggedInUser((DbUser) result.getActionReturnValue());
                    result.setCanDoActionMessages((ArrayList<String>) translateError(result));
                    callback.getDel().onSuccess(callback.getModel(), result);
                    if (getInstance().getLoginHandler() != null && result.getSucceeded()) {
                        getInstance().getLoginHandler().onLoginSuccess(userName, password, domain);
                    }
                }

                @Override
                public void onFailure(final VdcOperation<VdcActionType, LoginUserParameters> operation,
                        final Throwable caught) {
                    if (ignoreFailure(caught)) {
                        return;
                    }
                    logger.log(Level.SEVERE, "Failed to login: " + caught, caught); //$NON-NLS-1$
                    getEventsHandler().runQueryFailed(null);
                    failureEventHandler(caught);
                    clearLoggedInUser();
                    if (callback.isHandleFailure()) {
                        getInstance().setLoggedInUser(null);
                        callback.getDel().onSuccess(callback.getModel(), null);
                    }
                }
        });
        getInstance().getOperationManager().loginUser(loginOperation);
    }

    /**
     * Static version of the log off method.
     * @param dbUser The user object to use to log off.
     * @param callback The callback to call when the user is logged off.
     */
    @Deprecated
    public static void LogoffAsync(final DbUser dbUser, final AsyncQuery callback) {
        getInstance().logoffAsync(dbUser, callback);
    }

    /**
     * Log off the currently logged in user.
     * @param dbUser The user object to use to log off.
     * @param callback The callback to call when the user is logged off.
     */
    public void logoffAsync(final DbUser dbUser, final AsyncQuery callback) {
        logger.finer("Frontend: Invoking async logoff."); //$NON-NLS-1$

        getInstance().getOperationManager().logoutUser(dbUser, new UserCallback<VdcReturnValueBase>() {
            @Override
            public void onSuccess(final VdcReturnValueBase result) {
                logger.finer("Succesful returned result from logoff."); //$NON-NLS-1$
                callback.getDel().onSuccess(callback.getModel(), result);
                if (getInstance().getLoginHandler() != null) {
                    getInstance().getLoginHandler().onLogout();
                }
            }

            @Override
            public void onFailure(final Throwable caught) {
                if (ignoreFailure(caught)) {
                    return;
                }
                logger.log(Level.SEVERE, "Failed to execute logoff: " + caught, caught); //$NON-NLS-1$
                getEventsHandler().runQueryFailed(null);
                failureEventHandler(caught);
                callback.getDel().onSuccess(callback.getModel(), null);
            }
        });
    }

    /**
     * Checks if the user is logged.
     * @return {@code true} if the user is logged in, false otherwise.
     */
    public Boolean getIsUserLoggedIn() {
        return getLoggedInUser() != null;
    }

    /**
     * Initializes the currently logged in user.
     * @param loggedUser A {@code DbUser} object.
     * @param loginPassword The login password associated with that user.
     */
    public void initLoggedInUser(DbUser loggedUser, String loginPassword) {
        this.currentUser = loggedUser;
        this.currentPassword = loginPassword;
    }

    /**
     * Un-set the current logged in user.
     */
    public void clearLoggedInUser() {
        initLoggedInUser(null, null);
    }

    /**
     * Get the logged in user.
     * @return A {@code DbUser} object defining the user.
     */
    public DbUser getLoggedInUser() {
        return currentUser;
    }

    /**
     * Get the current login password.
     * @return The current login password as a {@code String}.
     */
    public String getLoginPassword() {
        return currentPassword;
    }

    /**
     * The user was logged in externally, we need to tell the frontend the user is logged in, otherwise it will
     * reject any operations that are not public.
     * @param loggedInUser The {@code VdcUser} object defining the user.
     */
    public void setLoggedInUser(final DbUser loggedInUser) {
        this.currentUser = loggedInUser;
        if (currentUser != null) {
            getOperationManager().setLoggedIn(true);
        } else {
            getOperationManager().setLoggedIn(false);
        }
    }

    // TODO: Externalize to a better location, should support translation via
    // resource bundle file.
    /**
     * Return the first message from the list of passed in messages.
     * @param messages A list of message, which can be empty.
     * @return The first message from the list or 'No Message' if the list is empty.
     */
    private String getRunActionErrorMessage(final List<String> messages) {
        if (messages.size() < 1) {
            return "No Message"; //$NON-NLS-1$
        } else {
            return messages.iterator().next();
        }
    }

// Use for debugging only.
//    private void dumpQueryDetails(VdcQueryType queryType, VdcQueryParametersBase searchParameters) {
//        StringBuffer sb = new StringBuffer();
//        sb.append("VdcQuery Type: '" + queryType + "', "); //$NON-NLS-1$//$NON-NLS-2$
//        if (searchParameters instanceof SearchParameters) {
//            SearchParameters sp = (SearchParameters) searchParameters;
//
//            if (sp.getSearchPattern().equals("Not implemented")) { //$NON-NLS-1$
//                throw new RuntimeException("Search pattern is defined as 'Not implemented',
//                    probably because of a use of String.format()"); //$NON-NLS-1$
//            }
//
//            sb.append("Type value: [" + sp.getSearchTypeValue() + "], Pattern: [" + //$NON-NLS-1$//$NON-NLS-2$
//                    sp.getSearchPattern() + "]"); //$NON-NLS-1$
//        } else {
//            sb.append("Search type is base or unknown"); //$NON-NLS-1$
//        }
//
//        logger.fine(sb.toString());
//    }

// Use for debugging only.
//    private void dumpActionDetails(VdcActionType actionType, VdcActionParametersBase parameters) {
//        StringBuffer sb = new StringBuffer();
//        sb.append("actionType Type: '" + actionType + "', "); //$NON-NLS-1$//$NON-NLS-2$
//        sb.append("Params: " + parameters); //$NON-NLS-1$
//
//        logger.fine(sb.toString());
//    }
//
    /**
     * Handle the result(s) of an action.
     * @param actionType The action type.
     * @param parameters The parameters of the action.
     * @param result The result of the action.
     * @param callback The callback to call.
     * @param state The state before the action happened.
     * @param showErrorDialog Should we show an error dialog?
     */
    void handleActionResult(final VdcActionType actionType, final VdcActionParametersBase parameters,
            final VdcReturnValueBase result, final IFrontendActionAsyncCallback callback,
            final Object state, final boolean showErrorDialog) {
        logger.log(Level.FINER, "Retrieved action result from RunAction."); //$NON-NLS-1$

        FrontendActionAsyncResult f = new FrontendActionAsyncResult(actionType, parameters, result, state);
        boolean success = false;
        if (!result.getCanDoAction()) {
            result.setCanDoActionMessages((ArrayList<String>) translateError(result));
            callback.executed(f);
        } else if (showErrorDialog && result.getIsSyncronious() && !result.getSucceeded()) {
            runActionExecutionFailed(actionType, result.getFault());
            callback.executed(f);

            // Prevent another (untranslated) error message pop-up display
            // ('runActionExecutionFailed' invokes an error pop-up displaying,
            // therefore calling 'failureEventHandler' is redundant)
            success = true;
        } else {
            success = true;
            callback.executed(f);
        }

        if ((!success) && (getEventsHandler() != null)
                && (getEventsHandler().isRaiseErrorModalPanel(actionType, result.getFault()))) {
            if (result.getCanDoActionMessages().size() <= 1) {
                String errorMessage = !result.getCanDoAction() || !result.getCanDoActionMessages().isEmpty()
                        ? getRunActionErrorMessage(result.getCanDoActionMessages()) : result.getFault().getMessage();

                failureEventHandler(result.getDescription(), errorMessage);
            } else {
                failureEventHandler(result.getDescription(), result.getCanDoActionMessages());
            }
        }
    }

    /**
     * Get the current context.
     * @return The current context
     */
    @Deprecated
    public static String getCurrentContext() {
        return getInstance().currentContext;
    }

    /**
     * Raise a query event.
     * @param queryEvent The event to raise.
     * @param queryType The query type of the event.
     * @param context The context.
     */
    private void raiseQueryEvent(final Event queryEvent, final VdcQueryType queryType, final String context) {
        if (context != null && subscribedQueryTypes != null) {
            for (VdcQueryType vdcQueryType : subscribedQueryTypes) {
                if (queryType.equals(vdcQueryType)) {
                    currentContext = context;
                    queryEvent.raise(Frontend.class, EventArgs.Empty);
                }
            }
        }
    }

    /**
     * Raise the query started event.
     * @param queryType The type of query.
     * @param context The context in which the query is executed.
     */
    private void raiseQueryStartedEvent(final VdcQueryType queryType, final String context) {
        raiseQueryEvent(getQueryStartedEvent(), queryType, context);
    }

    /**
     * Raise the query started event. for a list of events.
     * @param queryTypeList A list of query types.
     * @param context The context in which the query is executed.
     */
    private void raiseQueryStartedEvent(final List<VdcQueryType> queryTypeList, final String context) {
        for (VdcQueryType queryType : queryTypeList) {
            raiseQueryStartedEvent(queryType, context);
        }
    }

    /**
     * Raise the query complete event.
     * @param queryType The type of query.
     * @param context The context in which the query was completed.
     */
    private void raiseQueryCompleteEvent(final VdcQueryType queryType, final String context) {
        raiseQueryEvent(getQueryCompleteEvent(), queryType, context);
    }

    /**
     * Raise the query complete event for a list of events.
     * @param queryTypeList A list of query types.
     * @param context The context in which the query was completed.
     */
    private void raiseQueryCompleteEvent(final List<VdcQueryType> queryTypeList, final String context) {
        for (VdcQueryType queryType : queryTypeList) {
            raiseQueryCompleteEvent(queryType, context);
        }
    }

    /**
     * execute the event handler when an action failed.
     * @param actionType The type of action that failed.
     * @param fault The reason the action failed.
     */
    private void runActionExecutionFailed(final VdcActionType actionType, final VdcFault fault) {
        if (getEventsHandler() != null) {
            // The VdcFault error property takes precedence, if it's null we try to translate the message property
            String translatedMessage = translateVdcFault(fault);
            fault.setMessage(translatedMessage);
            getEventsHandler().runActionExecutionFailed(actionType, fault);
        }
    }

    /**
     * Translate the action failure fault.
     * @param fault The fault to translate.
     * @return A translated string defining the reason for the failure.
     */
    public String translateVdcFault(final VdcFault fault) {
        return getVdsmErrorsTranslator().TranslateErrorTextSingle(fault.getError() == null
                ? fault.getMessage() : fault.getError().toString());
    }

    /**
     * Getter for the VDSM error translator.
     * @return The {@code ErrorTranslator}
     */
    public ErrorTranslator getVdsmErrorsTranslator() {
        return vdsmErrorsTranslator;
    }

    /**
     * Setter for the VDSM error translator.
     * @param translator The translator.
     */
    protected void setVdsmErrorsTranslator(final ErrorTranslator translator) {
        vdsmErrorsTranslator = translator;
    }

    /**
     * Subscribed to query types. Clear any existing types.
     * @param queryTypes An array of query types to subscribe to.
     */
    public void subscribe(final VdcQueryType[] queryTypes) {
        subscribedQueryTypes.clear();
        Collections.addAll(subscribedQueryTypes, queryTypes);
    }

    /**
     * Subscribe to additional query types, keep existing types.
     * @param queryTypes An array of query types to subscribe to.
     */
    public void subscribeAdditionalQueries(final VdcQueryType[] queryTypes) {
        Collections.addAll(subscribedQueryTypes, queryTypes);
    }

    /**
     * Un-subscribe all query types.
     */
    public void unsubscribe() {
        subscribedQueryTypes.clear();
        currentContext = null;

        queryStartedEvent.getListeners().clear();
        queryCompleteEvent.getListeners().clear();
    }

    /**
     * Getter for the query started event.
     * @return An {@code Event} defining the query started event.
     */
    public Event getQueryStartedEvent() {
        return queryStartedEvent;
    }

    /**
     * Getter for the query completed event.
     * @return An {@code Event} defining the query complete event.
     */
    public Event getQueryCompleteEvent() {
        return queryCompleteEvent;
    }

    /**
     * Check if we should ignore the passed in {@code Throwable}.
     * @param caught The {@code Throwable} to check.
     * @return {@code true} if the {@code Throwable} should be ignore, false otherwise.
     */
    protected boolean ignoreFailure(final Throwable caught) {
        // ignore escape key
        if (caught instanceof StatusCodeException && ((StatusCodeException)
                caught).getStatusCode() == 0) {
            return true;
        }
        return false;
    }

    /**
     * Empty implementation of IFrontendActionAsyncCallback.
     */
    private static final class NullableFrontendActionAsyncCallback implements IFrontendActionAsyncCallback {
        @Override
        public void executed(final FrontendActionAsyncResult result) {
            //Do nothing, we are the 'NullableFrontendActionAsyncCallback'
        }
    }

    /**
     * empty callback. use this when we don't want anything called when an operation completes.
     */
    private static final NullableFrontendActionAsyncCallback NULLABLE_ASYNC_CALLBACK =
            new NullableFrontendActionAsyncCallback();

    /**
     * Getter for queryStartedEventDefinition.
     * @return an {@code EventDefinition} defining the query start event.
     */
    public EventDefinition getQueryStartedEventDefinition() {
        return queryStartedEventDefinition;
    }

    /**
     * Getter for queryCompleteEventDefinition.
     * @return an {@code EventDefinition} defining the query complete event.
     */
    public EventDefinition getQueryCompleteEventDefinition() {
        return queryCompleteEventDefinition;
    }

    /**
     * Getter for the frontend failure event.
     * @return an {@code Event} for the frontend failure.
     */
    public Event getFrontendFailureEvent() {
        return frontendFailureEvent;
    }

    /**
     * Getter for the frontend not logged in event.
     * @return an {@code Event} for the not logged in event.
     */
    public Event getFrontendNotLoggedInEvent() {
        return frontendNotLoggedInEvent;
    }

    /**
     * Getter for the events handler.
     * @return The events handler.
     */
    public IFrontendEventsHandler getEventsHandler() {
        return eventsHandler;
    }

    /**
     * Setter for the events handler.
     * @param frontendEventsHandler The new events handler.
     */
    public void setEventsHandler(final IFrontendEventsHandler frontendEventsHandler) {
        this.eventsHandler = frontendEventsHandler;
    }

    /**
     * Getter for the login handler.
     * @return The login handler.
     */
    public FrontendLoginHandler getLoginHandler() {
        return loginHandler;
    }

    /**
     * Setter for the login handler.
     * @param frontendLoginHandler The new login handler.
     */
    public void setLoginHandler(final FrontendLoginHandler frontendLoginHandler) {
        this.loginHandler = frontendLoginHandler;
    }

    /**
     * Getter for the Application error translator.
     * @return An {@code ErrorTranslator} that can translate application errors.
     */
    public ErrorTranslator getAppErrorsTranslator() {
        return canDoActionErrorsTranslator;
    }

    /**
     * Setter for the application error translator.
     * @param translator The new translator
     */
    protected void setAppErrorsTranslator(final ErrorTranslator translator) {
        canDoActionErrorsTranslator = translator;
    }

    /**
     * Translate application errors and store the translated messages back in return values.
     * @param errors A list of {@code VdcReturnValueBase}s.
     */
    private void translateErrors(final List<VdcReturnValueBase> errors) {
        for (VdcReturnValueBase retVal : errors) {
            retVal.setCanDoActionMessages((ArrayList<String>) translateError(retVal));
        }
    }

    /**
     * Translate a single application error and store the translated message back in the return value object.
     * @param error The {@code VdcReturnValueBase} error object.
     * @return A {@code List} of translated messages.
     */
    private List<String> translateError(final VdcReturnValueBase error) {
        return getAppErrorsTranslator().translateErrorText(error.getCanDoActionMessages());
    }
    /**
     * Call failure event handler with the passed in message.
     * @param errorMessage The error message.
     */
    private void failureEventHandler(final String errorMessage) {
        failureEventHandler(null, errorMessage);
    }

    /**
     * Call failure event handler based on the {@code Throwable} passed in.
     * @param caught The {@code Throwable}
     */
    private void failureEventHandler(final Throwable caught) {
        String errorMessage;
        if (caught instanceof StatusCodeException) {
            errorMessage = getConstants().requestToServerFailedWithCode() + ": " //$NON-NLS-1$
                    + ((StatusCodeException) caught).getStatusCode();
        } else {
            errorMessage =
                    getConstants().requestToServerFailed()
                            + ": " + caught.getLocalizedMessage(); //$NON-NLS-1$
        }
        failureEventHandler(errorMessage);
    }

    /**
     * Call failure event handler with the passed in description and message.
     * @param description A description of the failure (nullable)
     * @param errorMessage The error message.
     */
    private void failureEventHandler(final String description, final String errorMessage) {
        handleNotLoggedInEvent(errorMessage);
        frontendFailureEvent.raise(Frontend.class, new FrontendFailureEventArgs(
                new Message(description, errorMessage)));
    }

    /**
     * Call failure event handler with the passed in description and list of error messages.
     * @param description A description of the failure (nullable)
     * @param errorMessages A list of error messages.
     */
    private void failureEventHandler(final String description, final ArrayList<String> errorMessages) {
        ArrayList<Message> messages = new ArrayList<Message>();
        for (String errorMessage : errorMessages) {
            messages.add(new Message(description, errorMessage));
        }

        frontendFailureEvent.raise(Frontend.class, new FrontendFailureEventArgs(messages));
    }

    /**
     * Setter for constants.
     * @param uiConstants The constants to set.
     */
    public void setConstants(final UIConstants uiConstants) {
        constants = uiConstants;
    }

    /**
     * Get a copy of the UIConstants.
     * @return The UIConstants object.
     */
    private UIConstants getConstants() {
        if (constants == null) {
            constants = ConstantsManager.getInstance().getConstants();
        }
        return constants;
    }

    /**
     * Call the not logged in event handler.
     * @param errorMessage The error message.
     */
    private void handleNotLoggedInEvent(final String errorMessage) {
        if (errorMessage != null && errorMessage.equals("USER_IS_NOT_LOGGED_IN")) { //$NON-NLS-1$
            frontendNotLoggedInEvent.raise(Frontend.class, EventArgs.Empty);
        }
    }

    /**
     * Setter for filterQueries.
     * @param shouldFilterQueries should queries be filtered, true or false.
     */
    public void setFilterQueries(final boolean shouldFilterQueries) {
        this.filterQueries = shouldFilterQueries;
    }

    /**
     * set the filterQueries parameter.
     * @param parameters The parameters to set.
     */
    private void initQueryParamsFilter(final VdcQueryParametersBase parameters) {
        parameters.setFiltered(filterQueries);
    }
}
