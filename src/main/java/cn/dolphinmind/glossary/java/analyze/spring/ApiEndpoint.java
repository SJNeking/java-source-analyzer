package cn.dolphinmind.glossary.java.analyze.spring;

import java.util.*;

/**
 * Spring API endpoint data model.
 * Represents a REST/Web endpoint discovered from Spring annotations.
 */
public class ApiEndpoint {
    private String className;
    private String methodName;
    private String address; // fully qualified method address
    private String httpMethod; // GET, POST, PUT, DELETE, PATCH, ANY
    private String path; // URL path pattern
    private String produces; // response media type
    private String consumes; // request media type
    private String description; // method javadoc summary
    private List<String> parameters = new ArrayList<>();
    private String returnType;
    private int line;
    private String module;

    public ApiEndpoint() {}

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
    public String getMethodName() { return methodName; }
    public void setMethodName(String methodName) { this.methodName = methodName; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getHttpMethod() { return httpMethod; }
    public void setHttpMethod(String httpMethod) { this.httpMethod = httpMethod; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public String getProduces() { return produces; }
    public void setProduces(String produces) { this.produces = produces; }
    public String getConsumes() { return consumes; }
    public void setConsumes(String consumes) { this.consumes = consumes; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public List<String> getParameters() { return parameters; }
    public void setParameters(List<String> parameters) { this.parameters = parameters; }
    public String getReturnType() { return returnType; }
    public void setReturnType(String returnType) { this.returnType = returnType; }
    public int getLine() { return line; }
    public void setLine(int line) { this.line = line; }
    public String getModule() { return module; }
    public void setModule(String module) { this.module = module; }

    /**
     * Convert to Map for JSON serialization.
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("class", className);
        map.put("method", methodName);
        map.put("address", address);
        map.put("httpMethod", httpMethod);
        map.put("path", path);
        map.put("produces", produces);
        map.put("consumes", consumes);
        map.put("description", description);
        map.put("parameters", parameters);
        map.put("returnType", returnType);
        map.put("line", line);
        map.put("module", module);
        return map;
    }
}
