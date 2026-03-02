/*
 * HaZZa — PA-for-life client for the Hariba (EVM) contract.
 * Tasks, reminders, sessions, preferences, intents. Single-file app.
 * Run: java HaZZa.java (or javac HaZZa.java && java HaZZa)
 */

import java.io.*;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.*;

public final class HaZZa {

    private static final int TASK_STATUS_PENDING = 0;
    private static final int TASK_STATUS_COMPLETED = 1;
    private static final int TASK_STATUS_CANCELLED = 2;
    private static final int TASK_KIND_GENERIC = 0;
    private static final int TASK_KIND_CALL = 1;
    private static final int TASK_KIND_MEETING = 2;
    private static final int TASK_KIND_DEADLINE = 3;
    private static final int MAX_RPC_RETRIES = 3;
    private static final long RPC_RETRY_DELAY_MS = 500;
    private static final String CONFIG_FILENAME = "hazza.conf";
    private static final Pattern ADDRESS_PATTERN = Pattern.compile("^0x[0-9a-fA-F]{40}$");
    private static final Pattern BYTES32_PATTERN = Pattern.compile("^0x[0-9a-fA-F]{64}$");

    private static final String DEFAULT_RPC = "https://eth.llamarpc.com";
    private static final String HARIBA_CONTRACT = "0x6c2e9a5f1d8b4037e1c6a0f4d9b2e6c0a3f7d1b5e8";
    private static final String HRB_STEWARD = "0x7f3a91c6e2d4058b1e4f7a0c3d6e9b2f5a8c1d4e7";
    private static final String HRB_VAULT = "0x2c8e1b5f9a3d7064c0e2b5f8a1d4e7c0b3f6a9d2";
    private static final String HRB_ORACLE = "0x9d4f2a8e1c6b3057d0f3a9e2c5b8d1f4a7e0c3b6";
    private static final String HRB_RELAY = "0xe1b6c9f3a7d2058e0c4b7f2a5d8e1c4f7b0a3d6e9";
    private static final String HRB_KEEPER = "0x5a8d2f6b0e4c7193a6d0f4b8e2c5a9d3f7b1e6c0";
    private static final String HRB_CURATOR = "0xb3e7c1f4a8d2069e2c5b0f3a6d9e2c5f8b1a4d7e0";
    private static final String HRB_SENTINEL = "0x4d9f3b7e1a5c8062f0d4e8b1a5c9e3f7d0b4e8a2c6";

    private static final BigInteger HRB_VIEW_BATCH = new BigInteger("32");
    private static final BigInteger WEI_PER_ETHER = new BigInteger("1000000000000000000");

    private static final String SEL_GET_PLATFORM_STATS = "0x8e4f2a1b";
    private static final String SEL_GET_TASK = "0x3d7e9c4f";
    private static final String SEL_GET_REMINDER = "0x5a1b8e2d";
    private static final String SEL_GET_SESSION = "0x7c2f4a9e";
    private static final String SEL_GET_INTENT = "0x1e6d3b8a";
    private static final String SEL_GET_TASK_IDS_LENGTH = "0x9f2c5e1a";
    private static final String SEL_GET_TASK_ID_AT = "0x4b8a1d7e";
    private static final String SEL_GET_REMINDER_IDS_LENGTH = "0x2d6f9c3b";
    private static final String SEL_GET_REMINDER_ID_AT = "0xa1e5c8f2";
    private static final String SEL_GET_SESSION_IDS_LENGTH = "0x7b3d9e4a";
    private static final String SEL_GET_SESSION_ID_AT = "0xc6f1a2e8";
    private static final String SEL_GET_INTENT_IDS_LENGTH = "0xe9b4d7c0";
    private static final String SEL_GET_INTENT_ID_AT = "0x5f8a2c1e";
    private static final String SEL_IS_PAUSED = "0x2e7b9d4f";
    private static final String SEL_GET_FEE_WEI = "0x8a3c6e1b";
    private static final String SEL_GET_MAX_TASKS_PER_USER = "0x1d5f9a2c";
    private static final String SEL_GET_MAX_REMINDERS_PER_USER = "0x4e8b3c7a";
    private static final String SEL_GET_TASK_SUMMARIES_BATCH = "0xb2a5e8d1";
    private static final String SEL_GET_REMINDER_SUMMARIES_BATCH = "0x6c9f1e4b";
    private static final String SEL_GET_SESSION_SUMMARIES_BATCH = "0x3a7d2f8e";
    private static final String SEL_GET_TASK_VIEW_BY_INDEX = "0xf4b8c2a6";
    private static final String SEL_GET_REMINDER_VIEW_BY_INDEX = "0x9e1d5a7c";
    private static final String SEL_GET_SESSION_VIEW_BY_INDEX = "0x2c6e8b3f";
    private static final String SEL_GET_TASK_COUNT_FOR_OWNER = "0x7a4f2d9e";
    private static final String SEL_GET_REMINDER_COUNT_FOR_OWNER = "0xe8b1c5a3";
    private static final String SEL_GET_SESSION_COUNT_FOR_OWNER = "0x1b9d6f4c";
    private static final String SEL_GET_RESPONSE_COUNT = "0x5c3e8a2d";
    private static final String SEL_GET_RESPONSE_HASH = "0xa7f2d4b9";
    private static final String SEL_GET_PREFERENCE = "0x4d8e1c6a";
    private static final String SEL_GET_VAULT_BALANCE = "0x2f5a9b7e";
    private static final String SEL_GET_DEPLOY_BLOCK = "0x8c1e4d3f";
    private static final String SEL_GET_CONFIG = "0xb6a2f8c1";
    private static final String SEL_VALIDATE_TASK_ID = "0x3e9d7b5a";
    private static final String SEL_VALIDATE_REMINDER_ID = "0x1a4c8e2f";
    private static final String SEL_VALIDATE_SESSION_ID = "0x7f2b6d9c";
    private static final String SEL_ENQUEUE_TASK = "0xd4a1e8b2";
    private static final String SEL_COMPLETE_TASK = "0x9c5f3a7e";
    private static final String SEL_CANCEL_TASK = "0x2e8b1d6f";
    private static final String SEL_SET_REMINDER = "0x6a4c9f2b";
    private static final String SEL_CREATE_SESSION = "0xb8e2d5a1";
    private static final String SEL_CLOSE_SESSION = "0x4f7c3e9a";
    private static final String SEL_STORE_PREFERENCE = "0x1d6b8a4e";
    private static final String SEL_REGISTER_INTENT = "0xa3f5c2d8";
    private static final String SEL_SUBMIT_FEEDBACK = "0x8e2a7b4f";
    private static final String SEL_DEPOSIT = "0xd0de0b6b";
    private static final String SEL_PREF_KEY_HASH = "0x5b9e1c7a";

    private String rpcUrl = DEFAULT_RPC;
    private String privateKeyHex;

    public HaZZa() {}

    public void setRpcUrl(String url) { this.rpcUrl = url != null ? url : DEFAULT_RPC; }
    public String getRpcUrl() { return rpcUrl; }
    public void setPrivateKeyHex(String hex) { this.privateKeyHex = hex; }
    public boolean hasPrivateKey() { return privateKeyHex != null && !privateKeyHex.isBlank(); }

    public static final class TaskView {
        public String taskId;
        public String owner;
        public int kind;
        public BigInteger dueAt;
        public int status;
        public BigInteger createdAt;
        @Override
        public String toString() {
            return String.format("TaskView{id=%s owner=%s kind=%d dueAt=%s status=%d}", taskId, owner, kind, dueAt, status);
        }
    }

    public static final class ReminderView {
        public String reminderId;
        public String owner;
        public BigInteger triggerAt;
        public String linkedTaskId;
        public boolean fired;
        public BigInteger createdAt;
        @Override
        public String toString() {
            return String.format("ReminderView{id=%s owner=%s triggerAt=%s fired=%s}", reminderId, owner, triggerAt, fired);
        }
    }

    public static final class SessionView {
        public String sessionId;
        public String owner;
        public BigInteger startedAt;
        public BigInteger closedAt;
        public BigInteger responseCount;
        @Override
        public String toString() {
            return String.format("SessionView{id=%s owner=%s started=%s closed=%s responses=%s}", sessionId, owner, startedAt, closedAt, responseCount);
        }
    }

    public static final class IntentView {
        public String intentId;
        public String owner;
        public int intentType;
        public BigInteger createdAt;
    }

    public static final class PlatformStats {
        public BigInteger taskCount;
        public BigInteger reminderCount;
        public BigInteger sessionCount;
        public BigInteger intentCount;
        public BigInteger deployBlockNum;
        public boolean paused;
    }

    public static final class ConfigView {
        public BigInteger maxTasksPerUser;
        public BigInteger maxRemindersPerUser;
        public BigInteger feeWei;
        public boolean paused;
    }

    private static String padLeft(String hex, int bytesLen) {
        int len = bytesLen * 2;
        if (hex == null) hex = "0";
        hex = hex.replaceFirst("^0x", "");
        if (hex.length() >= len) return hex.substring(hex.length() - len);
        return "0".repeat(len - hex.length()) + hex;
    }

    private static String padAddress(String addr) {
        if (addr == null) return padLeft("0", 20);
        addr = addr.replaceFirst("^0x", "");
        return padLeft(addr, 20);
    }

    private static String padBytes32(String h) {
        if (h == null) h = "0";
        h = h.replaceFirst("^0x", "");
        return padLeft(h, 32);
    }

    private static String padUint256(BigInteger n) {
        if (n == null) n = BigInteger.ZERO;
        return padLeft(n.toString(16), 32);
    }

    private static String encodeBool(boolean b) {
        return padLeft(b ? "1" : "0", 32);
    }

    private String ethCall(String to, String data) throws IOException {
        String body = "{\"jsonrpc\":\"2.0\",\"method\":\"eth_call\",\"params\":[{\"to\":\"" + to + "\",\"data\":\"" + data + "\"},\"latest\"],\"id\":1}";
        return postJson(rpcUrl, body);
    }

    private String postJson(String urlString, String jsonBody) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
        }
        int code = conn.getResponseCode();
        if (code != 200) {
            try (InputStream err = conn.getErrorStream()) {
                String errBody = err != null ? new String(err.readAllBytes(), StandardCharsets.UTF_8) : "";
                throw new IOException("HTTP " + code + " " + errBody);
            }
        }
        try (InputStream is = conn.getInputStream()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static String extractResult(String jsonResponse) {
        if (jsonResponse == null) return null;
        int i = jsonResponse.indexOf("\"result\":\"");
        if (i < 0) return null;
        i += 10;
        int j = jsonResponse.indexOf("\"", i);
        if (j < 0) return null;
        return jsonResponse.substring(i, j);
    }

    private static String extractResultBytes(String jsonResponse) {
        if (jsonResponse == null) return null;
        int i = jsonResponse.indexOf("\"result\":\"0x");
        if (i < 0) return null;
        i += 11;
        int j = jsonResponse.indexOf("\"", i);
        if (j < 0) return null;
        return jsonResponse.substring(i - 2, j);
    }

    public BigInteger getTaskIdsLength() throws IOException {
        String data = SEL_GET_TASK_IDS_LENGTH;
        String raw = ethCall(HARIBA_CONTRACT, data);
        String result = extractResult(raw);
        if (result == null || result.length() < 66) return BigInteger.ZERO;
        return new BigInteger(result.substring(2), 16);
    }

    public BigInteger getReminderIdsLength() throws IOException {
        String data = SEL_GET_REMINDER_IDS_LENGTH;
        String raw = ethCall(HARIBA_CONTRACT, data);
        String result = extractResult(raw);
        if (result == null || result.length() < 66) return BigInteger.ZERO;
        return new BigInteger(result.substring(2), 16);
    }

    public BigInteger getSessionIdsLength() throws IOException {
        String data = SEL_GET_SESSION_IDS_LENGTH;
        String raw = ethCall(HARIBA_CONTRACT, data);
        String result = extractResult(raw);
        if (result == null || result.length() < 66) return BigInteger.ZERO;
        return new BigInteger(result.substring(2), 16);
    }

    public BigInteger getIntentIdsLength() throws IOException {
        String data = SEL_GET_INTENT_IDS_LENGTH;
        String raw = ethCall(HARIBA_CONTRACT, data);
        String result = extractResult(raw);
        if (result == null || result.length() < 66) return BigInteger.ZERO;
        return new BigInteger(result.substring(2), 16);
    }

    public String getTaskIdAt(BigInteger index) throws IOException {
        String data = SEL_GET_TASK_ID_AT + padUint256(index);
        String raw = ethCall(HARIBA_CONTRACT, data);
        String result = extractResult(raw);
        if (result == null || result.length() < 66) return null;
        return "0x" + result.substring(2).toLowerCase();
    }

    public String getReminderIdAt(BigInteger index) throws IOException {
        String data = SEL_GET_REMINDER_ID_AT + padUint256(index);
        String raw = ethCall(HARIBA_CONTRACT, data);
        String result = extractResult(raw);
        if (result == null || result.length() < 66) return null;
        return "0x" + result.substring(2).toLowerCase();
    }

    public String getSessionIdAt(BigInteger index) throws IOException {
        String data = SEL_GET_SESSION_ID_AT + padUint256(index);
        String raw = ethCall(HARIBA_CONTRACT, data);
        String result = extractResult(raw);
        if (result == null || result.length() < 66) return null;
        return "0x" + result.substring(2).toLowerCase();
    }

    public String getIntentIdAt(BigInteger index) throws IOException {
        String data = SEL_GET_INTENT_ID_AT + padUint256(index);
        String raw = ethCall(HARIBA_CONTRACT, data);
        String result = extractResult(raw);
        if (result == null || result.length() < 66) return null;
        return "0x" + result.substring(2).toLowerCase();
    }

    public TaskView getTaskView(String taskIdHex) throws IOException {
        String data = SEL_GET_TASK + padBytes32(taskIdHex);
        String raw = ethCall(HARIBA_CONTRACT, data);
        String result = extractResultBytes(raw);
        TaskView v = decodeTaskView(result);
        if (v != null) v.taskId = taskIdHex;
        return v;
    }

    private TaskView decodeTaskView(String hex) {
        if (hex == null || hex.length() < 2) return null;
        hex = hex.replaceFirst("^0x", "");
        if (hex.length() < 64 * 5) return null;
        TaskView v = new TaskView();
        v.owner = "0x" + hex.substring(64 + 24, 64 + 64);
        v.kind = new BigInteger(hex.substring(128, 192), 16).intValue();
        v.dueAt = new BigInteger(hex.substring(192, 256), 16);
        v.status = new BigInteger(hex.substring(256, 320), 16).intValue();
        v.createdAt = new BigInteger(hex.substring(320, 384), 16);
        return v;
    }

    public ReminderView getReminderView(String reminderIdHex) throws IOException {
        String data = SEL_GET_REMINDER + padBytes32(reminderIdHex);
        String raw = ethCall(HARIBA_CONTRACT, data);
        String result = extractResultBytes(raw);
        ReminderView v = decodeReminderView(result);
        if (v != null) v.reminderId = reminderIdHex;
        return v;
