/*
 * Copyright The Cryostat Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.cryostat.net.web.http.api.beta;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

import io.cryostat.configuration.CredentialsManager;
import io.cryostat.net.AuthManager;
import io.cryostat.net.security.ResourceAction;
import io.cryostat.net.web.http.HttpMimeType;
import io.cryostat.net.web.http.api.ApiVersion;
import io.cryostat.net.web.http.api.v2.AbstractV2RequestHandler;
import io.cryostat.net.web.http.api.v2.ApiException;
import io.cryostat.net.web.http.api.v2.IntermediateResponse;
import io.cryostat.net.web.http.api.v2.RequestParameters;
import io.cryostat.recordings.RecordingArchiveHelper;
import io.cryostat.recordings.RecordingNotFoundException;
import io.cryostat.rules.ArchivePathException;

import com.google.gson.Gson;
import io.vertx.core.http.HttpMethod;

public class RecordingDeleteFromPathHandler extends AbstractV2RequestHandler<Void> {

    static final String PATH = "fs/recordings/:subdirectoryName/:recordingName";

    private final RecordingArchiveHelper recordingArchiveHelper;

    @Inject
    RecordingDeleteFromPathHandler(
            AuthManager auth,
            CredentialsManager credentialsManager,
            Gson gson,
            RecordingArchiveHelper recordingArchiveHelper) {
        super(auth, credentialsManager, gson);
        this.recordingArchiveHelper = recordingArchiveHelper;
    }

    @Override
    public boolean requiresAuthentication() {
        return true;
    }

    @Override
    public ApiVersion apiVersion() {
        return ApiVersion.BETA;
    }

    @Override
    public HttpMethod httpMethod() {
        return HttpMethod.DELETE;
    }

    @Override
    public Set<ResourceAction> resourceActions() {
        return EnumSet.of(ResourceAction.DELETE_RECORDING);
    }

    @Override
    public String path() {
        return basePath() + PATH;
    }

    @Override
    public List<HttpMimeType> produces() {
        return List.of(HttpMimeType.JSON);
    }

    @Override
    public boolean isAsync() {
        return false;
    }

    @Override
    public IntermediateResponse<Void> handle(RequestParameters params) throws Exception {
        String subdirectoryName = params.getPathParams().get("subdirectoryName");
        String recordingName = params.getPathParams().get("recordingName");
        try {
            recordingArchiveHelper.deleteRecordingFromPath(subdirectoryName, recordingName).get();
            return new IntermediateResponse<Void>().body(null);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof RecordingNotFoundException
                    || e.getCause() instanceof ArchivePathException) {
                throw new ApiException(404, e.getMessage(), e);
            }
            throw e;
        }
    }
}
