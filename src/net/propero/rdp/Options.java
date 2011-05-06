/* Options.java
 * Component: ProperJavaRDP
 * 
 * Revision: $Revision: 1.7 $
 * Author: $Author: telliott $
 * Date: $Date: 2005/09/27 14:15:39 $
 *
 * Copyright (c) 2005 Propero Limited
 *
 * Purpose: Global static storage of user-definable options
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or (at
 * your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA
 * 
 * (See gpl.txt for details of the GNU General Public License.)
 * 
 */
package net.propero.rdp;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.awt.image.DirectColorModel;
import java.io.File;
import java.util.prefs.Preferences;

@SuppressWarnings({"UnusedDeclaration"})
public class Options {

    static Logger logger = Logger.getLogger(Options.class);

    static {
        logger.setLevel(Level.DEBUG);
    }

    private static String ourNodeName = "/net/propero/rdp";
    private static Preferences preferences = Preferences.userRoot().node(ourNodeName);

    public static final int DIRECT_BITMAP_DECOMPRESSION = 0;
    public static final int BUFFERED_IMAGE_BITMAP_DECOMPRESSION = 1;
    public static final int INTEGER_BITMAP_DECOMPRESSION = 2;

    private static final String SERVER_BPP = "server_bpp";
    private static final String LOW_LATENCY = "low_latency";
    private static final String KEY_LAYOUT = "key_layout";
    private static final String BITMAP_DECOMPRESSION_STORE = "bitmap_decompression_store";

    private static final String USER_NAME = "user_name";
    private static final String DOMAIN = "domain";
    private static final String PASSWORD = "password";
    private static final String HOSTNAME = "hostname";
    private static final String PORT = "port";

    private static final String COMMAND = "command";
    private static final String DIRECTORY = "directory";
    private static final String WINDOW_TITLE = "window_title";

    private static final String WIDTH = "width";
    private static final String HEIGHT = "height";
    private static final String FULL_SCREEN = "full_screen";

    private static final String BUILT_IN_LICENCE = "build_in_licence";
    private static final String LOAD_LICENCE = "load_licence";
    private static final String SAVE_LICENCE = "save_licence";
    private static final String LICENCE_PATH = "licence_path";

    private static final String DEBUG_KEYBOARD = "debug_keyboard";
    private static final String DEBUG_HEX = "debug_hex";

    private static final String ENABLE_MENU = "enable_menu";

    private static final String ALT_KEY_QUITE = "alt_key_quite";
    private static final String CAPS_SEND_UP_AND_DOWN = "caps_send_up_and_down";
    private static final String REMAP_HASH = "remap_hash";
    private static final String USE_LOCKING_KEY_STATE = "use_locking_key_state";

    private static final String USE_RDP5 = "use_rdp5";

    private static final String BPP_KEY = "bpp";              // Bytes per pixel
    private static final String BPP_MASK_KEY = "bpp_mask";    // Correction value to ensure only the relevant
    private static final String IMAGE_COUNT_KEY = "image_count";
    private static final String COLOR_MODEL_KEY = "color_model";
    private static final String SERVER_RDP_VERISON = "server_rdp_version";
    private static final String BITMAP_COMPRESSION = "bitmap_compression";
    private static final String PERSISTENT_BITMAP_CACHING = "persistent_bitmap_caching";
    private static final String BITMAP_CACHING = "bitmap_cashing";
    private static final String PRE_CACHE_BITMAPS = "pre_cache_bitmaps";
    private static final String POLYGON_ELLIPSE_ORDERS = "polygon_ellipse_orders";

    private static final String ORDERS = "orders";
    private static final String ENCRYPTION = "encryption";
    private static final String PACKET_ENCRYPTION = "packet_encryption";

    private static final String CONSOLE_SESSION = "console_session";

    private static final String USE_SSL = "use_ssl";
    private static final String MAP_CLIPBOARD = "map_clipboard";
    private static final String RDP5_PERFORMANCE_FLAGS = "rdp5_performance_flags";
    private static final String SAVE_GRAPHICS = "save_graphics";

    private static final String STARTUP_COMMAND = "startup_command";

    private static final String LOCAL_TIMEZONE = "x";
    private static final String LOCAL_TIMEZONE_DAYLIGHT_SAVINGS = "y";

    private final static boolean DEFAULT_USE_RDP5 = true;
    private final static int DEFAULT_SERVER_BPP = 24;                         // Bits per pixel
    private final static int DEFAULT_IMAGE_COUNT = 0;
    private final static int DEFAULT_WIDTH = 800;     // -g widthxheight
    private final static int DEFAULT_HEIGHT = 600;    // -g widthxheight
    private final static boolean DEFAULT_FULL_SCREEN = false;
    private final static int DEFAULT_PORT = 3389; // -t port
    private final static boolean DEFAULT_BUILT_IN_LICENSE = false;

    private final static boolean DEFAULT_LOW_LATENCY = true;   // disables bandwidth saving tcp packets
    private final static int DEFAULT_KEY_LAYOUT = 0x809;       // UK by default
    private final static String DEFAULT_USER_NAME = "root";    // -u username
    private final static String DEFAULT_DOMAIN = "";          // -d domain
    private final static String DEFAULT_PASSWORD = "";        // -p password
    private final static String DEFAULT_HOSTNAME = "";        // -n hostname
    private final static String DEFAULT_COMMAND = "";         // -s command
    private final static String DEFAULT_DIRECTORY = "";       // -d directory
    private final static String DEFAULT_WINDOW_TITLE = "properJavaRDP"; // -T windowTitle


    private final static boolean DEFAULT_LOAD_LICENSE = false;
    private final static boolean DEFAULT_SAVE_LICENSE = false;

    private final static String DEFAULT_LICENSE_PATH = System.getProperty("user.home") + File.separator + "RDP_Licences";

    private final static boolean DEFAULT_DEBUG_KEYBOARD = false;
    private final static boolean DEFAULT_DEBUG_HEX_DUMP = false;

    private final static boolean DEFAULT_ENABLE_MENU = true;

    private final static boolean DEFAULT_ALT_KEY_QUIET = false;
    private final static boolean DEFAULT_CAPS_SEND_UP_AND_DOWN = true;
    private final static boolean DEFAULT_REMAP_HASH = true;
    private final static boolean DEFAULT_USE_LOCKING_KEY_STATE = true;

    private final static boolean DEFAULT_BITMAP_COMPRESSION = true;
    private final static boolean DEFAULT_PERSISTENT_BITMAP_CACHING = false;
    private final static boolean DEFAULT_BITMAP_CACHING = true;
    private final static boolean DEFAULT_PRE_CACHE_BITMAPS = true;
    private final static boolean DEFAULT_POLYGON_ELLIPSE_ORDERS = false;

    private final static boolean DEFAULT_ORDERS = true;
    private final static boolean DEFAULT_ENCRYPTION = true;
    private final static boolean DEFAULT_PACKET_ENCRYPTION = true;

    private final static boolean DEFAULT_CONSOLE_SESSION = false;

    private final static int DEFAULT_SERVER_RDP_VERSION = 5;
    private final static boolean DEFAULT_USE_SSL = false;
    private final static boolean DEFAULT_MAP_CLIPBOARD = true;
    private final static int DEFAULT_RDP5_PERFORMANCE_FLAGS =
            Rdp.PERF_DISABLE_CURSOR_SHADOW |
                    Rdp.PERF_DISABLE_CURSORSETTINGS |
                    Rdp.PERF_DISABLE_FULLWINDOWDRAG |
                    Rdp.PERF_DISABLE_MENUANIMATIONS |
                    Rdp.PERF_DISABLE_THEMING |
                    Rdp.PERF_DISABLE_WALLPAPER;
    private final static boolean DEFAULT_SAVE_GRAPHICS = false;

    private final static String DEFAULT_STARTUP_COMMAND = "C:\\Program Files\\...";

    // TimeZone defaults
    private final static String DEFAULT_LOCAL_TIMEZONE = "PST, Pacific Standard Time";
    private final static String DEFAULT_LOCAL_TIMEZONE_DAYLIGHT_SAVINGS = "PDT, Pacific Daylight Time";

    private static DirectColorModel colorModel = new DirectColorModel(24, 0x00FF0000, 0x0000FF00, 0x000000FF);
    private static int bitmapDecompressionStore = INTEGER_BITMAP_DECOMPRESSION;


    /**
     * Set a new value for the server's bits per pixel
     *
     * @param serverBpp New bpp value
     */
    public static void setBpp(int serverBpp) {
        int bpp = (serverBpp + 7) / 8;
        Options.setServerBpp(bpp);

        if (bpp == 8) {
            setBppMask(0x000000FF);
        } else {
            setBppMask(0x00FFFFFF);
        }

        colorModel = new DirectColorModel(24, 0x00FF0000, 0x0000FF00, 0x000000FF); // 24 bit R G B model
    }

    // Accesser functions follow

    public static int getServerBpp() {
        int result;

        result = preferences.getInt(SERVER_BPP, DEFAULT_SERVER_BPP);
        result = Math.max(result, 16);
        
        return result;
    }

    public static void setServerBpp(int bpp) {
        preferences.putInt(SERVER_BPP, bpp);
    }

    public static int getBpp() {
        return preferences.getInt(BPP_KEY, DEFAULT_SERVER_BPP);
    }

    @SuppressWarnings({"ShiftOutOfRange"})
    public static int getBppMask() {
        int defaultBppMask = 0x00FFFFFF >> 8 * (3 - DEFAULT_SERVER_BPP);

        return preferences.getInt(BPP_MASK_KEY, defaultBppMask);
    }

    public static void setBppMask(int bppMask) {
        preferences.putInt(BPP_MASK_KEY, bppMask);
    }

    public static int getImgCountAndIncrement() {
        int newImageCount = getImgCount();
        setImgCount(newImageCount + 1);
        return newImageCount;
    }

    public static int getImgCount() {
        return preferences.getInt(IMAGE_COUNT_KEY, DEFAULT_IMAGE_COUNT);
    }

    public static void setImgCount(int imgCount) {
        preferences.putInt(IMAGE_COUNT_KEY, imgCount);
    }

    public static boolean isRdp5() {
        return preferences.getBoolean(USE_RDP5, DEFAULT_USE_RDP5);
    }

    public static void setUseRdp5(boolean useRdp5) {
        preferences.putBoolean(USE_RDP5, useRdp5);
    }

    public static String getLicencePath() {
        return preferences.get(LICENCE_PATH, DEFAULT_LICENSE_PATH);
    }

    public static void setLicencePath(String licencePath) {
        preferences.put(LICENCE_PATH, licencePath);
    }

    public static int getWidth() {
        return preferences.getInt(WIDTH, DEFAULT_WIDTH);
    }

    public static void setWidth(int width) {
        preferences.putInt(WIDTH, width);
    }

    public static int getHeight() {
        return preferences.getInt(HEIGHT, DEFAULT_HEIGHT);
    }

    public static void setHeight(int height) {
        preferences.putInt(HEIGHT, height);
    }

    public static int getPort() {
        return preferences.getInt(PORT, DEFAULT_PORT);
    }

    public static void setPort(int port) {
        preferences.putInt(PORT, port);
    }

    public static boolean isFullScreen() {
        return preferences.getBoolean(FULL_SCREEN, DEFAULT_FULL_SCREEN);
    }

    public static void setFullScreen(boolean fullScreen) {
        preferences.putBoolean(FULL_SCREEN, fullScreen);
    }

    public static boolean isBuiltInLicence() {
        return preferences.getBoolean(BUILT_IN_LICENCE, DEFAULT_BUILT_IN_LICENSE);
    }

    public static void setBuiltInLicence(boolean builtInLicence) {
        preferences.putBoolean(BUILT_IN_LICENCE, builtInLicence);
    }

    public static boolean isLowLatency() {
        return preferences.getBoolean(LOW_LATENCY, DEFAULT_LOW_LATENCY);
    }

    public static void setLowLatency(boolean lowLatency) {
        preferences.putBoolean(LOW_LATENCY, lowLatency);
    }

    public static int getKeyLayout() {
        return preferences.getInt(KEY_LAYOUT, DEFAULT_KEY_LAYOUT);
    }

    public static void setKeyLayout(int keyLayout) {
        preferences.putInt(KEY_LAYOUT, keyLayout);
    }

    public static String getUserName() {
        return preferences.get(USER_NAME, DEFAULT_USER_NAME);
    }

    public static void setUserName(String userName) {
        preferences.put(USER_NAME, userName);
    }

    public static String getDomain() {
        return preferences.get(DOMAIN, DEFAULT_DOMAIN);
    }

    public static void setDomain(String domain) {
        preferences.put(DOMAIN, domain);
    }

    public static String getPassword() {
        return preferences.get(PASSWORD, DEFAULT_PASSWORD);
    }

    public static void setPassword(String password) {
        preferences.put(PASSWORD, password);
    }

    public static String getHostname() {
        return preferences.get(HOSTNAME, DEFAULT_HOSTNAME);
    }

    public static void setHostname(String hostname) {
        preferences.put(HOSTNAME, hostname);
    }

    public static String getCommand() {
        return preferences.get(COMMAND, DEFAULT_COMMAND);
    }

    public static void setCommand(String command) {
        preferences.put(COMMAND, command);
    }

    public static String getDirectory() {
        return preferences.get(DIRECTORY, DEFAULT_DIRECTORY);
    }

    public static void setDirectory(String directory) {
        preferences.put(DIRECTORY, directory);
    }

    public static String getWindowTitle() {
        return preferences.get(WINDOW_TITLE, DEFAULT_WINDOW_TITLE);
    }

    public static void setWindowTitle(String windowTitle) {
        preferences.put(WINDOW_TITLE, windowTitle);
    }

    public static boolean isDebugKeyboard() {
        return preferences.getBoolean(DEBUG_KEYBOARD, DEFAULT_DEBUG_KEYBOARD);
    }

    public static void setDebugKeyboard(boolean debugKeyboard) {
        preferences.putBoolean(DEBUG_KEYBOARD, debugKeyboard);
    }

    public static boolean isDebugHexDump() {
        return preferences.getBoolean(DEBUG_HEX, DEFAULT_DEBUG_HEX_DUMP);
    }

    public static void setDebugHexDump(boolean debugHexDump) {
        preferences.putBoolean(DEBUG_HEX, debugHexDump);
    }

    public static boolean isEnableMenu() {
        return preferences.getBoolean(ENABLE_MENU, DEFAULT_ENABLE_MENU);
    }

    public static void setEnableMenu(boolean enableMenu) {
        preferences.putBoolean(ENABLE_MENU, enableMenu);
    }

    public static boolean isAltKeyQuite() {
        return preferences.getBoolean(ALT_KEY_QUITE, DEFAULT_ALT_KEY_QUIET);
    }

    public static void setAltKeyQuite(boolean altKeyQuite) {
        preferences.putBoolean(ALT_KEY_QUITE, altKeyQuite);
    }

    public static boolean isCapsSendsUpAndDown() {
        return preferences.getBoolean(CAPS_SEND_UP_AND_DOWN, DEFAULT_CAPS_SEND_UP_AND_DOWN);
    }

    public static void setCapsSendsUpAndDown(boolean capsSendsUpAndDown) {
        preferences.putBoolean(CAPS_SEND_UP_AND_DOWN, capsSendsUpAndDown);
    }

    public static boolean isRemapHash() {
        return preferences.getBoolean(REMAP_HASH, DEFAULT_REMAP_HASH);
    }

    public static void setRemapHash(boolean remapHash) {
        preferences.putBoolean(REMAP_HASH, remapHash);
    }

    public static boolean isUseLockingKeyState() {
        return preferences.getBoolean(USE_LOCKING_KEY_STATE, DEFAULT_USE_LOCKING_KEY_STATE);
    }

    public static void setUseLockingKeyState(boolean useLockingKeyState) {
        preferences.putBoolean(USE_LOCKING_KEY_STATE, useLockingKeyState);
    }

    public static DirectColorModel getColorModel() {
        return colorModel;
    }

    public static void setColorModel(DirectColorModel colorModel) {
        Options.colorModel = colorModel;
    }

    public static int getServerRDPVersion() {
        return preferences.getInt(SERVER_RDP_VERISON, DEFAULT_SERVER_RDP_VERSION);
    }

    public static void setServerRDPVersion(int serverRDPVersion) {
        preferences.putInt(SERVER_RDP_VERISON, serverRDPVersion);
    }

    public static boolean isLoadLicence() {
        return preferences.getBoolean(LOAD_LICENCE, DEFAULT_LOAD_LICENSE);
    }

    public static void setLoadLicence(boolean loadLicence) {
        preferences.putBoolean(LOAD_LICENCE, loadLicence);
    }

    public static boolean isSaveLicence() {
        return preferences.getBoolean(SAVE_LICENCE, DEFAULT_SAVE_LICENSE);
    }

    public static void setSaveLicence(boolean saveLicence) {
        preferences.putBoolean(SAVE_LICENCE, saveLicence);
    }

    public static int getBitmapDecompressionStore() {
        return preferences.getInt(BITMAP_DECOMPRESSION_STORE, bitmapDecompressionStore);
    }

    public static void setBitmapDecompressionStore(int bitmapDecompressionStore) {
        preferences.putInt(BITMAP_DECOMPRESSION_STORE, bitmapDecompressionStore);
        Options.bitmapDecompressionStore = bitmapDecompressionStore;
    }

    public static boolean isBitmapCompression() {
        return preferences.getBoolean(BITMAP_COMPRESSION, DEFAULT_BITMAP_COMPRESSION);
    }

    public static void setBitmapCompression(boolean bitmapCompression) {
        preferences.putBoolean(BITMAP_COMPRESSION, bitmapCompression);
    }

    public static boolean isPersistentBitmapCaching() {
        return preferences.getBoolean(PERSISTENT_BITMAP_CACHING, DEFAULT_PERSISTENT_BITMAP_CACHING);
    }

    public static void setPersistentBitmapCaching(boolean persistentBitmapCaching) {
        preferences.putBoolean(PERSISTENT_BITMAP_CACHING, persistentBitmapCaching);
    }

    public static boolean isBitmapCaching() {
        return preferences.getBoolean(BITMAP_CACHING, DEFAULT_BITMAP_CACHING);
    }

    public static void setBitmapCaching(boolean bitmapCaching) {
        preferences.putBoolean(BITMAP_CACHING, bitmapCaching);
    }

    public static boolean isPreCacheBitmaps() {
        return preferences.getBoolean(PRE_CACHE_BITMAPS, DEFAULT_PRE_CACHE_BITMAPS);
    }

    public static void setPreCacheBitmaps(boolean preCacheBitmaps) {
        preferences.putBoolean(PRE_CACHE_BITMAPS, preCacheBitmaps);
    }

    public static boolean isPolygonEllipseOrders() {
        return preferences.getBoolean(POLYGON_ELLIPSE_ORDERS, DEFAULT_POLYGON_ELLIPSE_ORDERS);
    }

    public static void setPolygonEllipseOrders(boolean polygonEllipseOrders) {
        preferences.putBoolean(POLYGON_ELLIPSE_ORDERS, polygonEllipseOrders);
    }

    public static boolean isOrders() {
        return preferences.getBoolean(ORDERS, DEFAULT_ORDERS);
    }

    public static void setOrders(boolean orders) {
        preferences.putBoolean(ORDERS, orders);
    }

    public static boolean isEncryption() {
        return preferences.getBoolean(ENCRYPTION, DEFAULT_ENCRYPTION);
    }

    public static void setEncryption(boolean encryption) {
        preferences.putBoolean(ENCRYPTION, encryption);
    }

    public static boolean isPacketEncryption() {
        return preferences.getBoolean(PACKET_ENCRYPTION, DEFAULT_PACKET_ENCRYPTION);
    }

    public static void setPacketEncryption(boolean packetEncryption) {
        preferences.putBoolean(PACKET_ENCRYPTION, packetEncryption);
    }

    public static boolean isConsoleSession() {
        return preferences.getBoolean(CONSOLE_SESSION, DEFAULT_CONSOLE_SESSION);
    }

    public static void setConsoleSession(boolean consoleSession) {
        preferences.putBoolean(CONSOLE_SESSION, consoleSession);
    }

    public static boolean isUseSsl() {
        return preferences.getBoolean(USE_SSL, DEFAULT_USE_SSL);
    }

    public static void setUseSsl(boolean useSsl) {
        preferences.putBoolean(USE_SSL, useSsl);
    }

    public static boolean isMapClipboard() {
        return preferences.getBoolean(MAP_CLIPBOARD, DEFAULT_MAP_CLIPBOARD);
    }

    public static void setMapClipboard(boolean mapClipboard) {
        preferences.putBoolean(MAP_CLIPBOARD, mapClipboard);
    }

    public static int getRdp5PerformanceFlags() {
        return preferences.getInt(RDP5_PERFORMANCE_FLAGS, DEFAULT_RDP5_PERFORMANCE_FLAGS);
    }

    public static void setRdp5PerformanceFlags(int rdp5PerformanceFlags) {
        preferences.putInt(RDP5_PERFORMANCE_FLAGS, rdp5PerformanceFlags);
    }

    public static boolean isSave_graphics() {
        return preferences.getBoolean(SAVE_GRAPHICS, DEFAULT_SAVE_GRAPHICS);
    }

    public static void setSave_graphics(boolean save_graphics) {
        preferences.putBoolean(SAVE_GRAPHICS, save_graphics);
    }

    public static String getStartupCommand() {
        return preferences.get(STARTUP_COMMAND, DEFAULT_STARTUP_COMMAND);
    }

    public static void setStartupCommand(String startupCommand) {
        preferences.put(STARTUP_COMMAND, startupCommand);
    }

    public static String getTimezoneName() {
        return preferences.get(LOCAL_TIMEZONE, DEFAULT_LOCAL_TIMEZONE);
    }

    public static void setTimezoneName(String timezoneName) {
        preferences.put(LOCAL_TIMEZONE, timezoneName);
    }

    public static String getTimezoneDaylightSavingsName() {
        return preferences.get(LOCAL_TIMEZONE_DAYLIGHT_SAVINGS, DEFAULT_LOCAL_TIMEZONE_DAYLIGHT_SAVINGS);
    }

    public static void setTimezoneDaylightSavingsName(String timeZoneName) {
        preferences.put(LOCAL_TIMEZONE_DAYLIGHT_SAVINGS, timeZoneName);
    }
}
