package com.ranhroi.esp32hidremote.data.hid

/**
 * Tự động nhận diện + chuyển đổi tiếng Việt.
 *
 * Không cần user chọn mode.
 * Chỉ chuyển ký tự Việt → Telex, giữ nguyên ASCII.
 *
 * TỐI ƯU:
 *  - Dùng mảng tra cứu O(1) thay vì HashMap
 *  - Duyệt text 1 lần duy nhất (không tách hasVietnamese + toTelex)
 *  - Không gọi lowercaseChar()/uppercase() trong vòng lặp
 */
object VietnameseUtils {

    // ============================================================
    // BẢNG TRA CỨU — MẢNG THAY VÌ MAP
    // Kích thước 0x1EF9 + 1 = 7929 (bao phủ toàn bộ Latin Extended Additional)
    // ============================================================
    private const val TABLE_SIZE = 0x1EF9 + 1

    private val telexLower: Array<String?> = arrayOfNulls(TABLE_SIZE)
    private val telexUpper: Array<String?> = arrayOfNulls(TABLE_SIZE)

    init {
        // ---- Chữ cái đặc biệt (thường) ----
        telexLower['ă'.code] = "aw"
        telexLower['â'.code] = "aa"
        telexLower['đ'.code] = "dd"
        telexLower['ê'.code] = "ee"
        telexLower['ô'.code] = "oo"
        telexLower['ơ'.code] = "ow"
        telexLower['ư'.code] = "uw"

        // ---- a + dấu ----
        telexLower['á'.code] = "as"
        telexLower['à'.code] = "af"
        telexLower['ả'.code] = "ar"
        telexLower['ã'.code] = "ax"
        telexLower['ạ'.code] = "aj"

        // ---- e + dấu ----
        telexLower['é'.code] = "es"
        telexLower['è'.code] = "ef"
        telexLower['ẻ'.code] = "er"
        telexLower['ẽ'.code] = "ex"
        telexLower['ẹ'.code] = "ej"

        // ---- i + dấu ----
        telexLower['í'.code] = "is"
        telexLower['ì'.code] = "if"
        telexLower['ỉ'.code] = "ir"
        telexLower['ĩ'.code] = "ix"
        telexLower['ị'.code] = "ij"

        // ---- o + dấu ----
        telexLower['ó'.code] = "os"
        telexLower['ò'.code] = "of"
        telexLower['ỏ'.code] = "or"
        telexLower['õ'.code] = "ox"
        telexLower['ọ'.code] = "oj"

        // ---- u + dấu ----
        telexLower['ú'.code] = "us"
        telexLower['ù'.code] = "uf"
        telexLower['ủ'.code] = "ur"
        telexLower['ũ'.code] = "ux"
        telexLower['ụ'.code] = "uj"

        // ---- y + dấu ----
        telexLower['ý'.code] = "ys"
        telexLower['ỳ'.code] = "yf"
        telexLower['ỷ'.code] = "yr"
        telexLower['ỹ'.code] = "yx"
        telexLower['ỵ'.code] = "yj"

        // ---- ă + dấu ----
        telexLower['ắ'.code] = "aws"
        telexLower['ằ'.code] = "awf"
        telexLower['ẳ'.code] = "awr"
        telexLower['ẵ'.code] = "awx"
        telexLower['ặ'.code] = "awj"

        // ---- â + dấu ----
        telexLower['ấ'.code] = "aas"
        telexLower['ầ'.code] = "aaf"
        telexLower['ẩ'.code] = "aar"
        telexLower['ẫ'.code] = "aax"
        telexLower['ậ'.code] = "aaj"

        // ---- ê + dấu ----
        telexLower['ế'.code] = "ees"
        telexLower['ề'.code] = "eef"
        telexLower['ể'.code] = "eer"
        telexLower['ễ'.code] = "eex"
        telexLower['ệ'.code] = "eej"

        // ---- ô + dấu ----
        telexLower['ố'.code] = "oos"
        telexLower['ồ'.code] = "oof"
        telexLower['ổ'.code] = "oor"
        telexLower['ỗ'.code] = "oox"
        telexLower['ộ'.code] = "ooj"

        // ---- ơ + dấu ----
        telexLower['ớ'.code] = "ows"
        telexLower['ờ'.code] = "owf"
        telexLower['ở'.code] = "owr"
        telexLower['ỡ'.code] = "owx"
        telexLower['ợ'.code] = "owj"

        // ---- ư + dấu ----
        telexLower['ứ'.code] = "uws"
        telexLower['ừ'.code] = "uwf"
        telexLower['ử'.code] = "uwr"
        telexLower['ữ'.code] = "uwx"
        telexLower['ự'.code] = "uwj"

        // ---- Điền chữ HOA tương ứng ----
        // Chữ hoa tiếng Việt có mã Unicode riêng, cần điền thủ công
        telexUpper['Ă'.code] = "AW"
        telexUpper['Â'.code] = "AA"
        telexUpper['Đ'.code] = "DD"
        telexUpper['Ê'.code] = "EE"
        telexUpper['Ô'.code] = "OO"
        telexUpper['Ơ'.code] = "OW"
        telexUpper['Ư'.code] = "UW"

        telexUpper['Á'.code] = "AS"
        telexUpper['À'.code] = "AF"
        telexUpper['Ả'.code] = "AR"
        telexUpper['Ã'.code] = "AX"
        telexUpper['Ạ'.code] = "AJ"

        telexUpper['É'.code] = "ES"
        telexUpper['È'.code] = "EF"
        telexUpper['Ẻ'.code] = "ER"
        telexUpper['Ẽ'.code] = "EX"
        telexUpper['Ẹ'.code] = "EJ"

        telexUpper['Í'.code] = "IS"
        telexUpper['Ì'.code] = "IF"
        telexUpper['Ỉ'.code] = "IR"
        telexUpper['Ĩ'.code] = "IX"
        telexUpper['Ị'.code] = "IJ"

        telexUpper['Ó'.code] = "OS"
        telexUpper['Ò'.code] = "OF"
        telexUpper['Ỏ'.code] = "OR"
        telexUpper['Õ'.code] = "OX"
        telexUpper['Ọ'.code] = "OJ"

        telexUpper['Ú'.code] = "US"
        telexUpper['Ù'.code] = "UF"
        telexUpper['Ủ'.code] = "UR"
        telexUpper['Ũ'.code] = "UX"
        telexUpper['Ụ'.code] = "UJ"

        telexUpper['Ý'.code] = "YS"
        telexUpper['Ỳ'.code] = "YF"
        telexUpper['Ỷ'.code] = "YR"
        telexUpper['Ỹ'.code] = "YX"
        telexUpper['Ỵ'.code] = "YJ"

        telexUpper['Ắ'.code] = "AWS"
        telexUpper['Ằ'.code] = "AWF"
        telexUpper['Ẳ'.code] = "AWR"
        telexUpper['Ẵ'.code] = "AWX"
        telexUpper['Ặ'.code] = "AWJ"

        telexUpper['Ấ'.code] = "AAS"
        telexUpper['Ầ'.code] = "AAF"
        telexUpper['Ẩ'.code] = "AAR"
        telexUpper['Ẫ'.code] = "AAX"
        telexUpper['Ậ'.code] = "AAJ"

        telexUpper['Ế'.code] = "EES"
        telexUpper['Ề'.code] = "EEF"
        telexUpper['Ể'.code] = "EER"
        telexUpper['Ễ'.code] = "EEX"
        telexUpper['Ệ'.code] = "EEJ"

        telexUpper['Ố'.code] = "OOS"
        telexUpper['Ồ'.code] = "OOF"
        telexUpper['Ổ'.code] = "OOR"
        telexUpper['Ỗ'.code] = "OOX"
        telexUpper['Ộ'.code] = "OOJ"

        telexUpper['Ớ'.code] = "OWS"
        telexUpper['Ờ'.code] = "OWF"
        telexUpper['Ở'.code] = "OWR"
        telexUpper['Ỡ'.code] = "OWX"
        telexUpper['Ợ'.code] = "OWJ"

        telexUpper['Ứ'.code] = "UWS"
        telexUpper['Ừ'.code] = "UWF"
        telexUpper['Ử'.code] = "UWR"
        telexUpper['Ữ'.code] = "UWX"
        telexUpper['Ự'.code] = "UWJ"
    }

    // ============================================================
    // NHẬN DIỆN
    // ============================================================

    /**
     * Kiểm tra text có chứa ký tự tiếng Việt không.
     * Duyệt 1 lần, tra cứu O(1) bằng mảng.
     */
    fun hasVietnamese(text: String): Boolean {
        for (ch in text) {
            val code = ch.code
            if (code < TABLE_SIZE) {
                if (telexLower[code] != null || telexUpper[code] != null) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * Kiểm tra text có phải tiếng Việt không.
     * (Có ít nhất 1 ký tự Việt)
     */
    fun isVietnamese(text: String): Boolean {
        return hasVietnamese(text)
    }

    // ============================================================
    // CHUYỂN ĐỔI
    // ============================================================

    /**
     * Chuyển 1 ký tự → Telex.
     * Trả về null nếu không phải ký tự Việt.
     */
    fun toTelex(ch: Char): String? {
        val code = ch.code
        if (code >= TABLE_SIZE) return null
        return telexUpper[code] ?: telexLower[code]
    }

    /**
     * Chuyển cả chuỗi → Telex.
     * CHỈ chuyển ký tự Việt, GIỮ NGUYÊN ASCII.
     *
     * VD: "Tôi dùng file" → "Too ddugf file"
     */
    fun toTelex(text: String): String {
        val sb = StringBuilder(text.length + 16)
        for (ch in text) {
            val code = ch.code
            val t = if (code < TABLE_SIZE) {
                telexUpper[code] ?: telexLower[code]
            } else null

            if (t != null) sb.append(t) else sb.append(ch)
        }
        return sb.toString()
    }

    /**
     * Tự động xử lý text:
     *  - Nếu có ký tự Việt → chuyển Telex
     *  - Nếu không → giữ nguyên
     *
     * TỐI ƯU: Duyệt 1 lần duy nhất, không tách hasVietnamese + toTelex.
     * Nếu không có ký tự Việt, trả về text gốc, không tạo StringBuilder mới.
     */
    fun autoConvert(text: String): String {
        val sb = StringBuilder(text.length + 16)
        var hasViet = false

        for (ch in text) {
            val code = ch.code
            val t = if (code < TABLE_SIZE) {
                telexUpper[code] ?: telexLower[code]
            } else null

            if (t != null) {
                hasViet = true
                sb.append(t)
            } else {
                sb.append(ch)
            }
        }

        return if (hasViet) sb.toString() else text
    }
}