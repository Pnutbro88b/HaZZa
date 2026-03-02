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
    }

    private ReminderView decodeReminderView(String hex) {
        if (hex == null || hex.length() < 2) return null;
        hex = hex.replaceFirst("^0x", "");
        if (hex.length() < 64 * 5) return null;
        ReminderView v = new ReminderView();
        v.owner = "0x" + hex.substring(64 + 24, 64 + 64);
        v.triggerAt = new BigInteger(hex.substring(128, 192), 16);
        v.linkedTaskId = "0x" + hex.substring(192, 256);
        v.fired = new BigInteger(hex.substring(256, 320), 16).signum() != 0;
        v.createdAt = new BigInteger(hex.substring(320, 384), 16);
        return v;
    }

    public SessionView getSessionView(String sessionIdHex) throws IOException {
        String data = SEL_GET_SESSION + padBytes32(sessionIdHex);
        String raw = ethCall(HARIBA_CONTRACT, data);
        String result = extractResultBytes(raw);
        SessionView v = decodeSessionView(result);
        if (v != null) v.sessionId = sessionIdHex;
        return v;
    }

    private SessionView decodeSessionView(String hex) {
        if (hex == null || hex.length() < 2) return null;
        hex = hex.replaceFirst("^0x", "");
        if (hex.length() < 64 * 4) return null;
        SessionView v = new SessionView();
        v.owner = "0x" + hex.substring(64 + 24, 64 + 64);
        v.startedAt = new BigInteger(hex.substring(128, 192), 16);
        v.closedAt = new BigInteger(hex.substring(192, 256), 16);
        v.responseCount = new BigInteger(hex.substring(256, 320), 16);
        return v;
    }

    public IntentView getIntentView(String intentIdHex) throws IOException {
        String data = SEL_GET_INTENT + padBytes32(intentIdHex);
        String raw = ethCall(HARIBA_CONTRACT, data);
        String result = extractResultBytes(raw);
        IntentView v = decodeIntentView(result);
        if (v != null) v.intentId = intentIdHex;
        return v;
    }

    private IntentView decodeIntentView(String hex) {
        if (hex == null || hex.length() < 2) return null;
        hex = hex.replaceFirst("^0x", "");
        if (hex.length() < 64 * 3) return null;
        IntentView v = new IntentView();
        v.owner = "0x" + hex.substring(64 + 24, 64 + 64);
        v.intentType = new BigInteger(hex.substring(128, 192), 16).intValue();
        v.createdAt = new BigInteger(hex.substring(192, 256), 16);
        return v;
    }

    public TaskView getTaskViewByIndex(BigInteger index) throws IOException {
        String id = getTaskIdAt(index);
        if (id == null) return null;
        return getTaskView(id);
    }

    public ReminderView getReminderViewByIndex(BigInteger index) throws IOException {
        String id = getReminderIdAt(index);
        if (id == null) return null;
        return getReminderView(id);
    }

    public SessionView getSessionViewByIndex(BigInteger index) throws IOException {
        String id = getSessionIdAt(index);
        if (id == null) return null;
        return getSessionView(id);
    }

    public IntentView getIntentViewByIndex(BigInteger index) throws IOException {
        String id = getIntentIdAt(index);
        if (id == null) return null;
        return getIntentView(id);
    }

    public boolean isPaused() throws IOException {
        String data = SEL_IS_PAUSED;
        String raw = ethCall(HARIBA_CONTRACT, data);
        String result = extractResult(raw);
        if (result == null || result.length() < 66) return true;
        return new BigInteger(result.substring(2), 16).signum() != 0;
    }

    public BigInteger getFeeWei() throws IOException {
        String data = SEL_GET_FEE_WEI;
        String raw = ethCall(HARIBA_CONTRACT, data);
        String result = extractResult(raw);
        if (result == null || result.length() < 66) return BigInteger.ZERO;
        return new BigInteger(result.substring(2), 16);
    }

    public BigInteger getMaxTasksPerUser() throws IOException {
        String data = SEL_GET_MAX_TASKS_PER_USER;
        String raw = ethCall(HARIBA_CONTRACT, data);
        String result = extractResult(raw);
        if (result == null || result.length() < 66) return BigInteger.ZERO;
        return new BigInteger(result.substring(2), 16);
    }

    public BigInteger getMaxRemindersPerUser() throws IOException {
        String data = SEL_GET_MAX_REMINDERS_PER_USER;
        String raw = ethCall(HARIBA_CONTRACT, data);
        String result = extractResult(raw);
        if (result == null || result.length() < 66) return BigInteger.ZERO;
        return new BigInteger(result.substring(2), 16);
    }

    public BigInteger getVaultBalance() throws IOException {
        String data = SEL_GET_VAULT_BALANCE;
        String raw = ethCall(HARIBA_CONTRACT, data);
        String result = extractResult(raw);
        if (result == null || result.length() < 66) return BigInteger.ZERO;
        return new BigInteger(result.substring(2), 16);
    }

    public BigInteger getDeployBlock() throws IOException {
        String data = SEL_GET_DEPLOY_BLOCK;
        String raw = ethCall(HARIBA_CONTRACT, data);
        String result = extractResult(raw);
        if (result == null || result.length() < 66) return BigInteger.ZERO;
        return new BigInteger(result.substring(2), 16);
    }

    public PlatformStats getPlatformStats() throws IOException {
        String data = SEL_GET_PLATFORM_STATS;
        String raw = ethCall(HARIBA_CONTRACT, data);
        String result = extractResultBytes(raw);
        if (result == null || result.length() < 2) return null;
        result = result.replaceFirst("^0x", "");
        if (result.length() < 64 * 6) return null;
        PlatformStats s = new PlatformStats();
        s.taskCount = new BigInteger(result.substring(0, 64), 16);
        s.reminderCount = new BigInteger(result.substring(64, 128), 16);
        s.sessionCount = new BigInteger(result.substring(128, 192), 16);
        s.intentCount = new BigInteger(result.substring(192, 256), 16);
        s.deployBlockNum = new BigInteger(result.substring(256, 320), 16);
        s.paused = new BigInteger(result.substring(320, 384), 16).signum() != 0;
        return s;
    }

    public ConfigView getConfig() throws IOException {
        String data = SEL_GET_CONFIG;
        String raw = ethCall(HARIBA_CONTRACT, data);
        String result = extractResultBytes(raw);
        if (result == null || result.length() < 2) return null;
        result = result.replaceFirst("^0x", "");
        if (result.length() < 64 * 4) return null;
        ConfigView c = new ConfigView();
        c.maxTasksPerUser = new BigInteger(result.substring(0, 64), 16);
        c.maxRemindersPerUser = new BigInteger(result.substring(64, 128), 16);
        c.feeWei = new BigInteger(result.substring(128, 192), 16);
        c.paused = new BigInteger(result.substring(192, 256), 16).signum() != 0;
        return c;
    }

    public BigInteger getTaskCountForOwner(String ownerAddr) throws IOException {
        String data = SEL_GET_TASK_COUNT_FOR_OWNER + padAddress(ownerAddr);
        String raw = ethCall(HARIBA_CONTRACT, data);
        String result = extractResult(raw);
        if (result == null || result.length() < 66) return BigInteger.ZERO;
        return new BigInteger(result.substring(2), 16);
    }

    public BigInteger getReminderCountForOwner(String ownerAddr) throws IOException {
        String data = SEL_GET_REMINDER_COUNT_FOR_OWNER + padAddress(ownerAddr);
        String raw = ethCall(HARIBA_CONTRACT, data);
        String result = extractResult(raw);
        if (result == null || result.length() < 66) return BigInteger.ZERO;
        return new BigInteger(result.substring(2), 16);
    }

    public BigInteger getSessionCountForOwner(String ownerAddr) throws IOException {
        String data = SEL_GET_SESSION_COUNT_FOR_OWNER + padAddress(ownerAddr);
        String raw = ethCall(HARIBA_CONTRACT, data);
        String result = extractResult(raw);
        if (result == null || result.length() < 66) return BigInteger.ZERO;
        return new BigInteger(result.substring(2), 16);
    }

    public BigInteger getResponseCount(String sessionIdHex) throws IOException {
        String data = SEL_GET_RESPONSE_COUNT + padBytes32(sessionIdHex);
        String raw = ethCall(HARIBA_CONTRACT, data);
        String result = extractResult(raw);
        if (result == null || result.length() < 66) return BigInteger.ZERO;
        return new BigInteger(result.substring(2), 16);
    }

    public String buildEnqueueTaskData(int kind, BigInteger dueAt) {
        return SEL_ENQUEUE_TASK + padUint256(BigInteger.valueOf(kind)) + padUint256(dueAt);
    }

    public String buildCompleteTaskData(String taskIdHex) {
        return SEL_COMPLETE_TASK + padBytes32(taskIdHex);
    }

    public String buildCancelTaskData(String taskIdHex) {
        return SEL_CANCEL_TASK + padBytes32(taskIdHex);
    }

    public String buildSetReminderData(BigInteger triggerAt, String linkedTaskIdHex) {
        return SEL_SET_REMINDER + padUint256(triggerAt) + padBytes32(linkedTaskIdHex);
    }

    public String buildCreateSessionData() {
        return SEL_CREATE_SESSION;
    }

    public String buildCloseSessionData(String sessionIdHex) {
        return SEL_CLOSE_SESSION + padBytes32(sessionIdHex);
    }

    public String buildStorePreferenceData(String keyHashHex, byte[] value) {
        int len = value == null ? 0 : value.length;
        String valHex = value == null ? "" : bytesToHex(value);
        int padLen = ((len + 31) / 32) * 32 * 2;
        if (valHex.length() < padLen) valHex = padLeft(valHex, padLen / 2);
        return SEL_STORE_PREFERENCE + padBytes32(keyHashHex) + padUint256(BigInteger.valueOf(64)) + padUint256(BigInteger.valueOf(len)) + valHex;
    }

    public String buildRegisterIntentData(int intentType) {
        return SEL_REGISTER_INTENT + padUint256(BigInteger.valueOf(intentType));
    }

    public String buildSubmitFeedbackData(String refIdHex, int rating) {
        return SEL_SUBMIT_FEEDBACK + padBytes32(refIdHex) + padUint256(BigInteger.valueOf(rating));
    }

    public String buildDepositData() {
        return SEL_DEPOSIT;
    }

    public String prefKeyHash(String key) {
        return sha256Hex(key.getBytes(StandardCharsets.UTF_8));
    }

    private static String bytesToHex(byte[] b) {
        if (b == null) return "";
        StringBuilder sb = new StringBuilder(b.length * 2);
        for (byte x : b) sb.append(String.format("%02x", x));
        return sb.toString();
    }

    private static String sha256Hex(byte[] input) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] h = md.digest(input);
            return "0x" + bytesToHex(h);
        } catch (Exception e) {
            return "0x" + "0".repeat(64);
        }
    }

    public List<TaskView> getTaskViewsBatch(int offset, int limit) throws IOException {
        BigInteger len = getTaskIdsLength();
        if (len.compareTo(BigInteger.valueOf(offset)) <= 0) return Collections.emptyList();
        int end = Math.min(offset + limit, len.intValue());
        int batch = Math.min(limit, HRB_VIEW_BATCH.intValue());
        List<TaskView> list = new ArrayList<>();
        for (int i = offset; i < end && (i - offset) < batch; i++) {
            TaskView v = getTaskViewByIndex(BigInteger.valueOf(i));
            if (v != null) list.add(v);
        }
        return list;
    }

    public List<ReminderView> getReminderViewsBatch(int offset, int limit) throws IOException {
        BigInteger len = getReminderIdsLength();
        if (len.compareTo(BigInteger.valueOf(offset)) <= 0) return Collections.emptyList();
        int end = Math.min(offset + limit, len.intValue());
        int batch = Math.min(limit, HRB_VIEW_BATCH.intValue());
        List<ReminderView> list = new ArrayList<>();
        for (int i = offset; i < end && (i - offset) < batch; i++) {
            ReminderView v = getReminderViewByIndex(BigInteger.valueOf(i));
            if (v != null) list.add(v);
        }
        return list;
    }

    public List<SessionView> getSessionViewsBatch(int offset, int limit) throws IOException {
        BigInteger len = getSessionIdsLength();
        if (len.compareTo(BigInteger.valueOf(offset)) <= 0) return Collections.emptyList();
        int end = Math.min(offset + limit, len.intValue());
        int batch = Math.min(limit, HRB_VIEW_BATCH.intValue());
        List<SessionView> list = new ArrayList<>();
        for (int i = offset; i < end && (i - offset) < batch; i++) {
            SessionView v = getSessionViewByIndex(BigInteger.valueOf(i));
            if (v != null) list.add(v);
        }
        return list;
    }

    public List<IntentView> getIntentViewsBatch(int offset, int limit) throws IOException {
        BigInteger len = getIntentIdsLength();
        if (len.compareTo(BigInteger.valueOf(offset)) <= 0) return Collections.emptyList();
        int end = Math.min(offset + limit, len.intValue());
        int batch = Math.min(limit, HRB_VIEW_BATCH.intValue());
        List<IntentView> list = new ArrayList<>();
        for (int i = offset; i < end && (i - offset) < batch; i++) {
            IntentView v = getIntentViewByIndex(BigInteger.valueOf(i));
            if (v != null) list.add(v);
        }
        return list;
    }

    public static String taskStatusName(int status) {
        switch (status) {
            case TASK_STATUS_PENDING: return "PENDING";
            case TASK_STATUS_COMPLETED: return "COMPLETED";
            case TASK_STATUS_CANCELLED: return "CANCELLED";
            default: return "UNKNOWN(" + status + ")";
        }
    }

    public static String taskKindName(int kind) {
        switch (kind) {
            case TASK_KIND_GENERIC: return "GENERIC";
            case TASK_KIND_CALL: return "CALL";
            case TASK_KIND_MEETING: return "MEETING";
            case TASK_KIND_DEADLINE: return "DEADLINE";
            default: return "UNKNOWN(" + kind + ")";
        }
    }

    public static boolean isValidAddress(String addr) {
        return addr != null && ADDRESS_PATTERN.matcher(addr.trim()).matches();
    }

    public static boolean isValidBytes32(String id) {
        return id != null && BYTES32_PATTERN.matcher(id.replaceFirst("^0x", "0x").trim()).matches();
    }

    public static String weiToEther(BigInteger wei) {
        if (wei == null) return "0";
        BigInteger[] divRem = wei.divideAndRemainder(WEI_PER_ETHER);
        if (divRem[1].signum() == 0) return divRem[0].toString();
        String frac = divRem[1].toString();
        frac = "0".repeat(Math.max(0, 18 - frac.length())) + frac;
        return divRem[0] + "." + frac.replaceFirst("0+$", "");
    }

    public static String formatIdShort(String id) {
        if (id == null || id.length() < 10) return id;
        return id.substring(0, 10) + "..." + id.substring(id.length() - 8);
    }

    public static String formatAddressShort(String addr) {
        if (addr == null || addr.length() < 10) return addr;
        return addr.substring(0, 6) + "..." + addr.substring(addr.length() - 4);
    }

    public void printTaskView(TaskView v) {
        if (v == null) { System.out.println("(null task)"); return; }
        System.out.printf("  taskId=%s owner=%s kind=%s dueAt=%s status=%s%n",
            formatIdShort(v.taskId), formatAddressShort(v.owner), taskKindName(v.kind), v.dueAt, taskStatusName(v.status));
    }

    public void printReminderView(ReminderView v) {
        if (v == null) { System.out.println("(null reminder)"); return; }
        System.out.printf("  reminderId=%s owner=%s triggerAt=%s fired=%s%n",
            formatIdShort(v.reminderId), formatAddressShort(v.owner), v.triggerAt, v.fired);
    }

    public void printSessionView(SessionView v) {
        if (v == null) { System.out.println("(null session)"); return; }
        System.out.printf("  sessionId=%s owner=%s started=%s closed=%s responses=%s%n",
            formatIdShort(v.sessionId), formatAddressShort(v.owner), v.startedAt, v.closedAt, v.responseCount);
    }

    public void runListTasks(int offset, int limit) {
        try {
            List<TaskView> list = getTaskViewsBatch(offset, limit);
            System.out.println("Tasks (offset=" + offset + " limit=" + limit + ") count=" + list.size());
            for (TaskView v : list) printTaskView(v);
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    public void runListReminders(int offset, int limit) {
        try {
            List<ReminderView> list = getReminderViewsBatch(offset, limit);
            System.out.println("Reminders (offset=" + offset + " limit=" + limit + ") count=" + list.size());
            for (ReminderView v : list) printReminderView(v);
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    public void runListSessions(int offset, int limit) {
        try {
            List<SessionView> list = getSessionViewsBatch(offset, limit);
            System.out.println("Sessions (offset=" + offset + " limit=" + limit + ") count=" + list.size());
            for (SessionView v : list) printSessionView(v);
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    public void runPlatformStats() {
        try {
            PlatformStats s = getPlatformStats();
            if (s == null) { System.out.println("(null stats)"); return; }
            System.out.println("Tasks=" + s.taskCount + " Reminders=" + s.reminderCount + " Sessions=" + s.sessionCount + " Intents=" + s.intentCount + " DeployBlock=" + s.deployBlockNum + " Paused=" + s.paused);
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    public void runConfig() {
        try {
            ConfigView c = getConfig();
            if (c == null) { System.out.println("(null config)"); return; }
            System.out.println("MaxTasksPerUser=" + c.maxTasksPerUser + " MaxRemindersPerUser=" + c.maxRemindersPerUser + " FeeWei=" + c.feeWei + " Paused=" + c.paused);
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    public void runGetTask(Scanner sc) {
        System.out.print("Task ID (0x...): ");
        String id = sc.nextLine().trim();
        try {
            TaskView v = getTaskView(id);
            printTaskView(v);
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    public void runGetReminder(Scanner sc) {
        System.out.print("Reminder ID (0x...): ");
        String id = sc.nextLine().trim();
        try {
            ReminderView v = getReminderView(id);
            printReminderView(v);
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    public void runGetSession(Scanner sc) {
        System.out.print("Session ID (0x...): ");
        String id = sc.nextLine().trim();
        try {
            SessionView v = getSessionView(id);
            printSessionView(v);
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    public void runEnqueueTaskInteractive(Scanner sc) {
        System.out.print("Task kind (0=generic 1=call 2=meeting 3=deadline): ");
        int kind = Integer.parseInt(sc.nextLine().trim());
        System.out.print("Due timestamp: ");
        BigInteger dueAt = new BigInteger(sc.nextLine().trim());
        String data = buildEnqueueTaskData(kind, dueAt);
        try {
            BigInteger fee = getFeeWei();
            System.out.println("Fee (wei): " + fee + " (" + weiToEther(fee) + " ether)");
        } catch (IOException e) {}
        System.out.println("Calldata: " + data.substring(0, Math.min(80, data.length())) + "...");
        if (!hasPrivateKey()) System.out.println("Set private key to send transaction.");
    }

    public void runSetReminderInteractive(Scanner sc) {
        System.out.print("Trigger timestamp: ");
        BigInteger triggerAt = new BigInteger(sc.nextLine().trim());
        System.out.print("Linked task ID (0x... or 0): ");
        String linked = sc.nextLine().trim();
        if (linked.isEmpty()) linked = "0x" + "0".repeat(64);
        String data = buildSetReminderData(triggerAt, linked);
        System.out.println("Calldata: " + data.substring(0, Math.min(80, data.length())) + "...");
    }

    public void runCreateSessionInteractive(Scanner sc) {
        String data = buildCreateSessionData();
        System.out.println("Calldata: " + data);
        if (!hasPrivateKey()) System.out.println("Set private key to send transaction.");
    }

    public void runCloseSessionInteractive(Scanner sc) {
        System.out.print("Session ID (0x...): ");
        String id = sc.nextLine().trim();
        String data = buildCloseSessionData(id);
        System.out.println("Calldata: " + data);
    }

    public void runRegisterIntentInteractive(Scanner sc) {
        System.out.print("Intent type (0-7): ");
        int t = Integer.parseInt(sc.nextLine().trim());
        String data = buildRegisterIntentData(t);
        System.out.println("Calldata: " + data.substring(0, Math.min(80, data.length())) + "...");
    }

    public void runDepositInteractive(Scanner sc) {
        System.out.print("Amount (wei) to send with tx: ");
        String amt = sc.nextLine().trim();
        System.out.println("Use buildDepositData() and send value=" + amt + " wei. Calldata: " + buildDepositData());
    }

    public void loadConfig() {
        try {
            Path p = Paths.get(CONFIG_FILENAME);
            if (!Files.exists(p)) return;
            List<String> lines = Files.readAllLines(p);
            for (String line : lines) {
                line = line.trim();
                if (line.startsWith("#") || line.isEmpty()) continue;
                int eq = line.indexOf('=');
                if (eq <= 0) continue;
                String key = line.substring(0, eq).trim();
                String val = line.substring(eq + 1).trim();
                if ("rpc".equalsIgnoreCase(key)) setRpcUrl(val);
            }
        } catch (IOException e) {}
    }

    public void saveConfig() {
        try {
            String content = "# HaZZa config\nrpc=" + rpcUrl + "\ncontract=" + HARIBA_CONTRACT + "\n";
            Files.writeString(Paths.get(CONFIG_FILENAME), content);
        } catch (IOException e) {
            System.err.println("Could not save config: " + e.getMessage());
        }
    }

    public void printContractInfo() {
        System.out.println("Hariba: " + HARIBA_CONTRACT);
        System.out.println("Steward: " + formatAddressShort(HRB_STEWARD));
        System.out.println("Vault: " + formatAddressShort(HRB_VAULT));
        System.out.println("Oracle: " + formatAddressShort(HRB_ORACLE));
        System.out.println("Relay: " + formatAddressShort(HRB_RELAY));
    }

    public List<TaskView> getPendingTasksOnly(int offset, int limit) throws IOException {
        List<TaskView> all = getTaskViewsBatch(offset, limit * 2);
        return all.stream().filter(v -> v.status == TASK_STATUS_PENDING).limit(limit).collect(Collectors.toList());
    }

    public List<TaskView> getCompletedTasksOnly(int offset, int limit) throws IOException {
        List<TaskView> all = getTaskViewsBatch(offset, limit * 2);
        return all.stream().filter(v -> v.status == TASK_STATUS_COMPLETED).limit(limit).collect(Collectors.toList());
    }

    public List<ReminderView> getUnfiredRemindersOnly(int offset, int limit) throws IOException {
        List<ReminderView> all = getReminderViewsBatch(offset, limit * 2);
        return all.stream().filter(v -> !v.fired).limit(limit).collect(Collectors.toList());
    }

    public List<SessionView> getOpenSessionsOnly(int offset, int limit) throws IOException {
        List<SessionView> all = getSessionViewsBatch(offset, limit * 2);
        return all.stream().filter(v -> v.closedAt.compareTo(BigInteger.ZERO) == 0).limit(limit).collect(Collectors.toList());
    }

    public BigInteger estimateFeeForEnqueueTask() throws IOException {
        return getFeeWei();
    }

    public void runFeeInfo() {
        try {
            BigInteger fee = getFeeWei();
            System.out.println("Task enqueue fee: " + fee + " wei (" + weiToEther(fee) + " ether)");
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    private static final String[] FALLBACK_RPC_URLS = {
        "https://eth.llamarpc.com",
        "https://rpc.ankr.com/eth",
        "https://ethereum.publicnode.com",
        "https://1rpc.io/eth",
        "https://cloudflare-eth.com"
    };

    public boolean tryFallbackRpc() {
        for (String url : FALLBACK_RPC_URLS) {
            if (url.equals(rpcUrl)) continue;
            setRpcUrl(url);
            try {
                getTaskIdsLength();
                System.out.println("Fallback RPC OK: " + url);
                return true;
            } catch (IOException e) {}
        }
        return false;
    }

    public static final class Snapshot {
        public final long timestamp;
        public final List<TaskView> tasks;
        public final List<ReminderView> reminders;
        public final PlatformStats stats;

        public Snapshot(long timestamp, List<TaskView> tasks, List<ReminderView> reminders, PlatformStats stats) {
            this.timestamp = timestamp;
            this.tasks = tasks != null ? new ArrayList<>(tasks) : Collections.emptyList();
            this.reminders = reminders != null ? new ArrayList<>(reminders) : Collections.emptyList();
            this.stats = stats;
        }
    }

    public Snapshot captureSnapshot(int maxTasks, int maxReminders) throws IOException {
        List<TaskView> tasks = getTaskViewsBatch(0, maxTasks);
        List<ReminderView> reminders = getReminderViewsBatch(0, maxReminders);
        PlatformStats stats = getPlatformStats();
        return new Snapshot(System.currentTimeMillis(), tasks, reminders, stats);
    }

    public void printSnapshot(Snapshot snap) {
        System.out.println("--- PA snapshot @ " + new Date(snap.timestamp) + " ---");
        System.out.println("Tasks: " + snap.tasks.size());
        for (TaskView v : snap.tasks) printTaskView(v);
        System.out.println("Reminders: " + snap.reminders.size());
        for (ReminderView v : snap.reminders) printReminderView(v);
        if (snap.stats != null) System.out.println("Platform: tasks=" + snap.stats.taskCount + " paused=" + snap.stats.paused);
    }

    public void runSnapshot() {
        try {
            Snapshot snap = captureSnapshot(16, 16);
            printSnapshot(snap);
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    public void exportTasksToCsv(String filepath, int maxTasks) throws IOException {
        List<TaskView> list = getTaskViewsBatch(0, maxTasks);
        StringBuilder sb = new StringBuilder();
        sb.append("taskId,owner,kind,dueAt,status,createdAt\n");
        for (TaskView v : list) {
            sb.append(v.taskId).append(",").append(v.owner).append(",").append(v.kind).append(",")
              .append(v.dueAt).append(",").append(v.status).append(",").append(v.createdAt).append("\n");
        }
        Files.writeString(Paths.get(filepath), sb.toString());
        System.out.println("Exported " + list.size() + " tasks to " + filepath);
    }

    public void runExportTasksCsv(Scanner sc) {
        System.out.print("Output file: ");
        String path = sc.nextLine().trim();
        System.out.print("Max tasks: ");
        int max = Integer.parseInt(sc.nextLine().trim());
        try {
            exportTasksToCsv(path, max);
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    public boolean wouldEnqueueSucceed(int kind, BigInteger dueAt) throws IOException {
        if (kind < 0 || kind > TASK_KIND_DEADLINE) return false;
        if (isPaused()) return false;
        BigInteger fee = getFeeWei();
        if (fee == null || fee.signum() < 0) return false;
        BigInteger totalTasks = getTaskIdsLength();
        if (totalTasks.compareTo(new BigInteger("4096")) >= 0) return false;
        return true;
    }

    public void runWouldEnqueueSucceed(Scanner sc) {
        System.out.print("Kind (0-3): ");
        int kind = Integer.parseInt(sc.nextLine().trim());
        System.out.print("Due timestamp: ");
        BigInteger due = new BigInteger(sc.nextLine().trim());
        try {
            boolean ok = wouldEnqueueSucceed(kind, due);
            System.out.println("Would enqueue succeed: " + ok);
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    private String ethCallWithRetry(String to, String data) throws IOException {
        IOException last = null;
        for (int i = 0; i < MAX_RPC_RETRIES; i++) {
            try {
                return ethCall(to, data);
            } catch (IOException e) {
                last = e;
                if (i < MAX_RPC_RETRIES - 1) {
                    try { Thread.sleep(RPC_RETRY_DELAY_MS); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); throw new IOException(ie); }
                }
            }
        }
        throw last != null ? last : new IOException("RPC failed");
    }

    public List<TaskView> getTasksByOwner(String ownerAddr, int maxCount) throws IOException {
        BigInteger total = getTaskIdsLength();
        List<TaskView> out = new ArrayList<>();
        for (BigInteger i = BigInteger.ZERO; i.compareTo(total) < 0 && out.size() < maxCount; i = i.add(BigInteger.ONE)) {
            TaskView v = getTaskViewByIndex(i);
            if (v != null && ownerAddr != null && ownerAddr.equalsIgnoreCase(v.owner)) out.add(v);
        }
        return out;
    }

    public List<ReminderView> getRemindersByOwner(String ownerAddr, int maxCount) throws IOException {
        BigInteger total = getReminderIdsLength();
        List<ReminderView> out = new ArrayList<>();
        for (BigInteger i = BigInteger.ZERO; i.compareTo(total) < 0 && out.size() < maxCount; i = i.add(BigInteger.ONE)) {
            ReminderView v = getReminderViewByIndex(i);
            if (v != null && ownerAddr != null && ownerAddr.equalsIgnoreCase(v.owner)) out.add(v);
        }
        return out;
    }

    public List<SessionView> getSessionsByOwner(String ownerAddr, int maxCount) throws IOException {
        BigInteger total = getSessionIdsLength();
        List<SessionView> out = new ArrayList<>();
        for (BigInteger i = BigInteger.ZERO; i.compareTo(total) < 0 && out.size() < maxCount; i = i.add(BigInteger.ONE)) {
            SessionView v = getSessionViewByIndex(i);
            if (v != null && ownerAddr != null && ownerAddr.equalsIgnoreCase(v.owner)) out.add(v);
        }
        return out;
    }

    public void runListTasksByOwner(Scanner sc) {
        System.out.print("Owner address (0x...): ");
        String addr = sc.nextLine().trim();
        if (!isValidAddress(addr)) { System.err.println("Invalid address"); return; }
        try {
            List<TaskView> list = getTasksByOwner(addr, 64);
            System.out.println("Tasks for owner: " + list.size());
            for (TaskView v : list) printTaskView(v);
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    public void runListRemindersByOwner(Scanner sc) {
        System.out.print("Owner address (0x...): ");
        String addr = sc.nextLine().trim();
        if (!isValidAddress(addr)) { System.err.println("Invalid address"); return; }
        try {
            List<ReminderView> list = getRemindersByOwner(addr, 64);
            System.out.println("Reminders for owner: " + list.size());
            for (ReminderView v : list) printReminderView(v);
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    public void runListSessionsByOwner(Scanner sc) {
        System.out.print("Owner address (0x...): ");
        String addr = sc.nextLine().trim();
        if (!isValidAddress(addr)) { System.err.println("Invalid address"); return; }
        try {
            List<SessionView> list = getSessionsByOwner(addr, 64);
            System.out.println("Sessions for owner: " + list.size());
            for (SessionView v : list) printSessionView(v);
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    public void exportRemindersToCsv(String filepath, int maxReminders) throws IOException {
        List<ReminderView> list = getReminderViewsBatch(0, maxReminders);
        StringBuilder sb = new StringBuilder();
        sb.append("reminderId,owner,triggerAt,linkedTaskId,fired,createdAt\n");
        for (ReminderView v : list) {
            sb.append(v.reminderId).append(",").append(v.owner).append(",").append(v.triggerAt).append(",")
              .append(v.linkedTaskId).append(",").append(v.fired).append(",").append(v.createdAt).append("\n");
        }
        Files.writeString(Paths.get(filepath), sb.toString());
        System.out.println("Exported " + list.size() + " reminders to " + filepath);
    }

    public void exportSessionsToCsv(String filepath, int maxSessions) throws IOException {
        List<SessionView> list = getSessionViewsBatch(0, maxSessions);
        StringBuilder sb = new StringBuilder();
        sb.append("sessionId,owner,startedAt,closedAt,responseCount\n");
        for (SessionView v : list) {
            sb.append(v.sessionId).append(",").append(v.owner).append(",").append(v.startedAt).append(",")
              .append(v.closedAt).append(",").append(v.responseCount).append("\n");
        }
        Files.writeString(Paths.get(filepath), sb.toString());
        System.out.println("Exported " + list.size() + " sessions to " + filepath);
    }

    public void runExportRemindersCsv(Scanner sc) {
        System.out.print("Output file: ");
        String path = sc.nextLine().trim();
        System.out.print("Max reminders: ");
        int max = Integer.parseInt(sc.nextLine().trim());
        try {
            exportRemindersToCsv(path, max);
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    public void runExportSessionsCsv(Scanner sc) {
        System.out.print("Output file: ");
        String path = sc.nextLine().trim();
        System.out.print("Max sessions: ");
        int max = Integer.parseInt(sc.nextLine().trim());
        try {
            exportSessionsToCsv(path, max);
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    public static final BigInteger DEFAULT_GAS_ENQUEUE_TASK = new BigInteger("180000");
    public static final BigInteger DEFAULT_GAS_COMPLETE_TASK = new BigInteger("80000");
    public static final BigInteger DEFAULT_GAS_CANCEL_TASK = new BigInteger("60000");
    public static final BigInteger DEFAULT_GAS_SET_REMINDER = new BigInteger("120000");
    public static final BigInteger DEFAULT_GAS_CREATE_SESSION = new BigInteger("100000");
    public static final BigInteger DEFAULT_GAS_CLOSE_SESSION = new BigInteger("60000");
    public static final BigInteger DEFAULT_GAS_REGISTER_INTENT = new BigInteger("80000");
    public static final BigInteger DEFAULT_GAS_DEPOSIT = new BigInteger("60000");

    public BigInteger estimateGasEnqueueTask() { return DEFAULT_GAS_ENQUEUE_TASK; }
    public BigInteger estimateGasCompleteTask() { return DEFAULT_GAS_COMPLETE_TASK; }
    public BigInteger estimateGasCancelTask() { return DEFAULT_GAS_CANCEL_TASK; }
    public BigInteger estimateGasSetReminder() { return DEFAULT_GAS_SET_REMINDER; }
    public BigInteger estimateGasCreateSession() { return DEFAULT_GAS_CREATE_SESSION; }
    public BigInteger estimateGasCloseSession() { return DEFAULT_GAS_CLOSE_SESSION; }
    public BigInteger estimateGasRegisterIntent() { return DEFAULT_GAS_REGISTER_INTENT; }
    public BigInteger estimateGasDeposit() { return DEFAULT_GAS_DEPOSIT; }

    public void printGasHints() {
        System.out.println("Gas hints (approx): enqueueTask=" + estimateGasEnqueueTask() + " completeTask=" + estimateGasCompleteTask()
            + " cancelTask=" + estimateGasCancelTask() + " setReminder=" + estimateGasSetReminder()
            + " createSession=" + estimateGasCreateSession() + " closeSession=" + estimateGasCloseSession()
            + " registerIntent=" + estimateGasRegisterIntent() + " deposit=" + estimateGasDeposit());
    }

    public void runGasHints() {
        printGasHints();
    }

    public String getLastTaskId() throws IOException {
        BigInteger len = getTaskIdsLength();
        if (len.compareTo(BigInteger.ZERO) == 0) return null;
        return getTaskIdAt(len.subtract(BigInteger.ONE));
    }

    public String getLastReminderId() throws IOException {
        BigInteger len = getReminderIdsLength();
        if (len.compareTo(BigInteger.ZERO) == 0) return null;
        return getReminderIdAt(len.subtract(BigInteger.ONE));
    }

    public String getLastSessionId() throws IOException {
        BigInteger len = getSessionIdsLength();
        if (len.compareTo(BigInteger.ZERO) == 0) return null;
        return getSessionIdAt(len.subtract(BigInteger.ONE));
    }

    public void runLastIds() {
        try {
            System.out.println("Last task ID: " + getLastTaskId());
            System.out.println("Last reminder ID: " + getLastReminderId());
            System.out.println("Last session ID: " + getLastSessionId());
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    public BigInteger getBalanceOf(String ownerAddr) throws IOException {
        String sel = "0x70a08231";
        String data = sel + padAddress(ownerAddr);
        String raw = ethCall(HARIBA_CONTRACT, data);
        String result = extractResult(raw);
        if (result == null || result.length() < 66) return BigInteger.ZERO;
        return new BigInteger(result.substring(2), 16);
    }

    public void runBalanceOf(Scanner sc) {
        System.out.print("Address (0x...): ");
        String addr = sc.nextLine().trim();
        if (!isValidAddress(addr)) { System.err.println("Invalid address"); return; }
        try {
            BigInteger bal = getBalanceOf(addr);
            System.out.println("Balance: " + bal + " wei (" + weiToEther(bal) + " ether)");
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    public boolean validateTaskIdFormat(String taskIdHex) {
        return isValidBytes32(taskIdHex);
    }

    public boolean validateReminderIdFormat(String reminderIdHex) {
        return isValidBytes32(reminderIdHex);
    }

    public boolean validateSessionIdFormat(String sessionIdHex) {
        return isValidBytes32(sessionIdHex);
    }

    public void runValidateTaskId(Scanner sc) {
        System.out.print("Task ID (0x...): ");
        String id = sc.nextLine().trim();
        System.out.println("Valid format: " + validateTaskIdFormat(id));
    }

    public void runValidateSessionId(Scanner sc) {
        System.out.print("Session ID (0x...): ");
        String id = sc.nextLine().trim();
        System.out.println("Valid format: " + validateSessionIdFormat(id));
    }

    public String formatTaskKindShort(int kind) {
        switch (kind) {
            case TASK_KIND_GENERIC: return "gen";
            case TASK_KIND_CALL: return "call";
            case TASK_KIND_MEETING: return "meet";
            case TASK_KIND_DEADLINE: return "dead";
            default: return "?";
        }
    }

    public String formatStatusShort(int status) {
        switch (status) {
            case TASK_STATUS_PENDING: return "pend";
            case TASK_STATUS_COMPLETED: return "done";
            case TASK_STATUS_CANCELLED: return "cancel";
            default: return "?";
        }
    }

    public void printTaskViewShort(TaskView v) {
        if (v == null) return;
        System.out.println(formatIdShort(v.taskId) + " " + formatAddressShort(v.owner) + " " + formatTaskKindShort(v.kind) + " " + formatStatusShort(v.status) + " due=" + v.dueAt);
    }

    public void runListTasksShort(int offset, int limit) {
        try {
            List<TaskView> list = getTaskViewsBatch(offset, limit);
            for (TaskView v : list) printTaskViewShort(v);
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    public long getServerTimeFromBlock() throws IOException {
        String body = "{\"jsonrpc\":\"2.0\",\"method\":\"eth_getBlockByNumber\",\"params\":[\"latest\",false],\"id\":1}";
        String raw = postJson(rpcUrl, body);
        if (raw == null) return 0;
        int i = raw.indexOf("\"timestamp\":\"0x");
        if (i < 0) return 0;
        i += 15;
        int j = raw.indexOf("\"", i);
        if (j < 0) return 0;
        String hex = raw.substring(i, j);
        return new BigInteger(hex, 16).longValue();
    }

    public void runServerTime() {
        try {
            long t = getServerTimeFromBlock();
            System.out.println("Chain block timestamp (approx): " + t + " (" + new Date(t * 1000L) + ")");
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    public List<TaskView> getTasksDueBefore(BigInteger timestamp, int maxCount) throws IOException {
        List<TaskView> all = getTaskViewsBatch(0, maxCount * 2);
        return all.stream().filter(v -> v.dueAt != null && v.dueAt.compareTo(timestamp) <= 0 && v.status == TASK_STATUS_PENDING).limit(maxCount).collect(Collectors.toList());
    }

    public void runTasksDueBefore(Scanner sc) {
        System.out.print("Timestamp: ");
        BigInteger ts = new BigInteger(sc.nextLine().trim());
        try {
            List<TaskView> list = getTasksDueBefore(ts, 32);
            System.out.println("Tasks due before " + ts + ": " + list.size());
            for (TaskView v : list) printTaskView(v);
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    public int countPendingTasks() throws IOException {
        List<TaskView> all = getTaskViewsBatch(0, 256);
        return (int) all.stream().filter(v -> v.status == TASK_STATUS_PENDING).count();
    }

    public int countUnfiredReminders() throws IOException {
        List<ReminderView> all = getReminderViewsBatch(0, 256);
        return (int) all.stream().filter(v -> !v.fired).count();
    }

    public void runCounts() {
        try {
            System.out.println("Pending tasks (sample): " + countPendingTasks());
            System.out.println("Unfired reminders (sample): " + countUnfiredReminders());
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    public String getContractAddress() {
        return HARIBA_CONTRACT;
    }

    public String getStewardAddress() { return HRB_STEWARD; }
    public String getVaultAddress() { return HRB_VAULT; }
    public String getOracleAddress() { return HRB_ORACLE; }
    public String getRelayAddress() { return HRB_RELAY; }
    public String getKeeperAddress() { return HRB_KEEPER; }
    public String getCuratorAddress() { return HRB_CURATOR; }
    public String getSentinelAddress() { return HRB_SENTINEL; }

    public void printAllRoleAddresses() {
        System.out.println("Steward:  " + HRB_STEWARD);
        System.out.println("Vault:    " + HRB_VAULT);
        System.out.println("Oracle:   " + HRB_ORACLE);
        System.out.println("Relay:    " + HRB_RELAY);
        System.out.println("Keeper:   " + HRB_KEEPER);
        System.out.println("Curator:  " + HRB_CURATOR);
        System.out.println("Sentinel: " + HRB_SENTINEL);
    }

    public void runPrintRoles() {
        printAllRoleAddresses();
    }

    public BigInteger getTaskIdsLengthSafe() {
        try {
            return getTaskIdsLength();
        } catch (IOException e) {
            return BigInteger.ZERO;
        }
    }

    public BigInteger getReminderIdsLengthSafe() {
        try {
            return getReminderIdsLength();
        } catch (IOException e) {
            return BigInteger.ZERO;
        }
    }

    public BigInteger getSessionIdsLengthSafe() {
        try {
            return getSessionIdsLength();
        } catch (IOException e) {
            return BigInteger.ZERO;
        }
    }

    public void runQuickSummary() {
        try {
            BigInteger tasks = getTaskIdsLength();
            BigInteger reminders = getReminderIdsLength();
            BigInteger sessions = getSessionIdsLength();
            BigInteger intents = getIntentIdsLength();
            boolean paused = isPaused();
            BigInteger fee = getFeeWei();
            System.out.println("Tasks=" + tasks + " Reminders=" + reminders + " Sessions=" + sessions + " Intents=" + intents + " Paused=" + paused + " FeeWei=" + fee);
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    public List<TaskView> getTasksByKind(int kind, int offset, int limit) throws IOException {
        List<TaskView> all = getTaskViewsBatch(offset, limit * 2);
        return all.stream().filter(v -> v.kind == kind).limit(limit).collect(Collectors.toList());
    }

    public void runListTasksByKind(Scanner sc) {
        System.out.print("Kind (0=generic 1=call 2=meeting 3=deadline): ");
        int kind = Integer.parseInt(sc.nextLine().trim());
        try {
            List<TaskView> list = getTasksByKind(kind, 0, 32);
            System.out.println("Tasks kind=" + kind + ": " + list.size());
            for (TaskView v : list) printTaskView(v);
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    public List<ReminderView> getRemindersTriggerBefore(BigInteger timestamp, int maxCount) throws IOException {
        List<ReminderView> all = getReminderViewsBatch(0, maxCount * 2);
        return all.stream().filter(v -> !v.fired && v.triggerAt != null && v.triggerAt.compareTo(timestamp) <= 0).limit(maxCount).collect(Collectors.toList());
    }

    public void runRemindersTriggerBefore(Scanner sc) {
        System.out.print("Timestamp: ");
        BigInteger ts = new BigInteger(sc.nextLine().trim());
        try {
            List<ReminderView> list = getRemindersTriggerBefore(ts, 32);
            System.out.println("Unfired reminders with triggerAt <= " + ts + ": " + list.size());
            for (ReminderView v : list) printReminderView(v);
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    public static String formatTimestamp(BigInteger ts) {
        if (ts == null) return "null";
        try {
            long t = ts.longValueExact();
            if (t > 0 && t < 1L << 62) return new Date(t * 1000L).toString();
        } catch (Exception e) {}
        return ts.toString();
    }

    public void printTaskViewWithTime(TaskView v) {
        if (v == null) return;
        System.out.printf("  %s owner=%s kind=%s status=%s due=%s (%s) created=%s (%s)%n",
            formatIdShort(v.taskId), formatAddressShort(v.owner), taskKindName(v.kind), taskStatusName(v.status),
            v.dueAt, formatTimestamp(v.dueAt), v.createdAt, formatTimestamp(v.createdAt));
    }

    public void runListTasksWithTime(int offset, int limit) {
        try {
            List<TaskView> list = getTaskViewsBatch(offset, limit);
            for (TaskView v : list) printTaskViewWithTime(v);
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    public void runCompleteTaskInteractive(Scanner sc) {
        System.out.print("Task ID (0x...): ");
        String id = sc.nextLine().trim();
        String data = buildCompleteTaskData(id);
        System.out.println("Calldata: " + data);
        if (!hasPrivateKey()) System.out.println("Set private key to send transaction.");
    }

    public void runCancelTaskInteractive(Scanner sc) {
        System.out.print("Task ID (0x...): ");
        String id = sc.nextLine().trim();
        String data = buildCancelTaskData(id);
        System.out.println("Calldata: " + data);
        if (!hasPrivateKey()) System.out.println("Set private key to send transaction.");
    }

    public void runSubmitFeedbackInteractive(Scanner sc) {
        System.out.print("Ref ID (bytes32 hex 0x...): ");
        String refId = sc.nextLine().trim();
        System.out.print("Rating (1-5): ");
        int rating = Integer.parseInt(sc.nextLine().trim());
        String data = buildSubmitFeedbackData(refId, rating);
        System.out.println("Calldata: " + data.substring(0, Math.min(80, data.length())) + "...");
    }

    public void runStorePreferenceInteractive(Scanner sc) {
        System.out.print("Preference key (string, will be hashed): ");
        String key = sc.nextLine().trim();
        System.out.print("Value (hex or text): ");
        String valStr = sc.nextLine().trim();
        byte[] value = valStr.getBytes(StandardCharsets.UTF_8);
        String keyHash = prefKeyHash(key);
        String data = buildStorePreferenceData(keyHash, value);
        System.out.println("Key hash: " + keyHash);
        System.out.println("Calldata length: " + data.length() / 2 + " bytes");
    }

    public BigInteger getVaultBalanceSafe() {
        try {
            return getVaultBalance();
        } catch (IOException e) {
            return BigInteger.ZERO;
        }
    }

    public boolean isPausedSafe() {
        try {
            return isPaused();
        } catch (IOException e) {
            return true;
        }
    }

    public void runVaultBalance() {
        try {
            BigInteger bal = getVaultBalance();
            System.out.println("Vault balance: " + bal + " wei (" + weiToEther(bal) + " ether)");
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    public void runDeployBlock() {
        try {
            BigInteger block = getDeployBlock();
            System.out.println("Deploy block: " + block);
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    public int getIntentIdsLengthInt() throws IOException {
        return getIntentIdsLength().min(BigInteger.valueOf(Integer.MAX_VALUE)).intValue();
    }

    public List<IntentView> getAllIntentViews(int max) throws IOException {
        return getIntentViewsBatch(0, max);
    }

    public void runListIntents(int limit) {
        try {
            List<IntentView> list = getAllIntentViews(limit);
            System.out.println("Intents: " + list.size());
            for (IntentView v : list) {
                System.out.println("  " + formatIdShort(v.intentId) + " owner=" + formatAddressShort(v.owner) + " type=" + v.intentType + " createdAt=" + v.createdAt);
            }
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        HaZZa app = new HaZZa();
        if (args.length > 0 && "--rpc".equals(args[0]) && args.length > 1) {
            app.setRpcUrl(args[1]);
        }
        Scanner sc = new Scanner(System.in);
        app.loadConfig();
        loop: for (;;) {
            System.out.println("\n--- HaZZa PA Client (Hariba) ---");
            System.out.println("1. List tasks");
            System.out.println("2. List reminders");
            System.out.println("3. List sessions");
            System.out.println("4. List intents");
            System.out.println("5. Platform stats");
            System.out.println("6. Config (fee, limits)");
            System.out.println("7. Get task by ID");
            System.out.println("8. Get reminder by ID");
            System.out.println("9. Get session by ID");
            System.out.println("10. Enqueue task (build calldata)");
            System.out.println("11. Set reminder (build calldata)");
            System.out.println("12. Create session (build calldata)");
            System.out.println("13. Close session (build calldata)");
            System.out.println("14. Register intent (build calldata)");
            System.out.println("15. Deposit (build calldata)");
            System.out.println("16. Fee info");
            System.out.println("17. Set RPC URL");
            System.out.println("18. Contract info");
            System.out.println("19. Pending tasks only");
            System.out.println("20. Snapshot (tasks + reminders)");
            System.out.println("21. Export tasks CSV");
            System.out.println("22. Would enqueue succeed?");
            System.out.println("23. Try fallback RPC");
            System.out.println("24. Tasks by owner");
            System.out.println("25. Reminders by owner");
            System.out.println("26. Sessions by owner");
            System.out.println("27. Export reminders CSV");
            System.out.println("28. Export sessions CSV");
            System.out.println("29. Gas hints");
            System.out.println("30. Last task/reminder/session IDs");
            System.out.println("31. Balance of address");
            System.out.println("32. Validate task/session ID format");
            System.out.println("33. List tasks (short)");
            System.out.println("34. Chain timestamp");
            System.out.println("35. Tasks due before timestamp");
            System.out.println("36. Pending/unfired counts");
            System.out.println("37. Quick summary");
            System.out.println("38. Tasks by kind");
            System.out.println("39. Reminders trigger before timestamp");
            System.out.println("40. List tasks with human time");
            System.out.println("41. Complete task (build calldata)");
            System.out.println("42. Cancel task (build calldata)");
            System.out.println("43. Submit feedback (build calldata)");
            System.out.println("44. Store preference (build calldata)");
            System.out.println("45. Print all role addresses");
            System.out.println("0. Quit");
            System.out.print("Choice: ");
            String line = sc.nextLine().trim();
            int choice = line.isEmpty() ? -1 : Integer.parseInt(line);
            switch (choice) {
                case 0: break loop;
                case 1: app.runListTasks(0, 32); break;
                case 2: app.runListReminders(0, 32); break;
                case 3: app.runListSessions(0, 32); break;
                case 4:
                    try {
                        List<IntentView> list = app.getIntentViewsBatch(0, 32);
                        System.out.println("Intents: " + list.size());
                        for (IntentView v : list) System.out.println("  " + v.intentId + " owner=" + app.formatAddressShort(v.owner) + " type=" + v.intentType);
                    } catch (IOException e) { System.err.println("Error: " + e.getMessage()); }
                    break;
                case 5: app.runPlatformStats(); break;
                case 6: app.runConfig(); break;
                case 7: app.runGetTask(sc); break;
                case 8: app.runGetReminder(sc); break;
                case 9: app.runGetSession(sc); break;
                case 10: app.runEnqueueTaskInteractive(sc); break;
                case 11: app.runSetReminderInteractive(sc); break;
                case 12: app.runCreateSessionInteractive(sc); break;
                case 13: app.runCloseSessionInteractive(sc); break;
                case 14: app.runRegisterIntentInteractive(sc); break;
                case 15: app.runDepositInteractive(sc); break;
                case 16: app.runFeeInfo(); break;
                case 17:
                    System.out.print("RPC URL: ");
                    app.setRpcUrl(sc.nextLine().trim());
                    app.saveConfig();
                    break;
                case 18: app.printContractInfo(); break;
                case 19:
                    try {
                        List<TaskView> list = app.getPendingTasksOnly(0, 32);
                        System.out.println("Pending tasks: " + list.size());
                        for (TaskView v : list) app.printTaskView(v);
                    } catch (IOException e) { System.err.println("Error: " + e.getMessage()); }
                    break;
                case 20: app.runSnapshot(); break;
                case 21: app.runExportTasksCsv(sc); break;
                case 22: app.runWouldEnqueueSucceed(sc); break;
                case 23: app.tryFallbackRpc(); break;
                case 24: app.runListTasksByOwner(sc); break;
                case 25: app.runListRemindersByOwner(sc); break;
                case 26: app.runListSessionsByOwner(sc); break;
                case 27: app.runExportRemindersCsv(sc); break;
                case 28: app.runExportSessionsCsv(sc); break;
                case 29: app.runGasHints(); break;
                case 30: app.runLastIds(); break;
                case 31: app.runBalanceOf(sc); break;
                case 32: app.runValidateTaskId(sc); break;
                case 33: app.runListTasksShort(0, 32); break;
                case 34: app.runServerTime(); break;
                case 35: app.runTasksDueBefore(sc); break;
                case 36: app.runCounts(); break;
                case 37: app.runQuickSummary(); break;
                case 38: app.runListTasksByKind(sc); break;
                case 39: app.runRemindersTriggerBefore(sc); break;
                case 40: app.runListTasksWithTime(0, 32); break;
                case 41: app.runCompleteTaskInteractive(sc); break;
                case 42: app.runCancelTaskInteractive(sc); break;
                case 43: app.runSubmitFeedbackInteractive(sc); break;
                case 44: app.runStorePreferenceInteractive(sc); break;
                case 45: app.runPrintRoles(); break;
                default: System.out.println("Unknown option.");
            }
        }
        app.saveConfig();
        sc.close();
        System.out.println("HaZZa done.");
    }
}
