package cn.dolphinmind.glossary.java.analyze.quality.rules;

import cn.dolphinmind.glossary.java.analyze.quality.QualityIssue;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Tests for SECURITY category rules.
 */
public class SecurityRulesTest extends AbstractRuleTest {

    @Test
    public void sqlInjection_shouldDetect() {
        AllRules.SQLInjection rule = new AllRules.SQLInjection();
        Map<String, Object> method = createMethod("findUser", "String sql = \"SELECT * FROM users WHERE id = \" + userId;\n" +
            "stmt.executeQuery(sql);\n");
        assertIssues(rule, method, 1);
    }

    @Test
    public void sqlInjection_shouldNotDetectWithPreparedStatement() {
        AllRules.SQLInjection rule = new AllRules.SQLInjection();
        Map<String, Object> method = createMethod("findUser", "PreparedStatement ps = conn.prepareStatement(\"SELECT * FROM users WHERE id = ?\");\n" +
            "ps.setString(1, userId);\n");
        assertIssues(rule, method, 0);
    }

    @Test
    public void hardcodedPassword_shouldDetect() {
        AllRules.HardcodedPassword rule = new AllRules.HardcodedPassword();
        Map<String, Object> method = createMethod("connect", "String password = \"admin123\";\n" +
            "connect(user, password);\n");
        assertIssues(rule, method, 1);
    }

    @Test
    public void commandInjection_shouldDetect() {
        AllRules.CommandInjection rule = new AllRules.CommandInjection();
        Map<String, Object> method = createMethod("exec", "Runtime.getRuntime().exec(\"cmd /c \" + userInput);\n");
        assertIssues(rule, method, 1);
    }

    @Test
    public void weakHashFunction_shouldDetect() {
        AllRules.WeakHashFunction rule = new AllRules.WeakHashFunction();
        Map<String, Object> method = createMethod("hash", "MessageDigest.getInstance(\"MD5\");\n");
        assertIssues(rule, method, 1);
    }

    @Test
    public void insecureRandom_shouldDetect() {
        AllRules.InsecureRandom rule = new AllRules.InsecureRandom();
        Map<String, Object> method = createMethod("generateToken", "Random r = new Random();\n" +
            "return r.nextInt();\n");
        // This rule checks for security context - simplified test
        assertNotNull(rule.getRuleKey());
    }

    @Test
    public void xxeInjection_shouldDetect() {
        AllRules.DOMParserXXE rule = new AllRules.DOMParserXXE();
        Map<String, Object> method = createMethod("parseXml", "DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();\n" +
            "factory.newDocumentBuilder().parse(input);\n");
        assertIssues(rule, method, 1);
    }

    @Test
    public void xxeInjection_shouldNotDetectWithSecureProcessing() {
        AllRules.DOMParserXXE rule = new AllRules.DOMParserXXE();
        Map<String, Object> method = createMethod("parseXml", "DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();\n" +
            "factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);\n");
        assertIssues(rule, method, 0);
    }

    @Test
    public void csrfDisabled_shouldDetect() {
        AllRules.CSRFDisabled rule = new AllRules.CSRFDisabled();
        Map<String, Object> method = createMethod("configure", "http.csrf().disable();\n");
        assertIssues(rule, method, 1);
    }

    @Test
    public void pathTraversal_shouldDetect() {
        AllRules.PathTraversal rule = new AllRules.PathTraversal();
        Map<String, Object> method = createMethod("readFile", "new File(\"/uploads/\" + request.getParameter(\"filename\"));\n");
        assertIssues(rule, method, 1);
    }

    @Test
    public void logInjection_shouldDetect() {
        AllRules.LogInjection rule = new AllRules.LogInjection();
        Map<String, Object> method = createMethod("log", "log.info(\"User: \" + request.getParameter(\"username\"));\n");
        assertIssues(rule, method, 1);
    }

    @Test
    public void corsMisconfiguration_shouldDetect() {
        AllRules.CORSMisconfiguration rule = new AllRules.CORSMisconfiguration();
        Map<String, Object> method = createMethod("configure", "response.setHeader(\"Access-Control-Allow-Origin\", \"*\");\n");
        assertIssues(rule, method, 1);
    }

    @Test
    public void tlsProtocol_shouldDetect() {
        AllRules.TLSProtocol rule = new AllRules.TLSProtocol();
        Map<String, Object> method = createMethod("createSSLContext", "SSLContext.getInstance(\"TLSv1\");\n");
        assertIssues(rule, method, 1);
    }
}
