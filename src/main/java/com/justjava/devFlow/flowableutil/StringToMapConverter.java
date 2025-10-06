package com.justjava.devFlow.flowableutil;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.flowable.engine.impl.el.FixedValue;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class StringToMapConverter implements JavaDelegate {
    private final ObjectMapper objectMapper;
    private FixedValue variableToConvertToMap;

    public StringToMapConverter(ObjectMapper objectMapper) {

        this.objectMapper = objectMapper;
    }

    @Override
    public void execute(DelegateExecution execution) {

        //System.out.println(" The Execution Variables===="+execution.getVariables());

        System.out.println(" The variableToConvertToMap.getExpressionText()===="+
                variableToConvertToMap.getExpressionText());

        //Map<String, Object> payload = new HashMap<>();
        String variableName= (String) execution.getVariable(variableToConvertToMap.getExpressionText());

//        variableName = variableName.replace("```json","").replace("```","");
//        variableName = variableName.replaceAll("(?m)^\\s*#.*$", "")
//                .replaceAll("(?m)^\\s*//.*$", "");
//        variableName = variableName.replaceAll("/\\*.*?\\*/", "");
//        variableName = variableName.replaceAll("(?<![a-zA-Z])'(?![a-zA-Z])", "\"")
//                .replaceAll("(?<=[a-zA-Z])'(?=[a-zA-Z])", "\\\\'");
//        variableName = variableName.replaceAll("\\*\\*", "")  // Remove bold **text**
//                .replaceAll("\\*", "")     // Remove italic *text*
//                .replaceAll("_", "")       // Remove underscore _text_
//                .replaceAll("`", "");      // Remove code `text`
        //System.out.println(" The String to be converted here======"+variableName);

        try {
           objectMapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
            objectMapper.configure(JsonParser.Feature.ALLOW_YAML_COMMENTS, true);
            objectMapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
            objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
            objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);
            objectMapper.configure(JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER, true);
            objectMapper.configure(JsonParser.Feature.INCLUDE_SOURCE_IN_LOCATION, true);

            // Remove multi-line comments (/* */)
            //variableName = cleanJsonString(variableName);

            Map<String,Object> map = objectMapper.readValue(variableName,Map.class);

            execution.setVariable(variableToConvertToMap.getExpressionText(), map);
            //System.out.println(" The JSON going to thymeleaf generation==="+json);
        } catch (JsonProcessingException e) {
            System.out.println(" The variable name crashing ======================"+variableName+" and " +
                    "variableToConvertToMap.getExpressionText()=="+variableToConvertToMap.getExpressionText()+"\n\n\n\n\n\n" +
                    "\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n");

            throw new RuntimeException(e);
        }

    }
    private static String cleanJsonString(String json) {
        if (json == null || json.trim().isEmpty()) {
            return json;
        }

        // Step 1: Remove markdown formatting
        String cleaned = json
                .replaceAll("\\*\\*", "")  // Remove bold **
                .replaceAll("\\*", "")     // Remove italic *
                .replaceAll("_", "")       // Remove underscore
                .replaceAll("`", "");      // Remove code ticks

        // Step 2: Fix common JSON issues in string values
        cleaned = fixStringValues(cleaned);

        // Step 3: Remove problematic characters outside strings
        cleaned = removeProblematicChars(cleaned);

        return cleaned;
    }

    private static String fixStringValues(String json) {
        StringBuilder result = new StringBuilder();
        boolean inString = false;
        char prevChar = 0;

        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);

            // Toggle string state when encountering unescaped quotes
            if (c == '"' && prevChar != '\\') {
                inString = !inString;
                result.append(c);
            }
            // If we're inside a string, handle special characters
            else if (inString) {
                // Escape problematic characters within strings
                if (c == '\n') {
                    result.append("\\n");
                } else if (c == '\r') {
                    result.append("\\r");
                } else if (c == '\t') {
                    result.append("\\t");
                } else if (c == '\\' && i + 1 < json.length()) {
                    // Preserve existing escape sequences
                    result.append(c);
                    i++;
                    result.append(json.charAt(i));
                } else {
                    result.append(c);
                }
            }
            // Outside strings, only allow valid JSON structure characters
            else {
                if (c == '{' || c == '}' || c == '[' || c == ']' ||
                        c == ':' || c == ',' ||
                        Character.isWhitespace(c) ||
                        Character.isLetterOrDigit(c) ||
                        c == '.' || c == '-' || c == '+' ||
                        c == '"' || c == '\'') {
                    result.append(c);
                }
                // Skip other characters (like problematic parentheses)
            }

            prevChar = c;
        }

        return result.toString();
    }

    private static String removeProblematicChars(String json) {
        // Remove parentheses and other problematic characters that break JSON structure
        return json
                .replaceAll("(?<!\")[\\(\\)](?!\")", "")  // Remove parentheses not within quotes
                .replaceAll(";", "")                       // Remove semicolons
                .replaceAll("—", "-")                      // Replace em dash with regular dash
                .replaceAll("–", "-")                      // Replace en dash with regular dash
                .replaceAll("“", "\"")                     // Replace smart quotes
                .replaceAll("”", "\"")                     // Replace smart quotes
                .replaceAll("‘", "'")                      // Replace smart single quotes
                .replaceAll("’", "'");                     // Replace smart single quotes
    }
}
