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

import androidx.annotation.NonNull;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

/**
 * Created by w5e.
 */
public final class PropertiesManager {
    private File mPropertiesFile;
    private Properties mProperties;

    public PropertiesManager(@NonNull File propertiesFile) {
        File parentDir = propertiesFile.getParentFile();
        parentDir.mkdirs();
        mPropertiesFile = propertiesFile;
        mProperties = new Properties();
        loadProperties();
    }

    public String getProperty(@NonNull String key) {
        return getProperty(key, false);
    }

    public String getProperty(@NonNull String key, boolean reload) {
        if (reload) {
            loadProperties();
        }
        try {
            return mProperties.getProperty(key);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void setProperty(@NonNull String key, String value) {
        OutputStream os = null;
        try {
            os = new FileOutputStream(mPropertiesFile);
            if (value == null) {
                mProperties.remove(key);
            } else {
                mProperties.setProperty(key, value);
            }
            mProperties.storeToXML(os, null);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(os);
        }
    }

    private void loadProperties() {
        InputStream is = null;
        try {
            if (mPropertiesFile.exists()) {
                is = new FileInputStream(mPropertiesFile);
                mProperties.loadFromXML(is);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(is);
        }
    }

    private void close(Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (IOException ignored) {
        }
    }
}
