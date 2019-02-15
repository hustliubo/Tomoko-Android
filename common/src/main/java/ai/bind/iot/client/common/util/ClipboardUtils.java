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

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;

import androidx.annotation.NonNull;

/**
 * 剪切板工具
 * Created by w5e.
 */
public final class ClipboardUtils {
    /**
     * 拷贝文本
     */
    public static void setText(@NonNull Context context, @NonNull CharSequence text) {
        ClipboardManager manager = getClipboardManager(context);
        if (manager == null) {
            return;
        }
        ClipData clipData = ClipData.newPlainText(null, text);
        manager.setPrimaryClip(clipData);
    }

    /**
     * 获取剪切板中的文本
     */
    public static CharSequence getText(@NonNull Context context) {
        ClipboardManager manager = getClipboardManager(context);
        if (manager == null) {
            return null;
        }
        ClipData clipData = manager.getPrimaryClip();
        if (clipData != null) {
            return clipData.getItemAt(0).getText();
        }
        return null;
    }

    private static ClipboardManager getClipboardManager(@NonNull Context context) {
        return (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
    }
}
