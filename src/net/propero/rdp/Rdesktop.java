/* Rdesktop.java
 * Component: ProperJavaRDP
 * 
 * Revision: $Revision: 1.7 $
 * Author: $Author: telliott $
 * Date: $Date: 2005/09/27 14:15:39 $
 *
 * Copyright (c) 2005 Propero Limited
 *
 * Purpose: Main class, launches session
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

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;
import net.propero.rdp.keymapping.KeyCode_FileBased;
import net.propero.rdp.tools.SendEvent;
import net.propero.rdp.virtualChannels.Rdp5;
import net.propero.rdp.virtualChannels.VChannels;
import net.propero.rdp.virtualChannels.cliprdr.ClipChannel;
import net.propero.rdp.virtualChannels.rdpEFS.RdpefsChannel;
import net.propero.rdp.virtualChannels.rdpSoundOut.SoundOutChannel;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.awt.*;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class Rdesktop {

    static Logger logger = Logger.getLogger(Rdp.class);

    /**
     * Translate a disconnect code into a textual description of the reason for the disconnect
     *
     * @param reason Integer disconnect code received from server
     * @return Text description of the reason for disconnection
     */
    static String textDisconnectReason(int reason) {
        String text;

        switch (reason) {
            case exDiscReasonNoInfo:
                text = "No information available";
                break;

            case exDiscReasonAPIInitiatedDisconnect:
                text = "Server initiated disconnect";
                break;

            case exDiscReasonAPIInitiatedLogoff:
                text = "Server initiated logoff";
                break;

            case exDiscReasonServerIdleTimeout:
                text = "Server idle timeout reached";
                break;

            case exDiscReasonServerLogonTimeout:
                text = "Server logon timeout reached";
                break;

            case exDiscReasonReplacedByOtherConnection:
                text = "Another user connected to the session";
                break;

            case exDiscReasonOutOfMemory:
                text = "The server is out of memory";
                break;

            case exDiscReasonServerDeniedConnection:
                text = "The server denied the connection";
                break;

            case exDiscReasonServerDeniedConnectionFips:
                text = "The server denied the connection for security reason";
                break;

            case exDiscReasonLicenseInternal:
                text = "Internal licensing error";
                break;

            case exDiscReasonLicenseNoLicenseServer:
                text = "No license server available";
                break;

            case exDiscReasonLicenseNoLicense:
                text = "No valid license available";
                break;

            case exDiscReasonLicenseErrClientMsg:
                text = "Invalid licensing message";
                break;

            case exDiscReasonLicenseHwidDoesntMatchLicense:
                text = "Hardware id doesn't match software license";
                break;

            case exDiscReasonLicenseErrClientLicense:
                text = "Client license error";
                break;

            case exDiscReasonLicenseCantFinishProtocol:
                text = "Network error during licensing protocol";
                break;

            case exDiscReasonLicenseClientEndedProtocol:
                text = "Licensing protocol was not completed";
                break;

            case exDiscReasonLicenseErrClientEncryption:
                text = "Incorrect client license enryption";
                break;

            case exDiscReasonLicenseCantUpgradeLicense:
                text = "Can't upgrade license";
                break;

            case exDiscReasonLicenseNoRemoteConnections:
                text = "The server is not licensed to accept remote connections";
                break;

            default:
                if (reason > 0x1000 && reason < 0x7fff) {
                    text = "Internal protocol error";
                } else {
                    text = "Unknown reason";
                }
        }
        return text;
    }

    /* RDP5 disconnect PDU */
    public static final int exDiscReasonNoInfo = 0x0000;
    public static final int exDiscReasonAPIInitiatedDisconnect = 0x0001;
    public static final int exDiscReasonAPIInitiatedLogoff = 0x0002;
    public static final int exDiscReasonServerIdleTimeout = 0x0003;
    public static final int exDiscReasonServerLogonTimeout = 0x0004;
    public static final int exDiscReasonReplacedByOtherConnection = 0x0005;
    public static final int exDiscReasonOutOfMemory = 0x0006;
    public static final int exDiscReasonServerDeniedConnection = 0x0007;
    public static final int exDiscReasonServerDeniedConnectionFips = 0x0008;
    public static final int exDiscReasonLicenseInternal = 0x0100;
    public static final int exDiscReasonLicenseNoLicenseServer = 0x0101;
    public static final int exDiscReasonLicenseNoLicense = 0x0102;
    public static final int exDiscReasonLicenseErrClientMsg = 0x0103;
    public static final int exDiscReasonLicenseHwidDoesntMatchLicense = 0x0104;
    public static final int exDiscReasonLicenseErrClientLicense = 0x0105;
    public static final int exDiscReasonLicenseCantFinishProtocol = 0x0106;
    public static final int exDiscReasonLicenseClientEndedProtocol = 0x0107;
    public static final int exDiscReasonLicenseErrClientEncryption = 0x0108;
    public static final int exDiscReasonLicenseCantUpgradeLicense = 0x0109;
    public static final int exDiscReasonLicenseNoRemoteConnections = 0x010a;

    static boolean keep_running;

    static boolean loggedOn;

    static boolean readyToSend;

    static boolean showTools;

    static final String keyMapPath = "keymaps/";

    static String mapFile = "en-gb";

    static String keyMapLocation = "";

    static SendEvent toolFrame = null;


    /**
     * Outputs version and usage information via System.err
     */
    public static void usage() {
        System.err.println("properJavaRDP version " + Version.version);
        System.err.println("Usage: java net.propero.rdp.Rdesktop [options] server[:port]");
        System.err.println("	-b 							bandwidth saving (good for 56k modem, but higher latency");
        System.err.println("	-c DIR						working directory");
        System.err.println("	-d DOMAIN					logon domain");
        System.err.println("	-f[l]						full-screen mode [with Linux KDE optimization]");
        System.err.println("	-g WxH						desktop geometry");
        System.err.println("	-m MAPFILE					keyboard mapping file for terminal server");
        System.err.println("	-l LEVEL					logging level {DEBUG, INFO, WARN, ERROR, FATAL}");
        System.err.println("	-n HOSTNAME					client hostname");
        System.err.println("	-p PASSWORD					password");
        System.err.println("	-s SHELL					shell");
        System.err.println("	-t NUM						RDP port (default 3389)");
        System.err.println("	-T TITLE					window title");
        System.err.println("	-u USERNAME					user name");
        System.err.println("	-o BPP						bits-per-pixel for display");
        System.err.println("    -r path                     path to load licence from (requests and saves licence from server if not found)");
        System.err.println("    --saveLicence              request and save licence from server");
        System.err.println("    --loadLicence              load licence from file");
        System.err.println("    --console                   connect to console");
        System.err.println("	--debug_key 				show scancodes sent for each keypress etc");
        System.err.println("	--debug_hex 				show bytes sent and received");
        System.err.println("	--no_remap_hash 			disable hash remapping");
        System.err.println("	--quiet_alt 				enable quiet alt fix");
        System.err.println("	--no_encryption				disable encryption from client to server");
        System.err.println("	--use_rdp4					use RDP version 4");
        //System.err.println("    --enableMenu               enable menu bar");
        System.err.println("	--log4j_config=FILE			use FILE for log4j configuration");
        System.err.println("Example: java net.propero.rdp.Rdesktop -g 800x600 -l WARN m52.propero.int");
        Rdesktop.exit(0, null, null, true);
    }


    /**
     * @param args passed in from the command line
     * @throws OrderException    One or more orders is not supported
     * @throws RdesktopException Protocol Error
     */
    public static void main(String[] args) throws OrderException,
            RdesktopException {

        // Ensure that static variables are properly initialised
        keep_running = true;
        loggedOn = false;
        readyToSend = false;
        showTools = false;
        mapFile = "en-us";
        keyMapLocation = "";
        toolFrame = null;

        BasicConfigurator.configure();
        logger.setLevel(Level.WARN);

        // parse arguments

        int logonflags = Rdp.RDP_LOGON_NORMAL;

        boolean fKdeHack = false;
        int c;
        String arg;
        StringBuffer sb = new StringBuffer();
        LongOpt[] alo = new LongOpt[15];
        alo[0] = new LongOpt("debug_key", LongOpt.NO_ARGUMENT, null, 0);
        alo[1] = new LongOpt("debug_hex", LongOpt.NO_ARGUMENT, null, 0);
        alo[2] = new LongOpt("no_paste_hack", LongOpt.NO_ARGUMENT, null, 0);
        alo[3] = new LongOpt("log4j_config", LongOpt.REQUIRED_ARGUMENT, sb, 0);
        alo[4] = new LongOpt("packet_tools", LongOpt.NO_ARGUMENT, null, 0);
        alo[5] = new LongOpt("quiet_alt", LongOpt.NO_ARGUMENT, sb, 0);
        alo[6] = new LongOpt("no_remap_hash", LongOpt.NO_ARGUMENT, null, 0);
        alo[7] = new LongOpt("no_encryption", LongOpt.NO_ARGUMENT, null, 0);
        alo[8] = new LongOpt("use_rdp4", LongOpt.NO_ARGUMENT, null, 0);
        alo[9] = new LongOpt("use_ssl", LongOpt.NO_ARGUMENT, null, 0);
        alo[10] = new LongOpt("enableMenu", LongOpt.NO_ARGUMENT, null, 0);
        alo[11] = new LongOpt("console", LongOpt.NO_ARGUMENT, null, 0);
        alo[12] = new LongOpt("loadLicence", LongOpt.NO_ARGUMENT, null, 0);
        alo[13] = new LongOpt("saveLicence", LongOpt.NO_ARGUMENT, null, 0);
        alo[14] = new LongOpt("persistent_caching", LongOpt.NO_ARGUMENT, null, 0);

        String progname = "properJavaRDP";

        Getopt g = new Getopt("properJavaRDP", args,
                "bc:d:f::g:k:l:m:n:p:s:t:T:u:o:r:", alo);

        ClipChannel clipChannel = new ClipChannel();
        boolean soundLocal = true; // bring sound to this computer by default
        boolean rdpefsLocal = true; // bring rdpefs to this computer by default

        while ((c = g.getopt()) != -1) {
            switch (c) {

                case 0:
                    switch (g.getLongind()) {
                        case 0:
                            Options.setDebugKeyboard(true);
                            break;
                        case 1:
                            Options.setDebugHexDump(true);
                            break;
                        case 2:
                            break;
                        case 3:
                            arg = g.getOptarg();
                            PropertyConfigurator.configure(arg);
                            logger.info("Log4j using config file " + arg);
                            break;
                        case 4:
                            showTools = true;
                            break;
                        case 5:
                            Options.setAltKeyQuite(true);
                            break;
                        case 6:
                            Options.setRemapHash(false);
                            break;
                        case 7:
                            Options.setPacketEncryption(false);
                            break;
                        case 8:
                            Options.setUseRdp5(false);
                            Options.setBpp(8);
                            break;
                        case 9:
                            Options.setUseSsl(true);
                            break;
                        case 10:
                            Options.setEnableMenu(true);
                            break;
                        case 11:
                            Options.setConsoleSession(true);
                            break;
                        case 12:
                            Options.setLoadLicence(true);
                            break;
                        case 13:
                            Options.setSaveLicence(true);
                            break;
                        case 14:
                            Options.setPersistentBitmapCaching(true);
                            break;
                        default:
                            usage();
                    }
                    break;

                case 'o':
                    Options.setBpp(Integer.parseInt(g.getOptarg()));
                    break;
                case 'b':
                    Options.setLowLatency(false);
                    break;
                case 'm':
                    mapFile = g.getOptarg();
                    break;
                case 'c':
                    Options.setDirectory(g.getOptarg());
                    break;
                case 'd':
                    Options.setDomain(g.getOptarg());
                    break;
                case 'f':
                    Dimension screen_size = Toolkit.getDefaultToolkit()
                            .getScreenSize();
                    // ensure width a multiple of 4
                    Options.setWidth(screen_size.width & ~3);
                    Options.setHeight(screen_size.height);
                    Options.setFullScreen(true);
                    arg = g.getOptarg();
                    if (arg != null) {
                        if (arg.charAt(0) == 'l')
                            fKdeHack = true;
                        else {
                            System.err.println(progname
                                    + ": Invalid fullscreen option '" + arg + "'");
                            usage();
                        }
                    }
                    break;
                case 'g':
                    arg = g.getOptarg();
                    int cut = arg.indexOf("x", 0);
                    if (cut == -1) {
                        System.err.println(progname + ": Invalid geometry: " + arg);
                        usage();
                    }
                    Options.setWidth(Integer.parseInt(arg.substring(0, cut)) & ~3);
                    Options.setHeight(Integer.parseInt(arg.substring(cut + 1)));
                    break;
                case 'k':
                    arg = g.getOptarg();
                    //Options.keylayout = KeyLayout.strToCode(arg);
                    if (Options.getKeyLayout() == -1) {
                        System.err.println(progname + ": Invalid key layout: "
                                + arg);
                        usage();
                    }
                    break;
                case 'l':
                    arg = g.getOptarg();
                    switch (arg.charAt(0)) {
                        case 'd':
                        case 'D':
                            logger.setLevel(Level.DEBUG);
                            break;
                        case 'i':
                        case 'I':
                            logger.setLevel(Level.INFO);
                            break;
                        case 'w':
                        case 'W':
                            logger.setLevel(Level.WARN);
                            break;
                        case 'e':
                        case 'E':
                            logger.setLevel(Level.ERROR);
                            break;
                        case 'f':
                        case 'F':
                            logger.setLevel(Level.FATAL);
                            break;
                        default:
                            System.err.println(progname + ": Invalid debug level: "
                                    + arg.charAt(0));
                            usage();
                    }
                    break;
                case 'n':
                    Options.setHostname(g.getOptarg());
                    break;
                case 'p':
                    Options.setPassword(g.getOptarg());
                    logonflags |= Rdp.RDP_LOGON_AUTO;
                    break;
                case 's':
                    Options.setCommand(g.getOptarg());
                    break;
                case 'u':
                    Options.setUserName(g.getOptarg());
                    break;
                case 't':
                    arg = g.getOptarg();
                    try {
                        Options.setPort(Integer.parseInt(arg));
                    } catch (NumberFormatException nex) {
                        System.err.println(progname + ": Invalid port number: " + arg);
                        usage();
                    }
                    break;
                case 'T':
                    Options.setWindowTitle(g.getOptarg().replace('_', ' '));
                    break;
                case 'r':
                    Options.setLicencePath(g.getOptarg());
                    break;

                case 'a':
                    String redirectAudio = g.getOptarg();
                    if (redirectAudio.equalsIgnoreCase("off"))
                        soundLocal = false;
                    else if (redirectAudio.equalsIgnoreCase("remote")) {
                        logonflags |= Rdp.RDP_LOGON_LEAVE_AUDIO;
                        soundLocal = false;
                    }
                    break;

                case 'x':
                    String redirectRDPEFS = g.getOptarg();
                    if (redirectRDPEFS.equalsIgnoreCase("off"))
                        rdpefsLocal = false;

                case '?':
                default:
                    usage();
                    break;

            }
        }

        if (fKdeHack) {
            Options.setHeight(Options.getHeight() - 46);
        }

        String server = null;

        if (g.getOptind() < args.length) {
            int colonat = args[args.length - 1].indexOf(":", 0);
            if (colonat == -1) {
                server = args[args.length - 1];
            } else {
                server = args[args.length - 1].substring(0, colonat);
                Options.setPort(Integer.parseInt(args[args.length - 1]
                        .substring(colonat + 1)));
            }
        } else {
            System.err.println(progname + ": A server name is required!");
            usage();
        }

        VChannels channels = new VChannels();

        // Initialise all RDP5 channels
        if (Options.isRdp5()) {
            // TODO: implement all relevant channels
            if (Options.isMapClipboard())
                channels.register(clipChannel);

            String java = System.getProperty("java.specification.version");
            if ((java.compareTo("1.4") >= 0) && soundLocal) {
                logger.info("Java support sound" + java);
                channels.register(new SoundOutChannel());
                logger.info("Java supports rdpefs " + java);
                channels.register(new RdpefsChannel());
            }
        }

        // Now do the startup...

        logger.info("properJavaRDP version " + Version.version);

        if (args.length == 0)
            usage();

        String java = System.getProperty("java.specification.version");
        logger.info("Java version is " + java);

        String os = System.getProperty("os.name");
        String osvers = System.getProperty("os.version");

        if (os.equals("Windows 2000") || os.equals("Windows XP"))
            Options.setBuiltInLicence(true);

        logger.info("Operating System is " + os + " version " + osvers);

        if (os.startsWith("Linux")) {
            Constants.OS = Constants.TS_OSMAJORTYPE_UNIX;
        } else if (os.startsWith("Windows")) {
            Constants.OS = Constants.TS_OSMAJORTYPE_WINDOWS;
        } else if (os.startsWith("Mac")) {
            Constants.OS = Constants.TS_OSMAJORTYPE_MACINTOSH;
            if (os.contains("2000")) {
                Constants.OS_MINOR = Constants.TS_OSMINORTYPE_WINDOWS_2000;
            } else if (os.contains("7")) {
                Constants.OS_MINOR = Constants.TS_OSMINORTYPE_WINDOWS_7;
            } else if (os.contains("95")) {
                Constants.OS_MINOR = Constants.TS_OSMINORTYPE_WINDOWS_95;
            } else if (os.contains("98")) {
                Constants.OS_MINOR = Constants.TS_OSMINORTYPE_WINDOWS_98;
            } else if (os.contains("NT")) {
                Constants.OS_MINOR = Constants.TS_OSMINORTYPE_WINDOWS_NT;
            } else if (os.contains("Vista")) {
                Constants.OS_MINOR = Constants.TS_OSMINORTYPE_WINDOWS_Vista;
            } else if (os.contains("XP")) {
                Constants.OS_MINOR = Constants.TS_OSMINORTYPE_WINDOWS_XP;
            }
        }


        if (Constants.OS == Constants.TS_OSMAJORTYPE_MACINTOSH)
            Options.setCapsSendsUpAndDown(false);

        Rdp5 RdpLayer = null;
        Common.rdp = RdpLayer;
        RdesktopFrame window = new RdesktopFrame();
        window.setClip(clipChannel);

        // Configure a keyboard layout
        KeyCode_FileBased keyMap = null;
        try {
            // logger.info("looking for: " + "/" + keyMapPath + mapFile);
            InputStream istr = Rdesktop.class.getResourceAsStream("/" + keyMapPath + mapFile);
            // logger.info("istr = " + istr);
            if (istr == null) {
                logger.debug("Loading keymap from filename");
                keyMap = new KeyCode_FileBased(keyMapPath + mapFile);
            } else {
                logger.debug("Loading keymap from InputStream");
                keyMap = new KeyCode_FileBased(istr);
            }
            if (istr != null)
                istr.close();
            Options.setKeyLayout(keyMap.getMapCode());
        } catch (Exception kmEx) {
            String[] msg = {(kmEx.getClass() + ": " + kmEx.getMessage())};
            window.showErrorDialog(msg);
            kmEx.printStackTrace();
            Rdesktop.exit(0, null, null, true);
        }

        logger.debug("Registering keyboard...");
        if (keyMap != null)
            window.registerKeyboard(keyMap);

        boolean[] deactivated = new boolean[1];
        int[] ext_disc_reason = new int[1];

        logger.debug("keep_running = " + keep_running);
        while (keep_running) {
            logger.debug("Initialising RDP layer...");
            RdpLayer = new Rdp5(channels);
            Common.rdp = RdpLayer;
            logger.debug("Registering drawing surface...");
            RdpLayer.registerDrawingSurface(window);
            logger.debug("Registering comms layer...");
            window.registerCommLayer(RdpLayer);
            loggedOn = false;
            readyToSend = false;
            logger.info("Connecting to " + server + ":" + Options.getPort() + " ...");

            if (server.equalsIgnoreCase("localhost"))
                server = "127.0.0.1";

            if (RdpLayer != null) {
                // Attempt to connect to server on port Options.port
                try {
                    RdpLayer.connect(Options.getUserName(),
                            InetAddress.getByName(server),
                            logonflags, Options.getDomain(),
                            Options.getPassword(),
                            Options.getCommand(),
                            Options.getDirectory());

                    // Remove to get rid of sendEvent tool
                    if (showTools) {
                        toolFrame = new SendEvent(RdpLayer);
                        toolFrame.setVisible(true);
                    }
                    // End

                    if (keep_running) {

                        /*
                           * By setting encryption to False here, we have an encrypted
                           * login packet but unencrypted transfer of other packets
                           */
                        if (!Options.isPacketEncryption())
                            Options.setEncryption(false);

                        logger.info("Connection successful");
                        // now show window after licence negotiation
                        RdpLayer.mainLoop(deactivated, ext_disc_reason);

                        if (deactivated[0]) {
                            /* clean disconnect */
                            Rdesktop.exit(0, RdpLayer, window, true);
                            // return 0;
                        } else {
                            if (ext_disc_reason[0] == exDiscReasonAPIInitiatedDisconnect
                                    || ext_disc_reason[0] == exDiscReasonAPIInitiatedLogoff) {
                                /* not so clean disconnect, but nothing to worry about */
                                Rdesktop.exit(0, RdpLayer, window, true);
                                //return 0;
                            }

                            if (ext_disc_reason[0] >= 2) {
                                String reason = textDisconnectReason(ext_disc_reason[0]);
                                String msg[] = {"Connection terminated", reason};
                                window.showErrorDialog(msg);
                                logger.warn("Connection terminated: " + reason);
                                Rdesktop.exit(0, RdpLayer, window, true);
                            }

                        }

                        keep_running = false; // exited main loop
                        if (!readyToSend) {
                            // maybe the licence server was having a comms
                            // problem, retry?
                            String msg1 = "The terminal server disconnected before licence negotiation completed.";
                            String msg2 = "Possible cause: terminal server could not issue a licence.";
                            String[] msg = {msg1, msg2};
                            logger.warn(msg1);
                            logger.warn(msg2);
                            window.showErrorDialog(msg);
                        }
                    } // closing bracket to if(running)

                    // Remove to get rid of tool window
                    if (showTools)
                        toolFrame.dispose();
                    // End

                } catch (ConnectionException e) {
                    String msg[] = {"Connection Exception", e.getMessage()};
                    window.showErrorDialog(msg);
                    Rdesktop.exit(0, RdpLayer, window, true);
                } catch (UnknownHostException e) {
                    error(e, RdpLayer, window, true);
                } catch (SocketException s) {
                    if (RdpLayer.isConnected()) {
                        logger.fatal(s.getClass().getName() + " " + s.getMessage());
                        //s.printStackTrace();
                        error(s, RdpLayer, window, true);
                        Rdesktop.exit(0, RdpLayer, window, true);
                    }
                } catch (RdesktopException e) {
                    String msg1 = e.getClass().getName();
                    String msg2 = e.getMessage();
                    logger.fatal(msg1 + ": " + msg2);

                    e.printStackTrace(System.err);

                    if (!readyToSend) {
                        // maybe the licence server was having a comms
                        // problem, retry?
                        String msg[] = {
                                "The terminal server reset connection before licence negotiation completed.",
                                "Possible cause: terminal server could not connect to licence server.",
                                "Retry?"};
                        boolean retry = window.showYesNoErrorDialog(msg);
                        if (!retry) {
                            logger.info("Selected not to retry.");
                            Rdesktop.exit(0, RdpLayer, window, true);
                        } else {
                            if (RdpLayer != null && RdpLayer.isConnected()) {
                                logger.info("Disconnecting ...");
                                RdpLayer.disconnect();
                                logger.info("Disconnected");
                            }
                            logger.info("Retrying connection...");
                            keep_running = true; // retry
                        }
                    } else {
                        String msg[] = {e.getMessage()};
                        window.showErrorDialog(msg);
                        Rdesktop.exit(0, RdpLayer, window, true);
                    }
                } catch (Exception e) {
                    logger.warn(e.getClass().getName() + " " + e.getMessage());
                    e.printStackTrace();
                    error(e, RdpLayer, window, true);
                }
            } else { // closing bracket to if(!rdp==null)
                logger.fatal("The communications layer could not be initiated!");
            }
        }
        Rdesktop.exit(0, RdpLayer, window, true);
    }

    /**
     * Disconnects from the server connected to through rdp and destroys the
     * RdesktopFrame window.
     * <p/>
     * Exits the application if systemExit == true, providing return value n to
     * the operating system.
     *
     * @param n          exit status
     * @param rdp        The application run loop
     * @param window     window that contains the rdp session visuals
     * @param systemExit terminate the vm
     */
    public static void exit(int n, Rdp rdp, RdesktopFrame window, boolean systemExit) {
        keep_running = false;

        // Remove to get rid of tool window
        if ((showTools) && (toolFrame != null))
            toolFrame.dispose();
        // End

        if (rdp != null && rdp.isConnected()) {
            logger.info("Disconnecting ...");
            rdp.disconnect();
            logger.info("Disconnected");
        }
        if (window != null) {
            window.setVisible(false);
            window.dispose();
        }

        System.gc();

        if (systemExit) {
            if (!Common.underApplet)
                System.exit(n);
        }
    }

    /**
     * Displays details of the Exception e in an error dialog via the
     * RdesktopFrame window and reports this through the logger, then prints a
     * stack trace.
     * <p/>
     * The application then exits iff systemExit == true
     *
     * @param e          Exception
     * @param RdpLayer   The main loop
     * @param window     Where the visual RDP displays
     * @param systemExit true if exit the VM on error
     */
    public static void error(Exception e, Rdp RdpLayer, RdesktopFrame window, boolean systemExit) {
        try {

            String msg1 = e.getClass().getName();
            String msg2 = e.getMessage();

            logger.fatal(msg1 + ": " + msg2);

            String[] msg = {msg1, msg2};
            window.showErrorDialog(msg);

            e.printStackTrace(System.err);
        } catch (Exception ex) {
            logger.warn("Exception in Rdesktop.error: " + ex.getClass().getName() + ": " + ex.getMessage());
        }

        Rdesktop.exit(0, RdpLayer, window, systemExit);
    }
}
