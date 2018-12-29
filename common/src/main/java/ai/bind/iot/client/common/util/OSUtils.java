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

import java.lang.reflect.Method;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * Created by w5e.
 */
public class OSUtils {
    @SuppressLint({"HardwareIds", "MissingPermission"})
    @SuppressWarnings("deprecation")
    private static String getSerial() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            return Build.getSerial();
        } else {
            return Build.SERIAL;
        }
    }

    @SuppressLint("HardwareIds")
    public static String getSerialNo() {
        String serial = null;
        try {//兼容旧应用
            @SuppressLint("PrivateApi") Class<?> c = Class.forName("android.os.SystemProperties");
            Method get = c.getMethod("get", String.class);
            serial = (String) get.invoke(c, "ro.boot.serialno");
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

    public static String getIpAddress(boolean onlyIPV4) {
        try {
            for (Enumeration<NetworkInterface> interfaceEnumeration
                 = NetworkInterface.getNetworkInterfaces();
                 interfaceEnumeration.hasMoreElements(); ) {
                NetworkInterface networkInterface = interfaceEnumeration.nextElement();
                for (Enumeration<InetAddress> addresses
                     = networkInterface.getInetAddresses(); addresses.hasMoreElements(); ) {
                    InetAddress inetAddress = addresses.nextElement();
                    if (onlyIPV4 && !(inetAddress instanceof Inet4Address)) continue;
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
}
