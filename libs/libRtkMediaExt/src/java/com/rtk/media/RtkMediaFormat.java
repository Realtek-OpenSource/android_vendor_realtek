/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.rtk.media.ext;

public final class RtkMediaFormat {

    /**
     * A key for the boolean MVC field for video track. True if it is a
     * MVC video track.
     */
    public static final String KEY_IS_MVC = "is-mvc";

    /**
     * A key for the boolean HDR field for video track. True if it is a
     * HDR video track.
     */
    public static final String KEY_IS_HDR = "is-hdr";

    /**
     * A key for the boolean progressive field for video track. True if it is a
     * progressive video track.
     */
    public static final String KEY_IS_PROGRESSIVE = "is-progressive";

    /**
     * A key for subtitle from. 
     * 1: SPU_FROM_EXTERNAL
     * 2: SPU_FROM_EMBEDDED
     * 3: SPU_FROM_EMBEDDED_TS_DVB
     * 4: SPU_FROM_EMBEDDED_TS_TT
     */
    public static final String KEY_SPU_FROM = "spu-from";

    /**
     * A key for subtitle type. 
     * 0: SUBTITLE_IDXSUB
     * 1: SUBTITLE_SRT
     * 2: SUBTITLE_SMI
     * 3: SUBTITLE_SSA
     * 4: SUBTITLE_ASS
     * 5: SUBTITLE_PSB
     * 6: SUBTITLE_TXT
     * 7: SUBTITLE_TEXTSUB
     * 8: SUBTITLE_SUBVIEWER
     * 9: SUBTITLE_LRC
     * 10: SUBTITLE_TEXTCPC
     * 11: SUBTITLE_TEXT
     */
    public static final String KEY_SPU_TYPE = "spu-type";
    /** @hide */
    public static final String KEY_IS_TIMED_TEXT = "is-timed-text";

}
