/**
 * Copyright (c) 2010 Yahoo! Inc. All rights reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License. See accompanying LICENSE file.
 */
package org.apache.oozie;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.hadoop.conf.Configuration;
import org.apache.oozie.client.CoordinatorJob;
import org.apache.oozie.client.Job;
import org.apache.oozie.client.OozieClient;
import org.apache.oozie.client.WorkflowJob;
import org.apache.oozie.command.CommandException;
import org.apache.oozie.command.bundle.BundleJobChangeXCommand;
import org.apache.oozie.command.bundle.BundleJobResumeXCommand;
import org.apache.oozie.command.bundle.BundleJobSuspendXCommand;
import org.apache.oozie.command.bundle.BundleJobXCommand;
import org.apache.oozie.command.bundle.BundleJobsXCommand;
import org.apache.oozie.command.bundle.BundleKillXCommand;
import org.apache.oozie.command.bundle.BundleRerunXCommand;
import org.apache.oozie.command.bundle.BundleStartXCommand;
import org.apache.oozie.command.bundle.BundleSubmitXCommand;
import org.apache.oozie.service.DagXLogInfoService;
import org.apache.oozie.service.Services;
import org.apache.oozie.service.XLogService;
import org.apache.oozie.util.ParamChecker;
import org.apache.oozie.util.XLog;
import org.apache.oozie.util.XLogStreamer;

public class BundleEngine extends BaseEngine {
    /**
     * Create a system Bundle engine, with no user and no group.
     */
    public BundleEngine() {
    }

    /**
     * Create a Bundle engine to perform operations on behave of a user.
     *
     * @param user user name.
     * @param authToken the authentication token.
     */
    public BundleEngine(String user, String authToken) {
        this.user = ParamChecker.notEmpty(user, "user");
        this.authToken = ParamChecker.notEmpty(authToken, "authToken");
    }

    /* (non-Javadoc)
     * @see org.apache.oozie.BaseEngine#change(java.lang.String, java.lang.String)
     */
    @Override
    public void change(String jobId, String changeValue) throws BundleEngineException {
        try {
            BundleJobChangeXCommand change = new BundleJobChangeXCommand(jobId, changeValue);
            change.call();
        }
        catch (CommandException ex) {
            throw new BundleEngineException(ex);
        }
    }

    /* (non-Javadoc)
     * @see org.apache.oozie.BaseEngine#dryrunSubmit(org.apache.hadoop.conf.Configuration, boolean)
     */
    @Override
    public String dryrunSubmit(Configuration conf, boolean startJob) throws BundleEngineException {
        BundleSubmitXCommand submit = new BundleSubmitXCommand(true, conf, getAuthToken());
        try {
            String jobId = submit.call();
            return jobId;
        }
        catch (CommandException ex) {
            throw new BundleEngineException(ex);
        }
    }

    /* (non-Javadoc)
     * @see org.apache.oozie.BaseEngine#getCoordJob(java.lang.String)
     */
    @Override
    public CoordinatorJob getCoordJob(String jobId) throws BundleEngineException {
        throw new BundleEngineException(new XException(ErrorCode.E0301));
    }

    public BundleJobBean getBundleJob(String jobId) throws BundleEngineException {
        try {
            return new BundleJobXCommand(jobId).call();
        }
        catch (CommandException ex) {
            throw new BundleEngineException(ex);
        }
    }

    /* (non-Javadoc)
     * @see org.apache.oozie.BaseEngine#getCoordJob(java.lang.String, int, int)
     */
    @Override
    public CoordinatorJob getCoordJob(String jobId, int start, int length) throws BundleEngineException {
        throw new BundleEngineException(new XException(ErrorCode.E0301));
    }

    /* (non-Javadoc)
     * @see org.apache.oozie.BaseEngine#getDefinition(java.lang.String)
     */
    @Override
    public String getDefinition(String jobId) throws BundleEngineException {
        BundleJobBean job;
        try {
            job = new BundleJobXCommand(jobId).call();
        }
        catch (CommandException ex) {
            throw new BundleEngineException(ex);
        }
        return job.getOrigJobXml();
    }

    /* (non-Javadoc)
     * @see org.apache.oozie.BaseEngine#getJob(java.lang.String)
     */
    @Override
    public WorkflowJob getJob(String jobId) throws BundleEngineException {
        throw new BundleEngineException(new XException(ErrorCode.E0301));
    }

    /* (non-Javadoc)
     * @see org.apache.oozie.BaseEngine#getJob(java.lang.String, int, int)
     */
    @Override
    public WorkflowJob getJob(String jobId, int start, int length) throws BundleEngineException {
        throw new BundleEngineException(new XException(ErrorCode.E0301));
    }

    /* (non-Javadoc)
     * @see org.apache.oozie.BaseEngine#getJobIdForExternalId(java.lang.String)
     */
    @Override
    public String getJobIdForExternalId(String externalId) throws BundleEngineException {
        return null;
    }

    /* (non-Javadoc)
     * @see org.apache.oozie.BaseEngine#kill(java.lang.String)
     */
    @Override
    public void kill(String jobId) throws BundleEngineException {
        try {
            new BundleKillXCommand(jobId).call();
        }
        catch (CommandException e) {
            throw new BundleEngineException(e);
        }
    }

    /* (non-Javadoc)
     * @see org.apache.oozie.BaseEngine#reRun(java.lang.String, org.apache.hadoop.conf.Configuration)
     */
    @Override
    @Deprecated
    public void reRun(String jobId, Configuration conf) throws BundleEngineException {
        throw new BundleEngineException(new XException(ErrorCode.E0301));
    }

    /**
     * Rerun Bundle actions for given rerunType
     *
     * @param jobId bundle job id
     * @param coordScope the rerun scope for coordinator job names separated by ","
     * @param dateScope the rerun scope for coordinator nominal times separated by ","
     * @param refresh true if user wants to refresh input/outpur dataset urls
     * @param noCleanup false if user wants to cleanup output events for given rerun actions
     * @throws BaseEngineException thrown if failed to rerun
     */
    public void reRun(String jobId, String coordScope, String dateScope, boolean refresh, boolean noCleanup)
            throws BaseEngineException {
        try {
            new BundleRerunXCommand(jobId, coordScope, dateScope, refresh, noCleanup).call();
        }
        catch (CommandException ex) {
            throw new BaseEngineException(ex);
        }
    }

    /* (non-Javadoc)
     * @see org.apache.oozie.BaseEngine#resume(java.lang.String)
     */
    @Override
    public void resume(String jobId) throws BundleEngineException {
        BundleJobResumeXCommand resume = new BundleJobResumeXCommand(jobId);
        try {
            resume.call();
        }
        catch (CommandException ex) {
            throw new BundleEngineException(ex);
        }
    }

    /* (non-Javadoc)
     * @see org.apache.oozie.BaseEngine#start(java.lang.String)
     */
    @Override
    public void start(String jobId) throws BundleEngineException {
        try {
            new BundleStartXCommand(jobId).call();
        }
        catch (CommandException e) {
            throw new BundleEngineException(e);
        }
    }

    /* (non-Javadoc)
     * @see org.apache.oozie.BaseEngine#streamLog(java.lang.String, java.io.Writer)
     */
    @Override
    public void streamLog(String jobId, Writer writer) throws IOException, BundleEngineException {
        XLogStreamer.Filter filter = new XLogStreamer.Filter();
        filter.setParameter(DagXLogInfoService.JOB, jobId);

        BundleJobBean job;
        try {
            job = new BundleJobXCommand(jobId).call();
        }
        catch (CommandException ex) {
            throw new BundleEngineException(ex);
        }

        Services.get().get(XLogService.class).streamLog(filter, job.getCreatedTime(), new Date(), writer);
    }

    /* (non-Javadoc)
     * @see org.apache.oozie.BaseEngine#submitJob(org.apache.hadoop.conf.Configuration, boolean)
     */
    @Override
    public String submitJob(Configuration conf, boolean startJob) throws BundleEngineException {
        try {
            String jobId = new BundleSubmitXCommand(conf, getAuthToken()).call();

            if (startJob) {
                start(jobId);
            }
            return jobId;
        }
        catch (CommandException ex) {
            throw new BundleEngineException(ex);
        }
    }

    /* (non-Javadoc)
     * @see org.apache.oozie.BaseEngine#suspend(java.lang.String)
     */
    @Override
    public void suspend(String jobId) throws BundleEngineException {
        BundleJobSuspendXCommand suspend = new BundleJobSuspendXCommand(jobId);
        try {
            suspend.call();
        }
        catch (CommandException ex) {
            throw new BundleEngineException(ex);
        }
    }

    private static final Set<String> FILTER_NAMES = new HashSet<String>();

    static {
        FILTER_NAMES.add(OozieClient.FILTER_USER);
        FILTER_NAMES.add(OozieClient.FILTER_NAME);
        FILTER_NAMES.add(OozieClient.FILTER_GROUP);
        FILTER_NAMES.add(OozieClient.FILTER_STATUS);
    }

    /**
     * Get bundle jobs
     *
     * @param filterStr the filter string
     * @param start start location for paging
     * @param len total length to get
     * @return bundle job info
     * @throws BundleEngineException thrown if failed to get bundle job info
     */
    public BundleJobInfo getBundleJobs(String filterStr, int start, int len) throws BundleEngineException {
        Map<String, List<String>> filter = parseFilter(filterStr);

        try {
            return new BundleJobsXCommand(filter, start, len).call();
        }
        catch (CommandException ex) {
            throw new BundleEngineException(ex);
        }
    }

    /**
     * Parse filter string to a map with key = filter name and values = filter values
     *
     * @param filter the filter string
     * @return filter key and value map
     * @throws CoordinatorEngineException thrown if failed to parse filter string
     */
    private Map<String, List<String>> parseFilter(String filter) throws BundleEngineException {
        Map<String, List<String>> map = new HashMap<String, List<String>>();
        if (filter != null) {
            StringTokenizer st = new StringTokenizer(filter, ";");
            while (st.hasMoreTokens()) {
                String token = st.nextToken();
                if (token.contains("=")) {
                    String[] pair = token.split("=");
                    if (pair.length != 2) {
                        throw new BundleEngineException(ErrorCode.E0420, filter, "elements must be name=value pairs");
                    }
                    if (!FILTER_NAMES.contains(pair[0])) {
                        throw new BundleEngineException(ErrorCode.E0420, filter, XLog.format("invalid name [{0}]",
                                pair[0]));
                    }
                    if (pair[0].equals("status")) {
                        try {
                            Job.Status.valueOf(pair[1]);
                        }
                        catch (IllegalArgumentException ex) {
                            throw new BundleEngineException(ErrorCode.E0420, filter, XLog.format(
                                    "invalid status [{0}]", pair[1]));
                        }
                    }
                    List<String> list = map.get(pair[0]);
                    if (list == null) {
                        list = new ArrayList<String>();
                        map.put(pair[0], list);
                    }
                    list.add(pair[1]);
                }
                else {
                    throw new BundleEngineException(ErrorCode.E0420, filter, "elements must be name=value pairs");
                }
            }
        }
        return map;
    }

}
