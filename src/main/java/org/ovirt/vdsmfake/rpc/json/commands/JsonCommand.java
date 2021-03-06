package org.ovirt.vdsmfake.rpc.json.commands;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.inject.spi.CDI;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.ovirt.vdsm.jsonrpc.client.JsonRpcResponse;
import org.ovirt.vdsm.jsonrpc.client.ResponseBuilder;
import org.ovirt.vdsmfake.AppLifecycleListener;
import org.ovirt.vdsmfake.rpc.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({ "rawtypes", "unchecked" })
public abstract class JsonCommand {
    protected static final Logger log = LoggerFactory
            .getLogger(JsonCommand.class);
    private final ObjectMapper mapper = new ObjectMapper();
    protected Api api = CDI.current().select(Api.class, AppLifecycleListener.DefaultLiteral.INSTANCE).get();

    public JsonRpcResponse run(JsonNode params, JsonNode requestId) {
        Object result;
        ResponseBuilder builder = new ResponseBuilder(requestId);
        try {
            Map apiResult = activateApi(params);

            if (fieldName() != null) {
                result = apiResult.get(fieldName());
            } else {
                result = apiResult;
            }

        } catch (Exception e) {
            log.error("Can't run api call", e);
            Map<String, Object> error = new HashMap<>();
            // General exception
            error.put("code", 100);
            error.put("message", e.getMessage());
            return builder.withError(error).build();
        }

        if (result instanceof Map) {
            builder =
                    builder.withResult((Map) result);
        } else if (result instanceof List) {
            builder =
                    builder.withResult((List) result);
        } else if (result instanceof String) {
            builder = builder.withResult((String) result);
        } else if (result != null && result.getClass().isArray()){
            List tempList = new ArrayList();
            Collections.addAll(tempList, (Object[]) result);
            builder = builder.withResult(tempList);
        } else {
            log.error("Unknown response data --> " + result, new Exception());
            Map<String, Object> error = new HashMap<>();
            // General exception
            error.put("code", 100);
            error.put("message", "Unknown response data");
            builder.withError(error);
        }

        return builder.build();
    }

    abstract String fieldName();

    abstract Map activateApi(JsonNode params) throws JsonParseException, JsonMappingException, IOException;

    protected List toList(JsonNode jsonNode) throws JsonParseException, JsonMappingException,
            IOException {
        return mapper.readValue(jsonNode, new TypeReference<List>() {
        });
    }

    protected Map toMap(JsonNode jsonNode) throws JsonParseException,
            JsonMappingException, IOException {
        return mapper.readValue(jsonNode, new TypeReference<Map>() {
        });
    }

}
