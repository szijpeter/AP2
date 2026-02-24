/**
 * Copyright 2025 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * ```
 *     https://www.apache.org/licenses/LICENSE-2.0
 * ```
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.example.ap2sample.platform

import android.util.Log

actual object PlatformLogger {
    actual fun d(tag: String, message: String) {
        Log.d(tag, message)
    }
    actual fun i(tag: String, message: String) {
        Log.i(tag, message)
    }
    actual fun w(tag: String, message: String) {
        Log.w(tag, message)
    }
    actual fun e(tag: String, message: String, throwable: Throwable?) {
        if (throwable != null) Log.e(tag, message, throwable) else Log.e(tag, message)
    }
}
