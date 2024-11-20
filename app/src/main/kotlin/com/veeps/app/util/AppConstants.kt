package com.veeps.app.util

import com.veeps.app.BuildConfig

object AppConstants {
	const val PREFS_FILENAME: String = "veeps"
	const val TAG: String = "VeepsAppTag"
	const val deviceType: String = "fireTv"
	const val deviceName: String = "Amazon Fire TV"

	const val clientId: String = "40a3903ae06585f0c114464750d605bba618afd01159f4eefef68c255c746a89"
	const val clientSecret: String =
		"0db660fe553f3fe5fba3f0c7270a7a20c31ef49f27167bbde9ce905777a5aaac"
	const val grantType: String = "urn:ietf:params:oauth:grant-type:device_code"
	private const val JWTSecretKey_Production: String =
		"27aSEvmPBBRQFrYsQcKJn8rJ0J0mBiiCZGeiZRNqV1Q/dJ7/rrXlbTxHHGJn9cNl"
	private const val JWTSecretKey_Staging: String =
		"xSISQv6jnL4lPgibQPrLdZWgtqYynARhSKKLiLK7ZjKrEA/i7x+MoEHTytVcnjP2"
	var secretKey = if (BuildConfig.isProduction) JWTSecretKey_Production else JWTSecretKey_Staging
	const val deviceModel: String = "device_model"
	const val deviceUniqueID: String = "device_unique_id"

	var lastKeyPressTime: Long = IntValue.NUMBER_0.toLong()
	const val keyPressLongDelayTime: Long = IntValue.NUMBER_333.toLong()
	const val keyPressShortDelayTime: Long = IntValue.NUMBER_100.toLong()

	const val isUserAuthenticated: String = "is_user_authenticated"
	const val authenticatedUserToken: String = "authenticated_user_token"
	const val generatedJWT: String = "generated_JWT"
	const val userID: String = "user_id"
	const val userSubscriptionStatus: String = "user_subscription_status"
	const val userCurrency: String = "user_currency"
	const val userTimeZone: String = "user_time_zone"
	const val userBeaconBaseURL: String = "user_beacon_base_url"
	const val userEmailAddress: String = "user_email_address"
	const val userFullName: String = "user_full_name"
	const val userDisplayName: String = "user_display_name"
	const val userAvatar: String = "user_avatar"
	const val userTimeZoneAbbr: String = "user_time_zone_abbr"
	const val drmLicenseURL = "https://widevine-dash.ezdrm.com/proxy?pX=72D27A"
	const val reservedId = "reservedId"
	const val receiptId = "receiptId"
	const val orderId = "orderId"
	const val requestId = "requestId"
	const val SKUId = "SKUId"
}

object Image {
	const val DEFAULT = "/upload/"
	const val HERO = "/upload/t_tv_hero/"
	const val LOGO = "/upload/t_tv_logo/"
	const val CARD = "/upload/t_tv_card_vertical/"
	const val CARD_HORIZONTAL = "/upload/t_tv_card/"
	const val CIRCLE = "/upload/t_tv_avatar/"
}

object ImageTags {
	const val DEFAULT = "DEFAULT"
	const val QR = "QR"
	const val AVATAR = "AVATAR"
	const val HERO = "HERO"
	const val CARD = "CARD"
	const val ARTIST_VENUE = "ARTIST_VENUE"
	const val LOGO = "LOGO"
}

enum class CardType {
	WIDE, CIRCLE, STANDARD, PORTRAIT, HERO
}

object CardTypes {
	const val WIDE = "wide"
	const val CIRCLE = "circle"
	const val STANDARD = "standard"
	const val CHART = "chart"
	const val LANDSCAPE = "landscape"
	const val PORTRAIT = "portrait"
	const val HERO = "hero"
	const val GENRE = "genre"
}

object IntValue {
	const val NUMBER_0 = 0
	const val NUMBER_1 = 1
	const val NUMBER_5 = 5
	const val NUMBER_8 = 8
	const val NUMBER_10 = 10
	const val NUMBER_LANDSCAPE = 9
	const val NUMBER_100 = 100
	const val NUMBER_200 = 200
	const val NUMBER_300 = 300
	const val NUMBER_333 = 333
	const val NUMBER_500 = 500
	const val NUMBER_1000 = 1000
	const val NUMBER_2000 = 2000
	const val NUMBER_5000 = 5000
	const val NUMBER_15000 = 15000
}

object PollingStatus {
	const val PENDING = "authorization_pending"
	const val SLOW_DOWN = "slow_down"
	const val EXPIRED_TOKEN = "expired_token"
}

object DEFAULT {
	const val EMPTY_STRING = ""
	const val EMPTY_INT = 0
	const val DOUBLE_VALUE = 0.0
	const val SEPARATOR = " · "
	const val DEFAULT_INT_STRING = "0"
}

object ContentBadgeValues {
	const val BADGE_5_1 = "5.1"
	const val BADGE_6_1 = "6.1"
	const val BADGE_7_1 = "7.1"
	const val BADGE_ATMOS = "ATMOS"
	const val BADGE_DIGITAL_PLUS = "DIGITAL PLUS"
	const val BADGE_DIGITAL = "DIGITAL"
	const val BADGE_TRUEHD = "TRUEHD"
	const val BADGE_VISION = "VISION"
	const val BADGE_GLOBAL = "GLOBAL"
	const val BADGE_DTS_HD = "DTS-HD"
	const val BADGE_DTS_X = "DTS:X"
	const val BADGE_1080P = "HD"
}

object ContentBadges {
	const val BADGE_5_1 = "5_1"
	const val BADGE_6_1 = "6_1"
	const val BADGE_7_1 = "7_1"
	const val BADGE_ATMOS = "ATMOS"
	const val BADGE_DOLBY_ATMOS = "dolby_atmos"
	const val BADGE_DOLBY_DIGITAL = "dolby_digital"
	const val BADGE_DOLBY_DIGITAL_PLUS = "dolby_digital_plus"
	const val BADGE_DOLBY_TRUEHD = "dolby_truehd"
	const val BADGE_DOLBY_VISION = "dolby_vision"
	const val BADGE_DOLBY_5_1 = "surround_sound_5_1"
	const val BADGE_DOLBY_6_1 = "surround_sound_6_1"
	const val BADGE_DOLBY_7_1 = "surround_sound_7_1"
	const val BADGE_DIGITAL_PLUS = "DIGITAL_PLUS"
	const val BADGE_DIGITAL = "DIGITAL"
	const val BADGE_TRUEHD = "TRUEHD"
	const val BADGE_VISION = "VISION"
	const val BADGE_GLOBAL = "GLOBAL"
	const val BADGE_DTS_HD = "DTS_HD"
	const val BADGE_DTS_X = "DTS_X"
	const val BADGE_1080P = "1080P"
}

object Screens {
	const val INTRO = "intro"
	const val SIGN_IN = "signIn"
	const val HOME = "HOME"
	const val PROFILE = "PROFILE"
	const val BROWSE = "BROWSE"
	const val WAITING_ROOM = "WAITING_ROOM"
	const val VIDEO = "VIDEO"
	const val SEARCH = "SEARCH"
	const val ARTIST = "ARTIST"
	const val VENUE = "VENUE"
	const val EVENT = "EVENT"
	const val SHOWS = "SHOWS"
	const val EXIT_APP = "EXIT_APP"
	const val PLAYER_ERROR = "PLAYER_ERROR"
	const val STREAM_END = "STREAM_END"
	const val APP_UPDATE = "APP_UPDATE"
	const val SUBSCRIPTION = "SUBSCRIPTION"
	const val GENRE = "GENRE"
}

object PurchaseResponseStatus {
	const val NONE = "NONE"
	const val SUCCESS = "SUCCESS"
	const val SUCCESS_WITH_PENDING_PURCHASE = "SUCCESS_WITH_PENDING_PURCHASE"
	const val FAILED = "FAILED"
	const val INVALID_SKU = "INVALID_SKU"
	const val ALREADY_PURCHASED = "ALREADY_PURCHASED"
	const val NOT_SUPPORTED = "NOT_SUPPORTED"
	const val PENDING = "PENDING"
	const val CANCELLED_BY_VEEPS = "CANCELLED_BY_VEEPS"
}

object EntityTypes {
	const val EVENT = "event"
	const val ARTIST = "artist"
	const val VENUE = "venue"
	const val GENRES = "genre"
}

object EventTypes {
	const val LIVE = "LIVE"
	const val UPCOMING = "UPCOMING"
	const val ON_DEMAND = "ON_DEMAND"
	const val EXPIRED = "EXPIRED"
	const val ENDED = "ENDED"
	const val ALLExpired = "All expired"
}

object LastSignalTypes {
	const val NO_SIGNAL = "no_signal"
	const val DISCONNECTED = "disconnected"
	const val CONNECTED = "connected"
	const val CONNECTING = "connecting"
	const val IDLE = "idle"
	const val ACTIVE = "active"
	const val RECORDING = "recording"
	const val STREAM_ENDED = "stream_ended"
	const val STREAM_RESTARTED = "stream_restarted"
	const val ON_DEMAND_READY = "on_demand_ready"
	const val VOD_READY = "VOD_ready"
	const val CHAT_MESSAGE_DELETED = "chat_message_deleted"
}

object BadgeStatus {
	const val DO_NOT_SHOW = "Do Not Show"
	const val TODAY = "Today"
	const val NOTHING = "Nothing"
	const val LIVE = "Live"
	const val EXPIRED = "Expired"
	const val ENDED = "Ended"
}

object EventAccessType {
	const val VEEPS_PLUS_PAID = "VEEPS_PLUS_PAID"
	const val VEEPS_PLUS = "VEEPS_PLUS"
	const val PAID = "PAID"
	const val FREE = "FREE"
	const val VEEPS_FREE = "VEEPS_FREE"
	const val NONE = "none"
	const val VEEPS_PARTNER = "veeps_partner"
}

object ButtonLabels {
	const val GO_TO_EVENT = "Go to Event"
	const val GET_TICKETS = "Get Tickets"
	const val UNAVAILABLE = "Unavailable"
	const val SOLD_OUT = "Sold Out"
	const val JOIN_LIVE = "Join Live"
	const val JOIN = "Join"
	const val PLAY = "Play"
	const val RESUME = "Resume"
	const val BUY_TICKET = "Buy Ticket${DEFAULT.SEPARATOR}"
	const val CLAIM_FREE_TICKET = "Claim Free Ticket"
}

object DateTimeCompareDifference {
	const val GREATER_THAN = "greater than"
	const val LESS_THAN = "less than"
	const val EQUALS = "equals"
	const val NOTHING = "NOTHING"
}

object UserType {
	const val VEEPS_PAID_SUBSCRIBER = "m"
	const val VEEPS_TICKETS_HOLDER = "b"
	const val VEEPS_FREE_TIER = "f"
}

object SubscriptionPlanSKUs {
	const val VEEPS_ALL_ACCESS_SUBSCRIPTION = "VP-ALL-ACCESS"
	const val VEEPS_MONTHLY_SUBSCRIPTION = "VP-M-US"
	const val VEEPS_YEARLY_SUBSCRIPTION = "VP-Y-US"
	const val MONTHLY_SUBSCRIPTION = "VP-M-77-US"
	const val MONTHLY_TERM_SUBSCRIPTION = "Monthly"
	const val YEARLY_SUBSCRIPTION = "VP-Y-89-US"
	const val YEARLY_TERM_SUBSCRIPTION = "Annually"
}

object PurchaseType {
	const val SUBSCRIPTION = "SUBSCRIPTION"
	const val ONE_TIME_PURCHASE = "ONE_TIME_PURCHASE"
}
object SubscriptionPlanDetails {
	const val MONTHLY_PLAN_NAME = "Monthly"
	const val YEARLY_PLAN_NAME = "Yearly"
	const val MONTHLY_PLAN_PRICE = "$11.99"
	const val YEARLY_PLAN_PRICE = "$120"
	const val PLAN_BENEFIT_ONE = "See every live show and rewatch for longer"
	const val PLAN_BENEFIT_TWO = "Enjoy 3,000 hours of on-demand shows"
	const val PLAN_BENEFIT_THREE = "Unlock exclusive content from your favorite artists"
	const val PLAN_BENEFIT_FOUR = "Stream on your browser, Apple TV, Roku, iOS, Fire TV, Samsung and Android"
}

object MarketPlace {
	const val US = "US"
}

object GenreName {
	const val COMEDY = "Comedy"
	const val COUNTRY = "Country"
	const val INDIE = "Indie"
	const val POP = "Pop"
	const val K_POP = "K-Pop"
	const val RAP = "Rap"
	const val FOLK = "Folk"
	const val ROCK = "Rock"
	const val ALTERNATIVE = "Alternative"
	const val METAL = "Metal"
	const val JAZZ = "Jazz"
	const val R_AND_B = "R&B"
}