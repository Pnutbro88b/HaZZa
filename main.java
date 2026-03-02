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
