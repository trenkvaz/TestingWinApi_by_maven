package org.example.test;

import com.sun.jna.platform.win32.*;

import static com.sun.jna.platform.win32.WinUser.*;

public class Win32WindowDemo implements WinUser.WindowProc {

    /**
     * Instantiates a new win32 window test.
     */
    public Win32WindowDemo() {
        // define new window class
        String windowClass = new String("MyWindowClass");
        WinDef.HMODULE hInst = Kernel32.INSTANCE.GetModuleHandle("");

        WinUser.WNDCLASSEX wClass = new WinUser.WNDCLASSEX();
        wClass.hInstance = hInst;
        wClass.lpfnWndProc = Win32WindowDemo.this;
        wClass.lpszClassName = windowClass;
        wClass.cbWndExtra = 0;
        // register window class
        User32.INSTANCE.RegisterClassEx(wClass);
        getLastError();

        // create new window
        WinDef.HWND hWnd = User32.INSTANCE
                .CreateWindowEx(
                       // User32.WS_OVERLAPPED|User32.WS_EX_LAYERED,
                        User32.WS_EX_TOPMOST,
                        windowClass,
                        "Amphitrite II - $0.01/$0.02 - No Limit Hold`em - Logged In as trenkvaz",
                        User32.WS_VISIBLE, 100, 100, 600, 400,
                        null, // WM_DEVICECHANGE contradicts parent=WinUser.HWND_MESSAGE
                        null, hInst, null);

        getLastError();
        System.out.println("window sucessfully created! window hwnd: "
                + hWnd.getPointer().toString());
        System.out.println(hWnd.toString());
        Wtsapi32.INSTANCE.WTSRegisterSessionNotification(hWnd,
                Wtsapi32.NOTIFY_FOR_THIS_SESSION);

        /* this filters for all device classes */
        // DEV_BROADCAST_HDR notificationFilter = new DEV_BROADCAST_HDR();
        // notificationFilter.dbch_devicetype = DBT.DBT_DEVTYP_DEVICEINTERFACE;

        /* this filters for all usb device classes */
        DBT.DEV_BROADCAST_DEVICEINTERFACE notificationFilter = new DBT.DEV_BROADCAST_DEVICEINTERFACE();
        notificationFilter.dbcc_size = notificationFilter.size();
        notificationFilter.dbcc_devicetype = DBT.DBT_DEVTYP_DEVICEINTERFACE;
        notificationFilter.dbcc_classguid = DBT.GUID_DEVINTERFACE_USB_DEVICE;

        /*
         * use User32.DEVICE_NOTIFY_ALL_INTERFACE_CLASSES instead of
         * DEVICE_NOTIFY_WINDOW_HANDLE to ignore the dbcc_classguid value
         */
        WinUser.HDEVNOTIFY hDevNotify = User32.INSTANCE.RegisterDeviceNotification(
                hWnd, notificationFilter, User32.DEVICE_NOTIFY_WINDOW_HANDLE);

        getLastError();
        if (hDevNotify != null)
            System.out.println("RegisterDeviceNotification was sucessfully!");

        WinUser.MSG msg = new WinUser.MSG();
        while (User32.INSTANCE.GetMessage(msg, hWnd, 0, 0) != 0) {
            User32.INSTANCE.TranslateMessage(msg);
            User32.INSTANCE.DispatchMessage(msg);
        }

        User32.INSTANCE.UnregisterDeviceNotification(hDevNotify);
        Wtsapi32.INSTANCE.WTSUnRegisterSessionNotification(hWnd);
        User32.INSTANCE.UnregisterClass(windowClass, hInst);
        User32.INSTANCE.DestroyWindow(hWnd);

        System.out.println("program exit!");
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.sun.jna.platform.win32.User32.WindowProc#callback(com.sun.jna.platform
     * .win32.WinDef.HWND, int, com.sun.jna.platform.win32.WinDef.WPARAM,
     * com.sun.jna.platform.win32.WinDef.LPARAM)
     */
    public WinDef.LRESULT callback(WinDef.HWND hwnd, int uMsg, WinDef.WPARAM wParam, WinDef.LPARAM lParam) {
        switch (uMsg) {
            case WinUser.WM_CREATE: {
                onCreate(wParam, lParam);
                return new WinDef.LRESULT(0);
            }
            case WinUser.WM_DESTROY: {
                User32.INSTANCE.PostQuitMessage(0);
                return new WinDef.LRESULT(0);
            }
            case WinUser.WM_SESSION_CHANGE: {
                this.onSessionChange(wParam, lParam);
                return new WinDef.LRESULT(0);
            }
            case WinUser.WM_DEVICECHANGE: {
                WinDef.LRESULT lResult = this.onDeviceChange(wParam, lParam);
                return lResult != null ? lResult :
                        User32.INSTANCE.DefWindowProc(hwnd, uMsg, wParam, lParam);
            }
            default:
                return User32.INSTANCE.DefWindowProc(hwnd, uMsg, wParam, lParam);
        }
    }

    /**
     * Gets the last error.
     *
     * @return the last error
     */
    public int getLastError() {
        int rc = Kernel32.INSTANCE.GetLastError();

        if (rc != 0)
            System.out.println("error: " + rc);

        return rc;
    }

    /**
     * On session change.
     *
     * @param wParam
     *            the w param
     * @param lParam
     *            the l param
     */
    protected void onSessionChange(WinDef.WPARAM wParam, WinDef.LPARAM lParam) {
        switch (wParam.intValue()) {
            case Wtsapi32.WTS_CONSOLE_CONNECT: {
                this.onConsoleConnect(lParam.intValue());
                break;
            }
            case Wtsapi32.WTS_CONSOLE_DISCONNECT: {
                this.onConsoleDisconnect(lParam.intValue());
                break;
            }
            case Wtsapi32.WTS_SESSION_LOGON: {
                this.onMachineLogon(lParam.intValue());
                break;
            }
            case Wtsapi32.WTS_SESSION_LOGOFF: {
                this.onMachineLogoff(lParam.intValue());
                break;
            }
            case Wtsapi32.WTS_SESSION_LOCK: {
                this.onMachineLocked(lParam.intValue());
                break;
            }
            case Wtsapi32.WTS_SESSION_UNLOCK: {
                this.onMachineUnlocked(lParam.intValue());
                break;
            }
        }
    }

    /**
     * On console connect.
     *
     * @param sessionId
     *            the session id
     */
    protected void onConsoleConnect(int sessionId) {
        System.out.println("onConsoleConnect: " + sessionId);
    }

    /**
     * On console disconnect.
     *
     * @param sessionId
     *            the session id
     */
    protected void onConsoleDisconnect(int sessionId) {
        System.out.println("onConsoleDisconnect: " + sessionId);
    }

    /**
     * On machine locked.
     *
     * @param sessionId
     *            the session id
     */
    protected void onMachineLocked(int sessionId) {
        System.out.println("onMachineLocked: " + sessionId);
    }

    /**
     * On machine unlocked.
     *
     * @param sessionId
     *            the session id
     */
    protected void onMachineUnlocked(int sessionId) {
        System.out.println("onMachineUnlocked: " + sessionId);
    }

    /**
     * On machine logon.
     *
     * @param sessionId
     *            the session id
     */
    protected void onMachineLogon(int sessionId) {
        System.out.println("onMachineLogon: " + sessionId);
    }

    /**
     * On machine logoff.
     *
     * @param sessionId
     *            the session id
     */
    protected void onMachineLogoff(int sessionId) {
        System.out.println("onMachineLogoff: " + sessionId);
    }

    /**
     * On device change.
     *
     * @param wParam
     *            the w param
     * @param lParam
     *            the l param
     * @return the result. Null if the message is not processed.
     */
    protected WinDef.LRESULT onDeviceChange(WinDef.WPARAM wParam, WinDef.LPARAM lParam) {
        switch (wParam.intValue()) {
            case DBT.DBT_DEVICEARRIVAL: {
                return onDeviceChangeArrival(lParam);
            }
            case DBT.DBT_DEVICEREMOVECOMPLETE: {
                return onDeviceChangeRemoveComplete(lParam);
            }
            case DBT.DBT_DEVNODES_CHANGED: {
                //lParam is 0 for this wParam
                return onDeviceChangeNodesChanged();
            }
            default:
                System.out.println(
                        "Message WM_DEVICECHANGE message received, value unhandled.");
        }
        return null;
    }

    protected WinDef.LRESULT onDeviceChangeArrivalOrRemoveComplete(WinDef.LPARAM lParam, String action) {
        DBT.DEV_BROADCAST_HDR bhdr = new DBT.DEV_BROADCAST_HDR(lParam.longValue());
        switch (bhdr.dbch_devicetype) {
            case DBT.DBT_DEVTYP_DEVICEINTERFACE: {
                // see http://msdn.microsoft.com/en-us/library/windows/desktop/aa363244.aspx
                DBT.DEV_BROADCAST_DEVICEINTERFACE bdif = new DBT.DEV_BROADCAST_DEVICEINTERFACE(bhdr.getPointer());
                System.out.println("BROADCAST_DEVICEINTERFACE: " + action);
                System.out.println("dbcc_devicetype: " + bdif.dbcc_devicetype);
                System.out.println("dbcc_name: " + bdif.getDbcc_name());
                System.out.println("dbcc_classguid: "
                        + bdif.dbcc_classguid.toGuidString());
                break;
            }
            case DBT.DBT_DEVTYP_HANDLE: {
                // see http://msdn.microsoft.com/en-us/library/windows/desktop/aa363245.aspx
                DBT.DEV_BROADCAST_HANDLE bhd = new DBT.DEV_BROADCAST_HANDLE(bhdr.getPointer());
                System.out.println("BROADCAST_HANDLE: " + action);
                break;
            }
            case DBT.DBT_DEVTYP_OEM: {
                // see http://msdn.microsoft.com/en-us/library/windows/desktop/aa363247.aspx
                DBT.DEV_BROADCAST_OEM boem = new DBT.DEV_BROADCAST_OEM(bhdr.getPointer());
                System.out.println("BROADCAST_OEM: " + action);
                break;
            }
            case DBT.DBT_DEVTYP_PORT: {
                // see http://msdn.microsoft.com/en-us/library/windows/desktop/aa363248.aspx
                DBT.DEV_BROADCAST_PORT bpt = new DBT.DEV_BROADCAST_PORT(bhdr.getPointer());
                System.out.println("BROADCAST_PORT: " + action);
                break;
            }
            case DBT.DBT_DEVTYP_VOLUME: {
                // see http://msdn.microsoft.com/en-us/library/windows/desktop/aa363249.aspx
                DBT.DEV_BROADCAST_VOLUME bvl = new DBT.DEV_BROADCAST_VOLUME(bhdr.getPointer());
                int logicalDriveAffected = bvl.dbcv_unitmask;
                short flag = bvl.dbcv_flags;
                boolean isMediaNotPhysical = 0 != (flag & DBT.DBTF_MEDIA/*value is 1*/);
                boolean isNet = 0 != (flag & DBT.DBTF_NET/*value is 2*/);
                System.out.println(action);
                int driveLetterIndex = 0;
                while (logicalDriveAffected != 0) {
                    if (0 != (logicalDriveAffected & 1)) {
                        System.out.println("Logical Drive Letter: " +
                                ((char) ('A' + driveLetterIndex)));
                    }
                    logicalDriveAffected >>>= 1;
                    driveLetterIndex++;
                }
                System.out.println("isMediaNotPhysical:"+isMediaNotPhysical);
                System.out.println("isNet:"+isNet);
                break;
            }
            default:
                return null;
        }
        // return TRUE means processed message for this wParam.
        // see http://msdn.microsoft.com/en-us/library/windows/desktop/aa363205.aspx
        // see http://msdn.microsoft.com/en-us/library/windows/desktop/aa363208.aspx
        return new WinDef.LRESULT(1);
    }

    protected WinDef.LRESULT onDeviceChangeArrival(WinDef.LPARAM lParam) {
        return onDeviceChangeArrivalOrRemoveComplete(lParam, "Arrival");
    }

    protected WinDef.LRESULT onDeviceChangeRemoveComplete(WinDef.LPARAM lParam) {
        return onDeviceChangeArrivalOrRemoveComplete(lParam, "Remove Complete");
    }

    protected WinDef.LRESULT onDeviceChangeNodesChanged() {
        System.out.println("Message DBT_DEVNODES_CHANGED");
        // return TRUE means processed message for this wParam.
        // see http://msdn.microsoft.com/en-us/library/windows/desktop/aa363211.aspx
        return new WinDef.LRESULT(1);
    }

    /**
     * On create.
     *
     * @param wParam
     *            the w param
     * @param lParam
     *            the l param
     */
    protected void onCreate(WinDef.WPARAM wParam, WinDef.LPARAM lParam) {
        System.out.println("onCreate: WM_CREATE");
    }

    /**
     * The main method.
     *
     * @param args
     *            the arguments
     */
    public static void main(String[] args) {
        new Win32WindowDemo();
    }
}
