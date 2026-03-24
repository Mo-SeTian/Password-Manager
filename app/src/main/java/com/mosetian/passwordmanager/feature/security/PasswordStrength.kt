package com.mosetian.passwordmanager.feature.security

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.ShieldMoon
import androidx.compose.material.icons.rounded.ShieldAlert
import androidx.compose.material.icons.rounded.ShieldCheck
import androidx.compose.material.icons.rounded.ShieldLock
import androidx.compose.material.icons.rounded.Warning

/**
 * 密码强度枚举，按安全从低到高排序。
 */
enum class PasswordStrength(
    val label: String,
    val description: String,
    val icon: ImageVector,
    val color: Color,
    val level: Int  // 1=极弱 2=弱 3=中等 4=强 5=极强
) {
    VERY_WEAK(
        label = "极弱",
        description = "建议重设，极易被破解",
        icon = Icons.Rounded.ShieldAlert,
        color = Color(0xFFD32F2F),
        level = 1
    ),
    WEAK(
        label = "弱",
        description = "建议重设，强度不足",
        icon = Icons.Rounded.Warning,
        color = Color(0xFFE64A19),
        level = 2
    ),
    FAIR(
        label = "中等",
        description = "基本可用，建议加强",
        icon = Icons.Rounded.ShieldMoon,
        color = Color(0xFFF57C00),
        level = 3
    ),
    GOOD(
        label = "良好",
        description = "推荐使用",
        icon = Icons.Rounded.ShieldLock,
        color = Color(0xFF388E3C),
        level = 4
    ),
    STRONG(
        label = "极强",
        description = "安全度高",
        icon = Icons.Rounded.ShieldCheck,
        color = Color(0xFF1B5E20),
        level = 5
    );

    companion object {
        /**
         * 根据密码字符串评估强度。
         *
         * 评分维度：
         * - 长度（8/10/14/18 四档）
         * - 字符种类（大写、小写、数字、特殊符号）
         * - 模式检测（重复字符、顺序字符、键盘序、纯数字/纯字母）
         */
        fun evaluate(password: String): PasswordStrength {
            if (password.isEmpty()) return VERY_WEAK

            val len = password.length
            val hasLower = password.any { it.isLowerCase() }
            val hasUpper = password.any { it.isUpperCase() }
            val hasDigit = password.any { it.isDigit() }
            val hasSpecial = password.any { !it.isLetterOrDigit() }

            val charTypes = listOf(hasLower, hasUpper, hasDigit, hasSpecial).count { it }

            // 模式检测：提前返回更低的评级
            if (isSequential(password)) return WEAK
            if (isRepeatedChars(password)) return WEAK
            if (isKeyboardPattern(password)) return WEAK

            // 纯数字或纯字母
            if (hasDigit && !hasLower && !hasUpper && !hasSpecial) return when {
                len < 6 -> VERY_WEAK
                len < 10 -> WEAK
                else -> FAIR
            }
            if (hasLower && !hasUpper && !hasDigit && !hasSpecial) return when {
                len < 10 -> WEAK
                len < 14 -> FAIR
                else -> GOOD
            }
            if (hasUpper && hasLower && !hasDigit && !hasSpecial) return when {
                len < 10 -> WEAK
                len < 14 -> FAIR
                else -> GOOD
            }

            // 综合评分
            val score = evaluateScore(password, len, charTypes)

            return when {
                score >= 85 -> STRONG
                score >= 65 -> GOOD
                score >= 40 -> FAIR
                score >= 20 -> WEAK
                else -> VERY_WEAK
            }
        }

        private fun evaluateScore(password: String, len: Int, charTypes: Int): Int {
            var score = 0

            // 长度得分（上限 40）
            score += when {
                len >= 20 -> 40
                len >= 16 -> 35
                len >= 14 -> 30
                len >= 12 -> 25
                len >= 10 -> 18
                len >= 8 -> 12
                len >= 6 -> 6
                else -> 2
            }

            // 字符种类得分（上限 30）
            score += charTypes * 7

            // 熵相关奖励（上限 30）
            val uniqueChars = password.toSet().size
            score += ((uniqueChars.toFloat() / len) * 20).toInt().coerceAtMost(20)

            // 长度奖励（超过 14 字符额外加 10）
            if (len > 14) score += 10

            return score.coerceIn(0, 100)
        }

        /**
         * 检测连续字符（abc, 123 等）
         */
        private fun isSequential(password: String): Boolean {
            if (password.length < 3) return false
            val lower = password.lowercase()
            for (i in 0 until lower.length - 2) {
                val a = lower[i].code
                val b = lower[i + 1].code
                val c = lower[i + 2].code
                // 升序连续
                if (b == a + 1 && c == b + 1) return true
                // 降序连续
                if (b == a - 1 && c == b - 1) return true
            }
            return false
        }

        /**
         * 检测重复字符（aaa, 111 等）
         */
        private fun isRepeatedChars(password: String): Boolean {
            if (password.length < 3) return false
            for (i in 0 until password.length - 2) {
                if (password[i] == password[i + 1] && password[i + 1] == password[i + 2]) return true
            }
            return false
        }

        /**
         * 检测键盘模式（qwerty, asdf, zxcv 等）
         */
        private fun isKeyboardPattern(password: String): Boolean {
            val lower = password.lowercase()
            val patterns = listOf(
                "qwerty", "qwertz", "azerty", "asdf", "zxcv",
                "qazwsx", "1234qwer", "1qaz", "2wsx", "3edc",
                "qweasd", "asdzxc", "zaqwsx", "qweasdzxc"
            )
            for (pattern in patterns) {
                if (lower.contains(pattern)) return true
            }
            // 键盘行连续 5 个以上
            val keyboardRows = listOf(
                "qwertyuiop",
                "asdfghjkl",
                "zxcvbnm"
            )
            for (row in keyboardRows) {
                var count = 0
                for (i in 0 until lower.length) {
                    val idx = row.indexOf(lower[i])
                    if (idx >= 0) {
                        count++
                        if (count >= 5) return true
                    } else {
                        count = 0
                    }
                }
            }
            return false
        }

        /**
         * 判断密码是否"弱"——应进入弱密码分组
         */
        fun isWeak(password: String): Boolean {
            return evaluate(password).level <= 2
        }
    }
}
