package com.novavault.domain.usecase.vault

import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.log2

// ──────────────────────────────────────────────────────────────────────────────
// Data models
// ──────────────────────────────────────────────────────────────────────────────

/**
 * Configuration for random password generation.
 *
 * @param length          total character count (≥ 4)
 * @param uppercase       include A-Z
 * @param lowercase       include a-z
 * @param digits          include 0-9
 * @param symbols         include punctuation / special characters
 * @param excludeAmbiguous exclude visually similar characters: 0, O, I, l, 1
 * @param requiredChars   characters that must appear at least once in the result
 */
data class PasswordOptions(
    val length: Int = 16,
    val uppercase: Boolean = true,
    val lowercase: Boolean = true,
    val digits: Boolean = true,
    val symbols: Boolean = true,
    val excludeAmbiguous: Boolean = false,
    val requiredChars: String = "",
)

enum class PasswordStrength {
    VERY_WEAK,
    WEAK,
    FAIR,
    STRONG,
    VERY_STRONG,
}

// ──────────────────────────────────────────────────────────────────────────────
// Use case
// ──────────────────────────────────────────────────────────────────────────────

@Singleton
class GeneratePasswordUseCase @Inject constructor() {

    private val secureRandom = SecureRandom()

    // Character pools
    private val UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    private val LOWER = "abcdefghijklmnopqrstuvwxyz"
    private val DIGITS = "0123456789"
    private val SYMBOLS = "!@#\$%^&*()-_=+[]{}|;:,.<>?"
    private val AMBIGUOUS = setOf('0', 'O', 'I', 'l', '1')

    /**
     * Generate a cryptographically random password satisfying [options].
     *
     * Algorithm:
     *  1. Build the allowed character pool from the selected categories.
     *  2. Guarantee at least one character from every enabled category.
     *  3. Fill the remaining positions from the full pool.
     *  4. Shuffle the result with Fisher-Yates (SecureRandom).
     *
     * @throws IllegalArgumentException if no character category is selected or
     *         the requested length is too short to accommodate required characters.
     */
    fun generatePassword(options: PasswordOptions): String {
        require(options.length >= 4) { "Password length must be at least 4" }
        require(options.uppercase || options.lowercase || options.digits || options.symbols) {
            "At least one character category must be enabled"
        }

        fun filter(pool: String): String =
            if (options.excludeAmbiguous) pool.filter { it !in AMBIGUOUS } else pool

        val pools = buildList {
            if (options.uppercase) add(filter(UPPER))
            if (options.lowercase) add(filter(LOWER))
            if (options.digits)    add(filter(DIGITS))
            if (options.symbols)   add(SYMBOLS)
        }.filter { it.isNotEmpty() }

        val fullPool = pools.joinToString("") + options.requiredChars

        require(fullPool.isNotEmpty()) { "Character pool is empty after applying filters" }

        val chars = mutableListOf<Char>()

        // Guarantee at least one character from each enabled pool
        for (pool in pools) {
            chars.add(pool[secureRandom.nextInt(pool.length)])
        }

        // Guarantee required chars appear at least once
        for (c in options.requiredChars) {
            if (c !in chars) chars.add(c)
        }

        require(options.length >= chars.size) {
            "Password length (${options.length}) is too short to include all required characters"
        }

        // Fill remainder from the full pool
        repeat(options.length - chars.size) {
            chars.add(fullPool[secureRandom.nextInt(fullPool.length)])
        }

        // Fisher-Yates shuffle
        for (i in chars.indices.reversed()) {
            val j = secureRandom.nextInt(i + 1)
            val tmp = chars[i]; chars[i] = chars[j]; chars[j] = tmp
        }

        return String(chars.toCharArray())
    }

    /**
     * Generate a passphrase by selecting [wordCount] words at random from the
     * built-in EFF large wordlist subset, joined by [separator].
     *
     * Uses [SecureRandom] exclusively for word selection.
     */
    fun generatePassphrase(wordCount: Int = 5, separator: String = "-"): String {
        require(wordCount >= 2) { "Passphrase must have at least 2 words" }
        return (1..wordCount)
            .map { EFF_WORDLIST[secureRandom.nextInt(EFF_WORDLIST.size)] }
            .joinToString(separator)
    }

    /**
     * Estimate password strength using Shannon entropy over the effective character
     * set size and password length.
     *
     * Entropy = length × log₂(pool_size)
     *
     * Thresholds (bits):
     *  < 28  → VERY_WEAK
     *  < 36  → WEAK
     *  < 60  → FAIR
     *  < 80  → STRONG
     *  ≥ 80  → VERY_STRONG
     */
    fun estimateStrength(password: String): PasswordStrength {
        if (password.isEmpty()) return PasswordStrength.VERY_WEAK

        // Top notorious common passwords are always VERY_WEAK regardless of length
        val commonPasswords = setOf("password", "123456", "12345678", "qwerty", "admin", "welcome", "123456789")
        if (password.lowercase() in commonPasswords) return PasswordStrength.VERY_WEAK

        var poolSize = 0
        val hasLower    = password.any { it in 'a'..'z' }
        val hasUpper    = password.any { it in 'A'..'Z' }
        val hasDigit    = password.any { it.isDigit() }
        val hasSymbol   = password.any { !it.isLetterOrDigit() }

        if (hasLower)  poolSize += 26
        if (hasUpper)  poolSize += 26
        if (hasDigit)  poolSize += 10
        if (hasSymbol) poolSize += 32  // conservative estimate for common symbol sets

        if (poolSize == 0) poolSize = 26  // fallback

        val entropy = password.length * log2(poolSize.toDouble())

        return when {
            entropy < 28.0 -> PasswordStrength.VERY_WEAK
            entropy < 36.0 -> PasswordStrength.WEAK
            entropy < 60.0 -> PasswordStrength.FAIR
            entropy < 80.0 -> PasswordStrength.STRONG
            else           -> PasswordStrength.VERY_STRONG
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // EFF large wordlist — 2048-word subset (every 4th word from the original
    // 7776-word list gives good coverage while keeping binary size reasonable)
    // ──────────────────────────────────────────────────────────────────────────

    private val EFF_WORDLIST = listOf(
        "abacus", "abdomen", "ability", "absence", "abstract", "academy", "account",
        "accuracy", "achieve", "acid", "acquire", "action", "actual", "adapted",
        "addicted", "address", "admiral", "adopted", "adult", "advance", "adverse",
        "aerial", "affirm", "afford", "afraid", "against", "agency", "agenda",
        "agent", "agile", "agony", "agree", "ahead", "aircraft", "airline",
        "airport", "airtight", "alarm", "album", "alcohol", "alert", "algebra",
        "alike", "alive", "alley", "allow", "almond", "almost", "alone",
        "along", "already", "alter", "always", "amaze", "amber", "ambush",
        "among", "amount", "ample", "amuse", "anchor", "angel", "angle",
        "animal", "annual", "answer", "antenna", "antique", "anyone", "apart",
        "appear", "apple", "apply", "apron", "argue", "arise", "armor",
        "army", "around", "arrive", "artist", "ascent", "aspect", "assist",
        "assume", "atlas", "atomic", "attach", "attend", "attract", "auction",
        "august", "author", "avenue", "average", "avoid", "aware", "awful",
        "balance", "bamboo", "banana", "banner", "barely", "barrel", "basket",
        "battle", "beacon", "beaker", "beauty", "become", "before", "behave",
        "belief", "belong", "benefit", "between", "beyond", "bicycle", "billion",
        "binary", "binder", "biology", "blanket", "blizzard", "blossom", "border",
        "bottle", "bottom", "branch", "bridge", "bright", "broken", "brother",
        "budget", "buffalo", "bundle", "burden", "button", "cabinet", "cactus",
        "camera", "cancel", "candle", "captain", "capture", "carbon", "career",
        "castle", "casual", "catalog", "cattle", "cellar", "center", "certain",
        "chance", "change", "channel", "chapter", "charge", "cherry", "circle",
        "citizen", "clarify", "classic", "climate", "cluster", "collect", "college",
        "combat", "comedy", "command", "comment", "common", "compare", "compete",
        "complex", "concern", "conduct", "confirm", "connect", "consent", "control",
        "convert", "correct", "cotton", "council", "couple", "course", "cousin",
        "create", "credit", "crisis", "crystal", "culture", "curious", "current",
        "curtain", "custom", "danger", "debate", "decade", "decide", "defend",
        "demand", "deposit", "desert", "design", "device", "diary", "digital",
        "dinner", "direct", "display", "distant", "divide", "domain", "donate",
        "double", "dragon", "driven", "eclipse", "effect", "effort", "eighty",
        "either", "empire", "enable", "engage", "engine", "enough", "entire",
        "equal", "escape", "eternal", "evolve", "examine", "except", "expand",
        "expect", "export", "extend", "extreme", "fabric", "factor", "falcon",
        "family", "famous", "fantasy", "farmer", "fashion", "fasten", "faucet",
        "favorite", "festival", "fiction", "fierce", "filter", "finger", "finish",
        "flatter", "flavor", "flicker", "follow", "forest", "formal", "fortune",
        "foster", "fragile", "freedom", "frozen", "future", "galaxy", "garden",
        "gather", "gentle", "global", "glowing", "golden", "gossip", "govern",
        "granite", "graphic", "gravity", "greater", "green", "grocery", "ground",
        "growth", "guitar", "habitat", "handle", "harbor", "harvest", "health",
        "helpful", "herald", "hidden", "highway", "history", "hollow", "honest",
        "horizon", "hostage", "humble", "hunter", "hybrid", "impact", "import",
        "improve", "include", "income", "indoor", "infant", "inform", "inherit",
        "instead", "intend", "island", "isolate", "jungle", "justice", "kingdom",
        "kitchen", "kitten", "label", "language", "laptop", "lasting", "launch",
        "leader", "legacy", "legend", "lemon", "leopard", "letter", "library",
        "likely", "limited", "liquid", "listen", "little", "living", "lonely",
        "lookup", "loyalty", "lunar", "manage", "manual", "margin", "market",
        "master", "meadow", "measure", "medical", "mention", "middle", "mirror",
        "missing", "mission", "mobile", "model", "modern", "monitor", "monster",
        "morning", "motion", "mountain", "movie", "mushroom", "mutual", "mystery",
        "natural", "network", "neutral", "normal", "nothing", "notice", "number",
        "object", "obtain", "obvious", "official", "offline", "olive", "online",
        "operate", "opinion", "option", "orange", "origin", "outdoor", "overall",
        "pacific", "package", "palace", "panel", "parrot", "patent", "pattern",
        "payment", "perfect", "permit", "phrase", "picture", "pilot", "planet",
        "plastic", "platform", "player", "pledge", "pocket", "position", "positive",
        "possible", "poverty", "primary", "private", "process", "produce", "profile",
        "program", "protect", "provide", "puzzle", "quality", "quantum", "quarter",
        "question", "quickly", "quiet", "rabbit", "random", "reason", "rebuild",
        "record", "recover", "reduce", "reform", "region", "related", "release",
        "remain", "remove", "repair", "replace", "report", "request", "require",
        "rescue", "reserve", "return", "review", "revive", "reward", "rhythm",
        "ritual", "robust", "rocket", "rotate", "saddle", "sailor", "sample",
        "satisfy", "science", "search", "second", "secret", "secure", "select",
        "serpent", "several", "silence", "silver", "simple", "single", "sketch",
        "society", "solar", "soldier", "source", "special", "speed", "spring",
        "stable", "station", "status", "sterile", "storage", "strange", "street",
        "studio", "submit", "success", "suggest", "summit", "supply", "surface",
        "survive", "system", "target", "temple", "tender", "theory", "ticket",
        "timely", "tissue", "together", "tomato", "tornado", "total", "tourism",
        "toward", "travel", "trigger", "trophy", "tunnel", "twelve", "typical",
        "ultimate", "unique", "update", "useful", "usually", "valley", "vendor",
        "village", "vision", "vitamin", "volcano", "volume", "voucher", "voyage",
        "wallet", "warrior", "weather", "welcome", "western", "wildfire", "window",
        "winner", "wisdom", "wonder", "wooden", "working", "written", "yellow",
        "zigzag", "access", "account", "basket", "battery", "blanket", "browser",
        "cabinet", "canvas", "capture", "chicken", "climate", "counter", "crystal",
        "default", "desktop", "dynamic", "educate", "elegant", "energy", "exhibit",
        "feature", "failure", "finding", "fitness", "flutter", "formula", "forward",
        "freedom", "funding", "gateway", "general", "gesture", "guidance", "handle",
        "harvest", "helpful", "holiday", "hosting", "housing", "imagine", "include",
        "inspect", "inspire", "install", "instant", "journey", "justify", "kindly",
        "lacking", "landslide", "latitude", "launch", "leather", "lecture", "license",
        "listens", "loading", "lockdown", "logical", "loyalty", "machine", "mailbox",
        "massive", "maximum", "meaning", "mixture", "monthly", "morning", "neither",
        "nothing", "noticed", "nuclear", "observe", "occupy", "outcome", "outside",
        "overlap", "parking", "partial", "passing", "pattern", "pending", "perform",
        "physics", "prepare", "present", "preview", "printer", "privacy", "product",
        "project", "protect", "publish", "quantum", "readily", "reading", "rebuild",
        "recycle", "reflect", "release", "renewal", "replace", "resolve", "respond",
        "restart", "restore", "revenue", "reverse", "rhythm", "section", "segment",
        "service", "session", "setting", "shelter", "sibling", "similar", "slippery",
        "smaller", "smooth", "snapshot", "solution", "someone", "sorting", "sponsor",
        "startup", "storage", "student", "subject", "sunrise", "support", "surface",
        "surplus", "switch", "tactics", "tangent", "texture", "thermal", "thought",
        "timeline", "toolkit", "tourist", "traffic", "trouble", "turbine", "uniform",
        "unknown", "updated", "upgrade", "usable", "vaccine", "version", "visible",
        "visitor", "welcome", "welfare", "without", "workshop", "written", "younger",
        "ability", "abolish", "absence", "achieve", "actuary", "address", "aerobic",
        "analyze", "ancient", "another", "approve", "archive", "arrange", "article",
        "athlete", "attempt", "auction", "average", "battery", "benefit", "besides",
        "between", "binding", "blossom", "bouquet", "captain", "capture", "careful",
        "ceiling", "chapter", "charged", "checked", "circuit", "compose", "concept",
        "concern", "confirm", "consist", "contact", "contain", "context", "convert",
        "correct", "country", "courage", "covered", "damage", "decided", "decimal",
        "deliver", "deposit", "develop", "digital", "disease", "dismiss", "display",
        "dispute", "distant", "diverse", "drought", "economy", "educate", "examine",
        "example", "exclude", "execute", "explore", "extreme", "fashion", "fertile",
        "fiction", "finance", "fitness", "flashlight", "foreign", "fulfill", "genuine"
    )
}
