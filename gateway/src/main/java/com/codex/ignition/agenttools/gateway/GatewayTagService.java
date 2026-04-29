package com.codex.ignition.agenttools.gateway;

import com.codex.ignition.agenttools.common.BrowseRequest;
import com.codex.ignition.agenttools.common.DeleteRequest;
import com.codex.ignition.agenttools.common.ErrorCode;
import com.codex.ignition.agenttools.common.ReadRequest;
import com.codex.ignition.agenttools.common.TagKind;
import com.codex.ignition.agenttools.common.TagSpec;
import com.codex.ignition.agenttools.common.TagWrite;
import com.codex.ignition.agenttools.common.UpsertRequest;
import com.codex.ignition.agenttools.common.ValueSourceType;
import com.codex.ignition.agenttools.common.WriteRequest;
import com.inductiveautomation.ignition.common.browsing.BrowseFilter;
import com.inductiveautomation.ignition.common.browsing.Results;
import com.inductiveautomation.ignition.common.model.values.BasicQualifiedValue;
import com.inductiveautomation.ignition.common.model.values.QualifiedValue;
import com.inductiveautomation.ignition.common.model.values.QualityCode;
import com.inductiveautomation.ignition.common.sqltags.model.types.DataType;
import com.inductiveautomation.ignition.common.tags.browsing.NodeDescription;
import com.inductiveautomation.ignition.common.tags.config.CollisionPolicy;
import com.inductiveautomation.ignition.common.tags.config.TagConfiguration;
import com.inductiveautomation.ignition.common.tags.config.TagConfigurationBuilder;
import com.inductiveautomation.ignition.common.tags.config.TagExecutionMode;
import com.inductiveautomation.ignition.common.tags.config.TagGroupConfiguration;
import com.inductiveautomation.ignition.common.tags.config.properties.WellKnownTagProps;
import com.inductiveautomation.ignition.common.tags.config.types.ExpressionTypeProperties;
import com.inductiveautomation.ignition.common.tags.config.types.OpcTagTypeProperties;
import com.inductiveautomation.ignition.common.tags.config.types.TagObjectType;
import com.inductiveautomation.ignition.common.tags.model.SecurityContext;
import com.inductiveautomation.ignition.common.tags.model.TagPath;
import com.inductiveautomation.ignition.common.tags.model.TagProvider;
import com.inductiveautomation.ignition.gateway.model.GatewayContext;
import com.inductiveautomation.ignition.gateway.tags.model.GatewayTagManager;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public final class GatewayTagService {
    private final GatewayTagManager tagManager;
    private final SecurityContext securityContext;

    public GatewayTagService(GatewayContext gatewayContext) {
        this.tagManager = gatewayContext.getTagManager();
        this.securityContext = SecurityContext.systemContext();
    }

    public Map<String, Object> health(AgentToolsConfig config) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("moduleVersion", com.codex.ignition.agenttools.common.AgentToolsModule.MODULE_VERSION);
        result.put("apiBasePath", com.codex.ignition.agenttools.common.AgentToolsModule.API_BASE_PATH);
        result.put("config", config.describe());
        result.put("providerCount", tagManager.getTagProviderNames().size());
        return result;
    }

    public Map<String, Object> browse(BrowseRequest request) {
        String providerName = requireProvider(request.provider);
        TagProvider provider = requireProviderExists(providerName);
        BrowseFilter filter = new BrowseFilter();
        filter.setRecursive(Boolean.TRUE.equals(request.recursive));
        if (request.maxResults != null && request.maxResults > 0) {
            filter.setMaxResults(request.maxResults);
        }
        if (request.continuationPoint != null && !request.continuationPoint.isBlank()) {
            filter.setContinuationPoint(request.continuationPoint);
        }

        TagPath browsePath = TagPathUtils.parse(providerName, request.path);
        Results<NodeDescription> results = await(provider.browseAsync(browsePath, filter, securityContext));
        Collection<NodeDescription> descriptions = results.getResults() == null ? List.of() : results.getResults();

        List<Map<String, Object>> nodes = new ArrayList<>();
        for (NodeDescription description : descriptions) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", description.getName());
            item.put("fullPath", description.getFullPath() == null ? null : description.getFullPath().toString());
            item.put("objectType", description.getObjectType() == null ? null : description.getObjectType().name());
            item.put("subTypeId", description.getSubTypeId());
            item.put("dataType", description.getDataType() == null ? null : description.getDataType().name());
            item.put("hasChildren", description.hasChildren());
            item.put(
                "attributes",
                description.getAttributes() == null
                    ? List.of()
                    : description.getAttributes().stream().map(String::valueOf).toList()
            );
            if (description.getCurrentValue() != null) {
                Map<String, Object> value = new LinkedHashMap<>();
                value.put("value", description.getCurrentValue().getValue());
                value.put("quality", description.getCurrentValue().getQuality() == null ? null : description.getCurrentValue().getQuality().toString());
                value.put("timestamp", description.getCurrentValue().getTimestamp());
                item.put("currentValue", value);
            }
            nodes.add(item);
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("provider", providerName);
        payload.put("path", TagPathUtils.normalizeRelativePath(request.path));
        payload.put("recursive", Boolean.TRUE.equals(request.recursive));
        payload.put("returnedSize", results.getReturnedSize());
        payload.put("totalAvailableSize", results.getTotalAvailableSize());
        payload.put("continuationPoint", results.getContinuationPoint());
        payload.put("resultQuality", results.getResultQuality() == null ? null : results.getResultQuality().toString());
        payload.put("nodes", nodes);
        return payload;
    }

    public Map<String, Object> read(ReadRequest request) {
        String providerName = requireProvider(request.provider);
        TagProvider provider = requireProviderExists(providerName);
        if (request.paths == null || request.paths.isEmpty()) {
            throw new TagOperationException(ErrorCode.INVALID_REQUEST, "paths is required");
        }

        List<TagPath> paths = request.paths.stream().map(path -> TagPathUtils.parse(providerName, path)).toList();
        List<Map<String, Object>> values = new ArrayList<>();
        List<QualifiedValue> results = await(provider.readAsync(paths, securityContext));

        for (int index = 0; index < paths.size(); index++) {
            QualifiedValue value = results.get(index);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("path", request.paths.get(index));
            item.put("qualifiedPath", paths.get(index).toString());
            item.put("value", value.getValue());
            item.put("quality", value.getQuality() == null ? null : value.getQuality().toString());
            item.put("timestamp", value.getTimestamp());
            values.add(item);
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("provider", providerName);
        payload.put("results", values);
        return payload;
    }

    public Map<String, Object> write(WriteRequest request) {
        String providerName = requireProvider(request.provider);
        TagProvider provider = requireProviderExists(providerName);
        if (request.writes == null || request.writes.isEmpty()) {
            throw new TagOperationException(ErrorCode.INVALID_REQUEST, "writes is required");
        }

        List<TagPath> paths = new ArrayList<>();
        List<QualifiedValue> values = new ArrayList<>();
        for (TagWrite write : request.writes) {
            if (write == null || write.path == null || write.path.isBlank()) {
                throw new TagOperationException(ErrorCode.INVALID_REQUEST, "write.path is required");
            }
            paths.add(TagPathUtils.parse(providerName, write.path));
            values.add(new BasicQualifiedValue(write.value, QualityCode.Good));
        }

        List<QualityCode> qualities = await(provider.writeAsync(paths, values, securityContext));
        List<Map<String, Object>> results = new ArrayList<>();
        for (int index = 0; index < qualities.size(); index++) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("path", request.writes.get(index).path);
            item.put("quality", qualities.get(index).toString());
            results.add(item);
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("provider", providerName);
        payload.put("results", results);
        return payload;
    }

    public Map<String, Object> upsert(UpsertRequest request, AgentToolsConfig config) {
        String providerName = requireProvider(request.provider);
        TagProvider provider = requireProviderExists(providerName);
        if (request.items == null || request.items.isEmpty()) {
            throw new TagOperationException(ErrorCode.INVALID_REQUEST, "items is required");
        }
        if (request.items.size() > config.getMaxBatchSize()) {
            throw new TagOperationException(
                ErrorCode.BATCH_LIMIT_EXCEEDED,
                "items exceeds maxBatchSize=" + config.getMaxBatchSize()
            );
        }

        validateTagGroups(provider, request.items);

        boolean dryRun = Boolean.TRUE.equals(request.dryRun);
        boolean continueOnError = Boolean.TRUE.equals(request.continueOnError);
        List<Map<String, Object>> results = new ArrayList<>();

        for (TagSpec spec : request.items) {
            TagConfiguration configuration = buildTagConfiguration(spec);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("path", spec.path);
            item.put("kind", spec.kind);
            item.put("dryRun", dryRun);

            if (dryRun) {
                item.put("validated", true);
                item.put("collisionPolicy", CollisionPolicy.Overwrite.name());
                results.add(item);
                continue;
            }

            QualityCode quality = await(provider.saveTagConfigsAsync(List.of(configuration), CollisionPolicy.Overwrite, securityContext)).get(0);
            item.put("quality", quality.toString());
            item.put("success", quality.isGood());
            results.add(item);

            if (!quality.isGood() && !continueOnError) {
                throw new TagOperationException(
                    ErrorCode.TAG_OPERATION_FAILED,
                    "Failed to save tag configuration for " + Objects.requireNonNullElse(spec.path, "(unknown)"),
                    spec.path
                );
            }
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("provider", providerName);
        payload.put("dryRun", dryRun);
        payload.put("continueOnError", continueOnError);
        payload.put("results", results);
        return payload;
    }

    public Map<String, Object> delete(DeleteRequest request, AgentToolsConfig config) {
        String providerName = requireProvider(request.provider);
        TagProvider provider = requireProviderExists(providerName);
        if (request.paths == null || request.paths.isEmpty()) {
            throw new TagOperationException(ErrorCode.INVALID_REQUEST, "paths is required");
        }
        if (request.paths.size() > config.getMaxDeleteCount()) {
            throw new TagOperationException(
                ErrorCode.DELETE_LIMIT_EXCEEDED,
                "paths exceeds maxDeleteCount=" + config.getMaxDeleteCount()
            );
        }

        boolean recursive = Boolean.TRUE.equals(request.recursive);
        List<Map<String, Object>> results = new ArrayList<>();
        for (String path : request.paths) {
            TagPath tagPath = TagPathUtils.parse(providerName, path);
            if (!recursive) {
                List<?> configs = await(provider.getTagConfigsAsync(List.of(tagPath), true, true));
                if (!configs.isEmpty()) {
                    Object first = configs.get(0);
                    if (first instanceof com.inductiveautomation.ignition.common.tags.config.TagConfigurationModel model) {
                        int childCount = model.getChildren() == null ? 0 : model.getChildren().size();
                        if (childCount > 0) {
                            throw new TagOperationException(
                                ErrorCode.INVALID_REQUEST,
                                "recursive=true is required to delete folders or complex tags with children",
                                path
                            );
                        }
                    }
                }
            }

            QualityCode quality = await(provider.removeTagConfigsAsync(List.of(tagPath), securityContext)).get(0);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("path", path);
            result.put("quality", quality.toString());
            result.put("success", quality.isGood());
            results.add(result);

            if (!quality.isGood()) {
                throw new TagOperationException(ErrorCode.TAG_OPERATION_FAILED, "Failed to delete tag", path);
            }
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("provider", providerName);
        payload.put("recursive", recursive);
        payload.put("results", results);
        return payload;
    }

    private void validateTagGroups(TagProvider provider, List<TagSpec> specs) {
        Set<String> requestedGroups = new LinkedHashSet<>();
        for (TagSpec spec : specs) {
            if (spec != null && spec.tagGroup != null && !spec.tagGroup.isBlank()) {
                requestedGroups.add(spec.tagGroup.trim());
            }
        }
        if (requestedGroups.isEmpty()) {
            return;
        }
        List<TagGroupConfiguration> groups = await(provider.getTagGroupsAsync());
        Set<String> existing = groups.stream().map(TagGroupConfiguration::getName).collect(LinkedHashSet::new, Set::add, Set::addAll);
        for (String requested : requestedGroups) {
            if (!existing.contains(requested)) {
                throw new TagOperationException(ErrorCode.TAG_GROUP_NOT_FOUND, "Unknown tag group: " + requested);
            }
        }
    }

    private String requireProvider(String provider) {
        if (provider == null || provider.isBlank()) {
            throw new TagOperationException(ErrorCode.INVALID_REQUEST, "provider is required");
        }
        return provider.trim();
    }

    private TagProvider requireProviderExists(String providerName) {
        TagProvider provider = tagManager.getTagProvider(providerName);
        if (provider == null) {
            throw new TagOperationException(ErrorCode.PROVIDER_NOT_FOUND, "Unknown provider: " + providerName);
        }
        return provider;
    }

    private TagConfiguration buildTagConfiguration(TagSpec spec) {
        if (spec == null) {
            throw new TagOperationException(ErrorCode.INVALID_REQUEST, "tag spec is required");
        }
        if (spec.path == null || spec.path.isBlank()) {
            throw new TagOperationException(ErrorCode.INVALID_REQUEST, "tag path is required");
        }

        String normalizedPath = TagPathUtils.normalizeRelativePath(spec.path);
        TagKind kind = TagKind.from(spec.kind);
        if (kind == null) {
            throw new TagOperationException(ErrorCode.INVALID_REQUEST, "Unsupported kind: " + spec.kind, spec.path);
        }

        TagConfigurationBuilder builder = TagConfigurationBuilder.newBuilder().fullPath(normalizedPath);
        applyCommonProperties(builder, spec);

        switch (kind) {
            case FOLDER -> builder.property(WellKnownTagProps.TagType, TagObjectType.Folder);
            case ATOMIC -> applyAtomic(builder, spec);
            case UDT_INSTANCE -> applyUdtInstance(builder, spec);
            default -> throw new TagOperationException(ErrorCode.INVALID_REQUEST, "Unsupported kind: " + kind, spec.path);
        }

        return builder.build();
    }

    private void applyCommonProperties(TagConfigurationBuilder builder, TagSpec spec) {
        if (spec.name != null && !spec.name.isBlank()) {
            builder.name(spec.name.trim());
        }
        if (spec.tagGroup != null && !spec.tagGroup.isBlank()) {
            builder.property(WellKnownTagProps.TagGroup, spec.tagGroup.trim());
        }
        applySupportedAttributes(builder, spec.path, spec.attributes);
        applySupportedAttributes(builder, spec.path, spec.overrides);
    }

    private void applyAtomic(TagConfigurationBuilder builder, TagSpec spec) {
        ValueSourceType valueSource = ValueSourceType.from(spec.valueSource);
        if (valueSource == null) {
            throw new TagOperationException(ErrorCode.INVALID_REQUEST, "atomic.valueSource is required", spec.path);
        }

        builder.property(WellKnownTagProps.TagType, TagObjectType.AtomicTag);
        builder.property(WellKnownTagProps.ValueSource, valueSource.wireValue());

        switch (valueSource) {
            case MEMORY -> {
                DataType dataType = parseDataType(spec.dataType, spec.path);
                builder.property(WellKnownTagProps.DataType, dataType);
                if (spec.value != null) {
                    builder.property(WellKnownTagProps.Value, new BasicQualifiedValue(coerceValue(dataType, spec.value), QualityCode.Good));
                }
            }
            case OPC -> {
                requireField(spec.opcServer, "atomic.opcServer is required", spec.path);
                requireField(spec.opcItemPath, "atomic.opcItemPath is required", spec.path);
                builder.property(OpcTagTypeProperties.OPCServer, spec.opcServer.trim());
                builder.property(OpcTagTypeProperties.OPCItemPath, spec.opcItemPath.trim());
            }
            case EXPRESSION -> {
                requireField(spec.expression, "atomic.expression is required", spec.path);
                builder.property(ExpressionTypeProperties.Expression, spec.expression);
                if (spec.dataType != null && !spec.dataType.isBlank()) {
                    builder.property(WellKnownTagProps.DataType, parseDataType(spec.dataType, spec.path));
                }
            }
            default -> throw new TagOperationException(ErrorCode.INVALID_REQUEST, "Unsupported valueSource: " + spec.valueSource, spec.path);
        }
    }

    private void applyUdtInstance(TagConfigurationBuilder builder, TagSpec spec) {
        requireField(spec.typePath, "udtInstance.typePath is required", spec.path);
        builder.property(WellKnownTagProps.TagType, TagObjectType.UdtInstance);
        builder.property(WellKnownTagProps.TypeId, spec.typePath.trim());
        if (spec.parameters != null) {
            for (Map.Entry<String, Object> entry : spec.parameters.entrySet()) {
                builder.parameter(entry.getKey(), entry.getValue() == null ? "" : String.valueOf(entry.getValue()));
            }
        }
    }

    private void applySupportedAttributes(TagConfigurationBuilder builder, String path, Map<String, Object> attributes) {
        if (attributes == null || attributes.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Object> entry : attributes.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            switch (key) {
                case "enabled" -> builder.property(WellKnownTagProps.Enabled, asBoolean(value, path, key));
                case "documentation" -> builder.property(WellKnownTagProps.Documentation, asString(value));
                case "readOnly" -> builder.property(WellKnownTagProps.ReadOnly, asBoolean(value, path, key));
                case "tooltip" -> builder.property(WellKnownTagProps.Tooltip, asString(value));
                case "historyEnabled" -> builder.property(WellKnownTagProps.HistoryEnabled, asBoolean(value, path, key));
                case "historyProvider" -> builder.property(WellKnownTagProps.HistoryProvider, asString(value));
                case "formatString" -> builder.property(WellKnownTagProps.FormatString, asString(value));
                case "engUnit" -> builder.property(WellKnownTagProps.EngUnit, asString(value));
                case "persistValue" -> builder.property(WellKnownTagProps.PersistValue, asBoolean(value, path, key));
                case "executionMode" -> builder.property(WellKnownTagProps.ExecutionMode, parseExecutionMode(value, path));
                case "executionRate" -> builder.property(WellKnownTagProps.ExecutionRate, asLong(value, path, key));
                default -> throw new TagOperationException(
                    ErrorCode.UNSUPPORTED_ATTRIBUTE,
                    "Unsupported attribute: " + key,
                    path
                );
            }
        }
    }

    private DataType parseDataType(String raw, String path) {
        requireField(raw, "atomic.dataType is required", path);
        try {
            return DataType.valueOf(raw.trim());
        } catch (IllegalArgumentException ex) {
            throw new TagOperationException(ErrorCode.INVALID_REQUEST, "Unsupported dataType: " + raw, path);
        }
    }

    private TagExecutionMode parseExecutionMode(Object value, String path) {
        if (value == null) {
            throw new TagOperationException(ErrorCode.INVALID_REQUEST, "executionMode cannot be null", path);
        }
        try {
            return TagExecutionMode.valueOf(String.valueOf(value).trim());
        } catch (IllegalArgumentException ex) {
            throw new TagOperationException(ErrorCode.INVALID_REQUEST, "Unsupported executionMode: " + value, path);
        }
    }

    private boolean asBoolean(Object value, String path, String key) {
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        if (value instanceof String stringValue) {
            String normalized = stringValue.trim();
            if ("true".equalsIgnoreCase(normalized)) {
                return true;
            }
            if ("false".equalsIgnoreCase(normalized)) {
                return false;
            }
        }
        throw new TagOperationException(ErrorCode.INVALID_REQUEST, key + " must be boolean", path);
    }

    private long asLong(Object value, String path, String key) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String stringValue) {
            try {
                return Long.parseLong(stringValue.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        throw new TagOperationException(ErrorCode.INVALID_REQUEST, key + " must be an integer", path);
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Object coerceValue(DataType dataType, Object raw) {
        if (raw == null) {
            return null;
        }
        return switch (dataType) {
            case Boolean -> toBoolean(raw);
            case Int1 -> ((Number) toNumber(raw)).byteValue();
            case Int2 -> ((Number) toNumber(raw)).shortValue();
            case Int4 -> ((Number) toNumber(raw)).intValue();
            case Int8 -> ((Number) toNumber(raw)).longValue();
            case Float4 -> ((Number) toNumber(raw)).floatValue();
            case Float8 -> ((Number) toNumber(raw)).doubleValue();
            case DateTime -> toDate(raw);
            case String, Text -> String.valueOf(raw);
            default -> raw;
        };
    }

    private Number toNumber(Object raw) {
        if (raw instanceof Number number) {
            return number;
        }
        if (raw instanceof String stringValue) {
            try {
                String normalized = stringValue.trim();
                if (normalized.contains(".") || normalized.contains("e") || normalized.contains("E")) {
                    return Double.parseDouble(normalized);
                }
                return Long.parseLong(normalized);
            } catch (NumberFormatException ignored) {
            }
        }
        throw new TagOperationException(ErrorCode.INVALID_REQUEST, "Value is not numeric: " + raw);
    }

    private Boolean toBoolean(Object raw) {
        if (raw instanceof Boolean booleanValue) {
            return booleanValue;
        }
        if (raw instanceof String stringValue) {
            String normalized = stringValue.trim();
            if ("true".equalsIgnoreCase(normalized)) {
                return true;
            }
            if ("false".equalsIgnoreCase(normalized)) {
                return false;
            }
        }
        throw new TagOperationException(ErrorCode.INVALID_REQUEST, "Value is not boolean: " + raw);
    }

    private Date toDate(Object raw) {
        if (raw instanceof Date date) {
            return date;
        }
        if (raw instanceof String stringValue) {
            try {
                return Date.from(Instant.parse(stringValue.trim()));
            } catch (DateTimeParseException ex) {
                throw new TagOperationException(ErrorCode.INVALID_REQUEST, "Value is not an ISO-8601 datetime: " + raw);
            }
        }
        throw new TagOperationException(ErrorCode.INVALID_REQUEST, "Value is not an ISO-8601 datetime: " + raw);
    }

    private void requireField(String value, String message, String path) {
        if (value == null || value.isBlank()) {
            throw new TagOperationException(ErrorCode.INVALID_REQUEST, message, path);
        }
    }

    private <T> T await(CompletableFuture<T> future) {
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TagOperationException(ErrorCode.TAG_OPERATION_FAILED, "Interrupted while waiting for tag operation");
        } catch (ExecutionException e) {
            throw new TagOperationException(
                ErrorCode.TAG_OPERATION_FAILED,
                e.getCause() == null ? e.getMessage() : e.getCause().getMessage()
            );
        }
    }
}
