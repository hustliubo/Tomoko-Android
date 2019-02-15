/*
 * Copyright (c) 2018 CELLA
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.bind.iot.client.common.util;

import android.annotation.SuppressLint;
import android.os.Build;
import android.text.TextUtils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * Created by w5e.
 */
public class OsUtils {
    @SuppressLint({"HardwareIds", "MissingPermission"})
    @SuppressWarnings("deprecation")
    private static String getSerial() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            return Build.getSerial();
        } else {
            return Build.SERIAL;
        }
    }

    @SuppressLint({"HardwareIds", "PrivateApi"})
    public static String getSerialNo() {
        String serial = null;
        try { //兼容旧应用
            Class<?> clazz = Class.forName("android.os.SystemProperties");
            Method get = clazz.getMethod("get", String.class);
            serial = (String) get.invoke(clazz, "ro.boot.serialno");
        } catch (Exception ignored) {
        }
        if (TextUtils.isEmpty(serial)) {
            try {
                serial = getSerial();
            } catch (Exception ignored) {
            }
        }
        return serial;
    }

    public static String getIpAddress(boolean onlyIpV4) {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress inetAddress = addresses.nextElement();
                    if (onlyIpV4 && !(inetAddress instanceof Inet4Address)) {
                        continue;
                    }
                    if (!inetAddress.isLoopbackAddress()) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public static String getMacAddress() {
        String macPath = "sys/class/net/eth0/address";
        try (BufferedReader reader = new BufferedReader(new FileReader(macPath))) {
            return reader.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
