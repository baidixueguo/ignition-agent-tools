package com.codex.ignition.agenttools.gateway;

import com.codex.ignition.agenttools.common.ApiError;
import com.codex.ignition.agenttools.common.ApiResponse;
import com.codex.ignition.agenttools.common.BrowseRequest;
import com.codex.ignition.agenttools.common.DeleteRequest;
import com.codex.ignition.agenttools.common.ErrorCode;
import com.codex.ignition.agenttools.common.ReadRequest;
import com.codex.ignition.agenttools.common.UpsertRequest;
import com.codex.ignition.agenttools.common.WriteRequest;
import com.inductiveautomation.ignition.common.gson.JsonParseException;
import com.inductiveautomation.ignition.gateway.dataroutes.AccessControlStrategy;
import com.inductiveautomation.ignition.gateway.dataroutes.HttpMethod;
import com.inductiveautomation.ignition.gateway.dataroutes.RequestContext;
import com.inductiveautomation.ignition.gateway.dataroutes.RouteGroup;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

public final class AgentToolsRoutes {
    private static final int SC_UNPROCESSABLE_ENTITY = 422;

    private final AgentToolsConfig config;
    private final GatewayTagService tagService;
    private final AuditLogger auditLogger;

    public AgentToolsRoutes(AgentToolsConfig config, GatewayTagService tagService, AuditLogger auditLogger) {
        this.config = config;
        this.tagService = tagService;
        this.auditLogger = auditLogger;
    }

    public void mount(RouteGroup routes) {
        routes.newRoute("/v1/health")
            .method(HttpMethod.GET)
            .type(RouteGroup.TYPE_JSON)
            .renderer(JsonSupport.GSON::toJson)
            .accessControl(AccessControlStrategy.OPEN_ROUTE)
            .handler(this::health)
            .mount();

        post(routes, "/v1/tags/browse", BrowseRequest.class, "browse", body -> tagService.browse(body));
        post(routes, "/v1/tags/read", ReadRequest.class, "read", body -> tagService.read(body));
        post(routes, "/v1/tags/write", WriteRequest.class, "write", body -> tagService.write(body));
        post(routes, "/v1/tags/upsert", UpsertRequest.class, "upsert", body -> tagService.upsert(body, config));
        post(routes, "/v1/tags/delete", DeleteRequest.class, "delete", body -> tagService.delete(body, config));
    }

    private <T> void post(
        RouteGroup routes,
        String path,
        Class<T> requestType,
        String operation,
        Function<T, Map<String, Object>> handler
    ) {
        routes.newRoute(path)
            .method(HttpMethod.POST)
            .type(RouteGroup.TYPE_JSON)
            .acceptedTypes(RouteGroup.TYPE_JSON)
            .renderer(JsonSupport.GSON::toJson)
            .accessControl(AccessControlStrategy.OPEN_ROUTE)
            .handler((req, res) -> execute(req, res, requestType, operation, handler))
            .mount();
    }

    private ApiResponse<Map<String, Object>> health(RequestContext req, HttpServletResponse res) {
        String requestId = requestId(req);
        AuthorizationFailure failure = authorize(req, res, requestId);
        if (failure != null) {
            List<ApiError> errors = List.of(failure.error());
            auditLogger.log(requestId, "health", null, 0, false, false, remoteAddress(req), errors);
            return ApiResponse.failure(requestId, errors);
        }
        Map<String, Object> payload = tagService.health(config);
        auditLogger.log(requestId, "health", null, 0, false, true, remoteAddress(req), List.of());
        return ApiResponse.success(requestId, payload);
    }

    private <T> ApiResponse<Map<String, Object>> execute(
        RequestContext req,
        HttpServletResponse res,
        Class<T> requestType,
        String operation,
        Function<T, Map<String, Object>> handler
    ) {
        String requestId = requestId(req);
        AuthorizationFailure failure = authorize(req, res, requestId);
        if (failure != null) {
            List<ApiError> errors = List.of(failure.error());
            auditLogger.log(requestId, operation, null, 0, false, false, remoteAddress(req), errors);
            return ApiResponse.failure(requestId, errors);
        }

        try {
            T body = parseBody(req, requestType);
            Map<String, Object> payload = handler.apply(body);
            String provider = payload.containsKey("provider") ? String.valueOf(payload.get("provider")) : null;
            boolean dryRun = Boolean.TRUE.equals(payload.get("dryRun"));
            int itemCount = countItems(payload);
            auditLogger.log(requestId, operation, provider, itemCount, dryRun, true, remoteAddress(req), List.of());
            return ApiResponse.success(requestId, payload);
        } catch (TagOperationException ex) {
            res.setStatus(SC_UNPROCESSABLE_ENTITY);
            List<ApiError> errors = List.of(ApiError.of(ex.getCode(), ex.getMessage(), ex.getPath()));
            auditLogger.log(requestId, operation, null, 0, false, false, remoteAddress(req), errors);
            return ApiResponse.failure(requestId, errors);
        } catch (JsonParseException | IOException ex) {
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            List<ApiError> errors = List.of(ApiError.of(ErrorCode.INVALID_REQUEST, ex.getMessage()));
            auditLogger.log(requestId, operation, null, 0, false, false, remoteAddress(req), errors);
            return ApiResponse.failure(requestId, errors);
        } catch (RuntimeException ex) {
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            List<ApiError> errors = List.of(ApiError.of(ErrorCode.TAG_OPERATION_FAILED, ex.getMessage()));
            auditLogger.log(requestId, operation, null, 0, false, false, remoteAddress(req), errors);
            return ApiResponse.failure(requestId, errors);
        }
    }

    private <T> T parseBody(RequestContext req, Class<T> requestType) throws IOException {
        String body = req.readBody();
        if (body == null || body.isBlank()) {
            throw new TagOperationException(ErrorCode.INVALID_REQUEST, "Request body is required");
        }
        T parsed = JsonSupport.GSON.fromJson(body, requestType);
        if (parsed == null) {
            throw new TagOperationException(ErrorCode.INVALID_REQUEST, "Request body is invalid");
        }
        return parsed;
    }

    private AuthorizationFailure authorize(RequestContext req, HttpServletResponse res, String requestId) {
        res.setHeader("X-Request-Id", requestId);
        if (!config.isConfigured()) {
            res.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return new AuthorizationFailure(
                ApiError.of(ErrorCode.CONFIGURATION_ERROR, "Module API key is not configured")
            );
        }
        if (!config.isEnabled()) {
            res.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return new AuthorizationFailure(ApiError.of(ErrorCode.DISABLED, "Module is disabled"));
        }
        HttpServletRequest servletRequest = req.getRequest();
        String authorization = servletRequest.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            res.setHeader("WWW-Authenticate", "Bearer");
            return new AuthorizationFailure(ApiError.of(ErrorCode.UNAUTHORIZED, "Missing bearer token"));
        }
        String token = authorization.substring("Bearer ".length()).trim();
        if (!config.matchesApiKey(token)) {
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            res.setHeader("WWW-Authenticate", "Bearer");
            return new AuthorizationFailure(ApiError.of(ErrorCode.UNAUTHORIZED, "Invalid API key"));
        }
        return null;
    }

    private String requestId(RequestContext req) {
        String headerValue = req.getRequest().getHeader("X-Request-Id");
        if (headerValue != null && !headerValue.isBlank()) {
            return headerValue;
        }
        return UUID.randomUUID().toString();
    }

    private String remoteAddress(RequestContext req) {
        return req.getRequest().getRemoteAddr();
    }

    private int countItems(Map<String, Object> payload) {
        Object results = payload.get("results");
        if (results instanceof List<?> list) {
            return list.size();
        }
        Object nodes = payload.get("nodes");
        if (nodes instanceof List<?> list) {
            return list.size();
        }
        return 0;
    }

    private record AuthorizationFailure(ApiError error) {
    }
}
