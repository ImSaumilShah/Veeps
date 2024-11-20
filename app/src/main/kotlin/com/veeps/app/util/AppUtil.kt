package com.veeps.app.util

import com.veeps.app.R
import com.veeps.app.application.Veeps
import com.veeps.app.feature.contentRail.model.Entities
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.Days
import org.joda.time.Hours
import org.joda.time.Minutes
import java.util.concurrent.TimeUnit


object AppUtil {

	fun generateJWT(eventIds: String): String {
		val builder = Jwts.builder()
		builder.setAudience(eventIds)
		builder.claim("exp", TimeUnit.MILLISECONDS.toSeconds(DateTime().plusMinutes(30).millis))
		builder.claim("vsn", 2)
		builder.claim("sty", "event")
		builder.claim("sid", eventIds)
		builder.claim("iss", "veeps-firetv")
		builder.claim("sub", AppPreferences.get(AppConstants.userID, ""))
		builder.signWith(SignatureAlgorithm.HS256, AppConstants.secretKey.toByteArray())
		return builder.compact()
	}

	fun getFilterFor(eventType: String): ArrayList<(Entities) -> Boolean> {
		return when (eventType) {
			EventTypes.LIVE -> {
				arrayListOf(
					{
						(it.status ?: DEFAULT.EMPTY_STRING).contains(
							EventTypes.LIVE, true
						) && (it.watchStatus ?: DEFAULT.EMPTY_STRING).contains("watchable", true)
					},
				)
			}

			EventTypes.UPCOMING -> {
				arrayListOf(
					{
						(it.status ?: DEFAULT.EMPTY_STRING).contains(
							EventTypes.LIVE, true
						) && !(it.watchStatus ?: DEFAULT.EMPTY_STRING).contains(
							"watchable", true
						) && !(it.watchStatus ?: DEFAULT.EMPTY_STRING).contains("expired", true)
					},
					{ (it.status ?: DEFAULT.EMPTY_STRING).contains(EventTypes.UPCOMING, true) },
					{
						(it.status ?: DEFAULT.EMPTY_STRING).contains(
							EventTypes.ON_DEMAND, true
						) && (it.watchUntil ?: DEFAULT.EMPTY_STRING).isNotBlank() && compare(
							(it.watchUntil ?: DEFAULT.EMPTY_STRING)
						) == DateTimeCompareDifference.GREATER_THAN && !(it.watchStatus
							?: DEFAULT.EMPTY_STRING).contains(
							"watchable_ondemand", true
						)
					},
					{
						(it.status ?: DEFAULT.EMPTY_STRING).contains(
							EventTypes.ENDED, true
						) && !(it.watchStatus ?: DEFAULT.EMPTY_STRING).contains("expired", true)
					},
				)
			}

			EventTypes.ON_DEMAND -> {
				arrayListOf(
					{
						(it.status ?: DEFAULT.EMPTY_STRING).contains(
							EventTypes.ON_DEMAND, true
						) && (it.watchUntil ?: DEFAULT.EMPTY_STRING).isBlank()
					},
					{
						(it.status ?: DEFAULT.EMPTY_STRING).contains(
							EventTypes.ON_DEMAND, true
						) && (it.watchUntil ?: DEFAULT.EMPTY_STRING).isNotBlank() && compare(
							(it.watchUntil ?: DEFAULT.EMPTY_STRING)
						) == DateTimeCompareDifference.GREATER_THAN && (it.watchStatus
							?: DEFAULT.EMPTY_STRING).contains(
							"watchable_ondemand", true
						)
					},
				)
			}

			EventTypes.EXPIRED -> {
				arrayListOf(
					{
						(it.status ?: DEFAULT.EMPTY_STRING).contains(
							EventTypes.LIVE, true
						) && (it.watchStatus ?: DEFAULT.EMPTY_STRING).contains("expired", true)
					},
					{
						(it.status ?: DEFAULT.EMPTY_STRING).contains(
							EventTypes.ON_DEMAND, true
						) && (it.watchUntil ?: DEFAULT.EMPTY_STRING).isNotBlank() && compare(
							(it.watchUntil ?: DEFAULT.EMPTY_STRING)
						) != DateTimeCompareDifference.GREATER_THAN
					},
					{
						(it.status ?: DEFAULT.EMPTY_STRING).contains(
							EventTypes.ENDED, true
						) && (it.watchStatus ?: DEFAULT.EMPTY_STRING).contains("expired", true)
					},
				)
			}

			else -> {
				arrayListOf()
			}
		}
	}

	fun compare(dateString: String?): String {
		return if (dateString.isNullOrBlank()) {
			DateTimeCompareDifference.NOTHING
		} else {
			val currentDate = DateTime.now()
			val otherDate =
				DateTime(dateString, DateTimeZone.UTC).withZone(DateTimeZone.getDefault())
					.toDateTime()
			if (otherDate.isBefore(currentDate)) {
				DateTimeCompareDifference.LESS_THAN
			} else {
				if (otherDate.isEqual(currentDate)) {
					DateTimeCompareDifference.EQUALS
				} else {
					DateTimeCompareDifference.GREATER_THAN
				}
			}
		}
	}

	fun compare(otherDate: DateTime, currentDate: DateTime): String {
		return if (otherDate.isBefore(currentDate)) {
			DateTimeCompareDifference.LESS_THAN
		} else {
			if (otherDate.isEqual(currentDate)) {
				DateTimeCompareDifference.EQUALS
			} else {
				DateTimeCompareDifference.GREATER_THAN
			}
		}
	}

	private fun formatExpiredDate(otherDate: DateTime, currentDate: DateTime): String {
		val prefix = "Expires in"
		var postfix: String //= "s"
		var difference: Int //= Seconds.secondsBetween(currentDate, otherDate).seconds
//		if (difference > 59) {
		postfix = "m"
		difference = Minutes.minutesBetween(currentDate, otherDate).minutes
		if (difference > 59) {
			postfix = "h"
			difference = Hours.hoursBetween(currentDate, otherDate).hours
//			if (difference > 1) postfix += "s"
			if (difference > 23) {
				postfix = "d"
				difference = Days.daysBetween(currentDate, otherDate).days
//				if (difference > 1) postfix += "s"
//				if (difference > 6) {
//					postfix = "week"
//					difference = Weeks.weeksBetween(currentDate, otherDate).weeks
//					if (difference > 1) postfix += "s"
//					if (difference > 3) {
//						postfix = "month"
//						difference = Months.monthsBetween(currentDate, otherDate).months
//						if (difference > 1) postfix += "s"
//						if (difference > 11) {
//							postfix = "year"
//							difference = Years.yearsBetween(currentDate, otherDate).years
//							if (difference > 1) postfix += "s"
//						}
//					}
//				}
			}
		}
//		}
		return "$prefix $difference$postfix"
	}

	fun getBadgeStatus(
		entity: Entities, isContinueWatching: Boolean, isShowsScreen: Boolean
	): String {
		val currentDate = DateTime.now()
		val streamStartAt =
			entity.eventStreamStartsAt?.ifBlank { currentDate.toString() } ?: currentDate.toString()
		val watchUntil =
			entity.watchUntil?.ifBlank { currentDate.toString() } ?: currentDate.toString()
		val status = entity.status ?: DEFAULT.EMPTY_STRING
		val watchStatus = entity.watchStatus ?: DEFAULT.EMPTY_STRING
		val onSaleStatus = entity.onSaleStatus?.lowercase() ?: DEFAULT.EMPTY_STRING

		val streamStartsAtDate =
			DateTime(streamStartAt, DateTimeZone.UTC).withZone(DateTimeZone.getDefault())
				.toDateTime()
		val watchUntilDate =
			DateTime(watchUntil, DateTimeZone.UTC).withZone(DateTimeZone.getDefault()).toDateTime()

		return if (isContinueWatching) {
			if (compare(
					streamStartsAtDate, currentDate
				) == DateTimeCompareDifference.GREATER_THAN
			) {
				if (Days.daysBetween(
						currentDate.toLocalDate(), streamStartsAtDate.toLocalDate()
					).days == 0
				) {
					BadgeStatus.DO_NOT_SHOW//BadgeStatus.TODAY
				} else {
					BadgeStatus.DO_NOT_SHOW//streamStartsAtDate.toString("MMM dd")
				}
			} else {
				if (watchUntil.isBlank() || watchUntil == currentDate.toString()) {
					BadgeStatus.NOTHING
				} else {
					formatExpiredDate(watchUntilDate, currentDate)
				}
			}
		} else if (isShowsScreen) {
			if (status.contains(EventTypes.LIVE, true)) {
				if (watchStatus.contains("watchable", true)) {
					BadgeStatus.LIVE
				} else if (watchStatus.contains("expired", true)) {
					BadgeStatus.EXPIRED
				} else {
					if (Days.daysBetween(
							currentDate.toLocalDate(), streamStartsAtDate.toLocalDate()
						).days == 0
					) {
						BadgeStatus.TODAY
					} else {
						streamStartsAtDate.toString("MMM dd")
					}
				}
			} else if (status.contains(EventTypes.UPCOMING, true)) {
				if (Days.daysBetween(
						currentDate.toLocalDate(), streamStartsAtDate.toLocalDate()
					).days == 0
				) {
					BadgeStatus.TODAY
				} else {
					streamStartsAtDate.toString("MMM dd")
				}
			} else if (status.contains(EventTypes.ON_DEMAND, true)) {
				if (watchUntil.isBlank() || watchUntil == currentDate.toString()) {
					BadgeStatus.NOTHING
				} else if (compare(
						watchUntilDate, currentDate
					) == DateTimeCompareDifference.GREATER_THAN
				) {
					if (watchStatus.contains("watchable_ondemand", true)) {
						formatExpiredDate(watchUntilDate, currentDate)
					} else {
						if (Days.daysBetween(
								currentDate.toLocalDate(), streamStartsAtDate.toLocalDate()
							).days == 0
						) {
							BadgeStatus.DO_NOT_SHOW//TODAY
						} else {
							BadgeStatus.DO_NOT_SHOW//streamStartsAtDate.toString("MMM dd")
						}
					}
				} else {
					BadgeStatus.EXPIRED
				}
			} else if (status.contains(EventTypes.ENDED, true)) {
				if (watchStatus.contains("expired", true)) {
					BadgeStatus.EXPIRED
				} else {
					if (Days.daysBetween(
							currentDate.toLocalDate(), streamStartsAtDate.toLocalDate()
						).days == 0
					) {
						BadgeStatus.DO_NOT_SHOW//BadgeStatus.TODAY
					} else {
						BadgeStatus.DO_NOT_SHOW//streamStartsAtDate.toString("MMM dd")
					}
				}
			} else {
				BadgeStatus.EXPIRED
			}
		} else {
			if (status.contains(EventTypes.LIVE, true)) {
				BadgeStatus.NOTHING
			} else if ((status.contains(
					EventTypes.ON_DEMAND, true
				) && onSaleStatus == "off_sale_ondemand") || (status.contains(
					EventTypes.ENDED, true
				))
			) {
				BadgeStatus.ENDED
			} else if (status.contains(EventTypes.ON_DEMAND, true)) {
				BadgeStatus.DO_NOT_SHOW
			} else {
				if (Days.daysBetween(
						currentDate.toLocalDate(), streamStartsAtDate.toLocalDate()
					).days == 0
				) {
					BadgeStatus.TODAY
				} else {
					streamStartsAtDate.toString("MMM dd")
				}
			}
		}
	}

	fun calculateReWatchTime(hours: Int): String {
		return if (hours <= 48) {
			"$hours Hour${if (hours > 1) "s" else ""}"
		} else if (hours in 49..168) {
			"${hours / 24} Days"
		} else if (hours in 169..720) {
			"${hours / 168} Week${if ((hours / 168) > 1) "s" else ""}"
		} else {
			"${hours / 720} Month${if ((hours / 720) > 1) "s" else ""}"
		}
	}

	fun getContentBadge(contentBadgeList: ArrayList<String>): String {
		val contentBadges: ArrayList<String> = arrayListOf()
		if (contentBadgeList.isNotEmpty()) {
			contentBadgeList.forEach { badge ->
				when (badge.uppercase()) {
					ContentBadges.BADGE_5_1 -> contentBadges.add(ContentBadgeValues.BADGE_5_1)
					ContentBadges.BADGE_DOLBY_5_1 -> contentBadges.add(ContentBadgeValues.BADGE_5_1)
					ContentBadges.BADGE_6_1 -> contentBadges.add(ContentBadgeValues.BADGE_6_1)
					ContentBadges.BADGE_DOLBY_6_1 -> contentBadges.add(ContentBadgeValues.BADGE_6_1)
					ContentBadges.BADGE_7_1 -> contentBadges.add(ContentBadgeValues.BADGE_7_1)
					ContentBadges.BADGE_DOLBY_7_1 -> contentBadges.add(ContentBadgeValues.BADGE_7_1)
					ContentBadges.BADGE_ATMOS -> contentBadges.add(ContentBadgeValues.BADGE_ATMOS)
					ContentBadges.BADGE_DOLBY_ATMOS -> contentBadges.add(ContentBadgeValues.BADGE_ATMOS)
					ContentBadges.BADGE_DIGITAL_PLUS -> contentBadges.add(ContentBadgeValues.BADGE_DIGITAL_PLUS)
					ContentBadges.BADGE_DOLBY_DIGITAL_PLUS -> contentBadges.add(ContentBadgeValues.BADGE_DIGITAL_PLUS)
					ContentBadges.BADGE_DIGITAL -> contentBadges.add(ContentBadgeValues.BADGE_DIGITAL)
					ContentBadges.BADGE_DOLBY_DIGITAL -> contentBadges.add(ContentBadgeValues.BADGE_DIGITAL)
					ContentBadges.BADGE_TRUEHD -> contentBadges.add(ContentBadgeValues.BADGE_TRUEHD)
					ContentBadges.BADGE_DOLBY_TRUEHD -> contentBadges.add(ContentBadgeValues.BADGE_TRUEHD)
					ContentBadges.BADGE_VISION -> contentBadges.add(ContentBadgeValues.BADGE_VISION)
					ContentBadges.BADGE_DOLBY_VISION -> contentBadges.add(ContentBadgeValues.BADGE_VISION)
					ContentBadges.BADGE_GLOBAL -> contentBadges.add(ContentBadgeValues.BADGE_GLOBAL)
					ContentBadges.BADGE_DTS_HD -> contentBadges.add(ContentBadgeValues.BADGE_DTS_HD)
					ContentBadges.BADGE_DTS_X -> contentBadges.add(ContentBadgeValues.BADGE_DTS_X)
					ContentBadges.BADGE_1080P -> contentBadges.add(ContentBadgeValues.BADGE_1080P)
					else -> contentBadges.add(badge)
				}
			}
		}
		return contentBadges.joinToString(separator = DEFAULT.SEPARATOR)
			.ifBlank { ContentBadges.BADGE_GLOBAL }
	}

	fun getRewatchLabelText(entity: Entities): String {
		val currentDate = DateTime.now()
		val streamStartAtDate =
			if (entity.eventStreamStartsAt.isNullOrBlank()) currentDate else DateTime(
				entity.eventStreamStartsAt, DateTimeZone.UTC
			).withZone(DateTimeZone.getDefault()).toDateTime()
		val isEventStarted =
			compare(streamStartAtDate, currentDate) != DateTimeCompareDifference.GREATER_THAN

		val subscriberAccessEndsAt =
			if (entity.subscriberAccessEndsAt.isNullOrBlank()) currentDate else DateTime(
				entity.subscriberAccessEndsAt, DateTimeZone.UTC
			).withZone(DateTimeZone.getDefault()).toDateTime()
		val subscriberAccessEndsAtString = subscriberAccessEndsAt.toString("MMM d yyyy")
		val reWatchDuration = (entity.eventReWatchDuration?.ifBlank { DEFAULT.DEFAULT_INT_STRING }
			?: DEFAULT.DEFAULT_INT_STRING).toInt()
		val reWatchDurationString = calculateReWatchTime(reWatchDuration)

		entity.access.replaceAll(String::lowercase)
		val userEventAccess = if (entity.access.containsAll(arrayListOf("veeps_plus", "paid"))) {
			EventAccessType.VEEPS_PLUS_PAID
		} else if (entity.access.contains("veeps_plus")) {
			EventAccessType.VEEPS_PLUS
		} else if (entity.access.contains("paid")) {
			EventAccessType.PAID
		} else if (entity.access.contains("free")) {
			EventAccessType.FREE
		} else if (entity.access.contains("veeps_free")) {
			EventAccessType.VEEPS_FREE
		} else EventAccessType.NONE

		if (entity.eventReWatchDuration.isNullOrBlank()) {
			return Veeps.appContext.getString(R.string.re_watch_not_available)
		} else {
			if (reWatchDuration == 0) {
				return Veeps.appContext.getString(R.string.no_re_watch_period)
			} else {
				when (userEventAccess) {
					EventAccessType.VEEPS_PLUS_PAID -> {
						return if (compare(
								subscriberAccessEndsAt, currentDate
							) == DateTimeCompareDifference.EQUALS
						) {
							"Rewatch through $subscriberAccessEndsAtString for subscribers or $reWatchDurationString after ${if (isEventStarted) "purchase" else "the event"}."
						} else {
							"Rewatch through $reWatchDurationString after ${if (isEventStarted) "purchase" else "the event"}."
						}
					}

					EventAccessType.VEEPS_PLUS -> {
						return if (compare(
								subscriberAccessEndsAt, currentDate
							) == DateTimeCompareDifference.EQUALS
						) {
							"Available through $subscriberAccessEndsAtString for all access subscribers."
						} else {
							DEFAULT.EMPTY_STRING
						}
					}

					EventAccessType.PAID -> {
						return "Rewatch available for $reWatchDurationString after the event or rewatch available for $reWatchDurationString after purchase."
					}

					else -> {
						return DEFAULT.EMPTY_STRING
					}
				}
			}
		}
	}

	fun getPrimaryLabelText(entity: Entities, screen: String, isEventPurchased: Boolean): String {
		val isUserSubscribed =
			AppPreferences.get(AppConstants.userSubscriptionStatus, "none") != "none"
		val currentDate = DateTime.now()
		val streamStartAt =
			entity.eventStreamStartsAt?.ifBlank { currentDate.toString() } ?: currentDate.toString()
		val onSaleStatus = entity.onSaleStatus?.lowercase() ?: DEFAULT.EMPTY_STRING
		entity.access.replaceAll(String::lowercase)
		val userEventAccess = if (entity.access.containsAll(arrayListOf("veeps_plus", "paid"))) {
			EventAccessType.VEEPS_PLUS_PAID
		} else if (entity.access.contains("veeps_plus")) {
			EventAccessType.VEEPS_PLUS
		} else if (entity.access.contains("paid")) {
			EventAccessType.PAID
		} else if (entity.access.contains("free")) {
			EventAccessType.FREE
		} else if (entity.access.contains("veeps_free")) {
			EventAccessType.VEEPS_FREE
		} else EventAccessType.NONE
		val streamStartsAtDate =
			DateTime(streamStartAt, DateTimeZone.UTC).withZone(DateTimeZone.getDefault())
				.toDateTime()
		val isEventStarted =
			compare(streamStartsAtDate, currentDate) != DateTimeCompareDifference.GREATER_THAN
		return when (onSaleStatus) {
			"free_livestream" -> {
				if (screen == Screens.BROWSE) {
					ButtonLabels.GO_TO_EVENT
				} else {
					if (isUserSubscribed || isEventPurchased) {
						if (isEventStarted) {
							ButtonLabels.JOIN_LIVE
						} else {
							ButtonLabels.JOIN
						}
					} else {
						ButtonLabels.CLAIM_FREE_TICKET
					}
				}
			}

			"free_ondemand" -> {
				if (screen == Screens.BROWSE) {
					ButtonLabels.GO_TO_EVENT
				} else {
					if (isUserSubscribed || isEventPurchased) {
						if (isEventStarted) {
							ButtonLabels.PLAY
						} else {
							ButtonLabels.JOIN
						}
					} else {
						ButtonLabels.CLAIM_FREE_TICKET
					}
				}
			}

			"off_sale_livestream" -> {
				when (userEventAccess) {
					EventAccessType.VEEPS_FREE -> ButtonLabels.JOIN
					else -> ButtonLabels.UNAVAILABLE
				}
			}

			"off_sale_ondemand" -> {
				when (userEventAccess) {
					EventAccessType.VEEPS_FREE -> ButtonLabels.PLAY
					else -> ButtonLabels.UNAVAILABLE
				}
			}

			"on_sale_livestream" -> {
				if (screen == Screens.BROWSE) {
					if (isEventStarted) {
						if (isUserSubscribed) when (userEventAccess) {
							EventAccessType.VEEPS_PLUS_PAID, EventAccessType.VEEPS_PLUS, EventAccessType.FREE -> ButtonLabels.GO_TO_EVENT
							EventAccessType.PAID -> ButtonLabels.GO_TO_EVENT
							else -> ButtonLabels.UNAVAILABLE
						}
						else when (userEventAccess) {
							EventAccessType.VEEPS_PLUS_PAID, EventAccessType.PAID, EventAccessType.FREE -> ButtonLabels.GET_TICKETS
							EventAccessType.VEEPS_PLUS -> ButtonLabels.UNAVAILABLE
							else -> ButtonLabels.UNAVAILABLE
						}
					} else {
						if (isUserSubscribed) when (userEventAccess) {
							EventAccessType.VEEPS_PLUS_PAID, EventAccessType.VEEPS_PLUS, EventAccessType.FREE, EventAccessType.PAID -> ButtonLabels.GO_TO_EVENT
							else -> ButtonLabels.UNAVAILABLE
						}
						else when (userEventAccess) {
							EventAccessType.VEEPS_PLUS_PAID, EventAccessType.PAID, EventAccessType.FREE -> ButtonLabels.GO_TO_EVENT
							EventAccessType.VEEPS_PLUS -> ButtonLabels.UNAVAILABLE
							else -> ButtonLabels.UNAVAILABLE
						}
					}
				} else {
					when (userEventAccess) {
						EventAccessType.VEEPS_FREE -> if (isEventStarted)
							ButtonLabels.JOIN_LIVE
						else ButtonLabels.JOIN
						else -> if (isEventPurchased) {
							if (isEventStarted) {
								ButtonLabels.JOIN_LIVE
							} else {
								ButtonLabels.JOIN
							}
						} else {
							if (userEventAccess == EventAccessType.PAID || userEventAccess == EventAccessType.VEEPS_PLUS_PAID) {
								ButtonLabels.BUY_TICKET
							} else {
								ButtonLabels.UNAVAILABLE
							}
						}
					}
				}
			}

			"on_sale_ondemand" -> {
				if (screen == Screens.BROWSE) {
					if (isUserSubscribed) when (userEventAccess) {
						EventAccessType.VEEPS_PLUS_PAID, EventAccessType.VEEPS_PLUS, EventAccessType.FREE, EventAccessType.PAID -> ButtonLabels.GO_TO_EVENT
						else -> ButtonLabels.UNAVAILABLE
					}
					else when (userEventAccess) {
						EventAccessType.VEEPS_PLUS_PAID, EventAccessType.PAID, EventAccessType.FREE -> ButtonLabels.GO_TO_EVENT
						EventAccessType.VEEPS_PLUS -> ButtonLabels.UNAVAILABLE
						else -> ButtonLabels.UNAVAILABLE
					}
				} else {
					if (userEventAccess == EventAccessType.VEEPS_FREE)
						ButtonLabels.PLAY
					else if (isEventPurchased) {
						if (isEventStarted) {
							ButtonLabels.PLAY
						} else {
							ButtonLabels.JOIN
						}
					} else {
						if (userEventAccess == EventAccessType.PAID || userEventAccess == EventAccessType.VEEPS_PLUS_PAID) {
							ButtonLabels.BUY_TICKET
						} else {
							ButtonLabels.UNAVAILABLE
						}
					}
				}
			}

			"protected_livestream" -> {
				if (screen == Screens.BROWSE) {
					if (isEventStarted) {
						if (isUserSubscribed) when (userEventAccess) {
							EventAccessType.VEEPS_PLUS_PAID, EventAccessType.VEEPS_PLUS, EventAccessType.FREE, EventAccessType.PAID -> ButtonLabels.GO_TO_EVENT
							else -> ButtonLabels.UNAVAILABLE
						}
						else when (userEventAccess) {
							EventAccessType.VEEPS_PLUS_PAID, EventAccessType.PAID, EventAccessType.FREE -> ButtonLabels.GO_TO_EVENT
							EventAccessType.VEEPS_PLUS -> ButtonLabels.UNAVAILABLE
							else -> ButtonLabels.UNAVAILABLE
						}
					} else {
						if (isUserSubscribed) when (userEventAccess) {
							EventAccessType.VEEPS_PLUS_PAID, EventAccessType.VEEPS_PLUS, EventAccessType.FREE, EventAccessType.PAID -> ButtonLabels.GO_TO_EVENT
							else -> ButtonLabels.UNAVAILABLE
						}
						else when (userEventAccess) {
							EventAccessType.VEEPS_PLUS_PAID, EventAccessType.PAID, EventAccessType.FREE -> ButtonLabels.GO_TO_EVENT
							EventAccessType.VEEPS_PLUS -> ButtonLabels.UNAVAILABLE
							else -> ButtonLabels.UNAVAILABLE
						}
					}
				} else {
					when (userEventAccess) {
						EventAccessType.VEEPS_FREE -> if (isEventStarted)
							ButtonLabels.JOIN_LIVE
						else ButtonLabels.JOIN
						else -> if (isUserSubscribed || isEventPurchased) {
							if (isEventStarted) {
								ButtonLabels.JOIN_LIVE
							} else {
								ButtonLabels.JOIN
							}
						} else {
							ButtonLabels.UNAVAILABLE
						}
					}
					}
				}

			"protected_ondemand" -> {
				if (screen == Screens.BROWSE) {
					if (isUserSubscribed) when (userEventAccess) {
						EventAccessType.VEEPS_PLUS_PAID, EventAccessType.VEEPS_PLUS, EventAccessType.FREE, EventAccessType.PAID -> ButtonLabels.GO_TO_EVENT
						else -> ButtonLabels.UNAVAILABLE
					}
					else when (userEventAccess) {
						EventAccessType.VEEPS_PLUS_PAID, EventAccessType.PAID, EventAccessType.FREE -> ButtonLabels.GO_TO_EVENT
						EventAccessType.VEEPS_PLUS -> ButtonLabels.UNAVAILABLE
						else -> ButtonLabels.UNAVAILABLE
					}
				} else {
					if (userEventAccess == EventAccessType.VEEPS_FREE)
						ButtonLabels.PLAY
					else if (isUserSubscribed || isEventPurchased) {
						if (isEventStarted) {
							ButtonLabels.PLAY
						} else {
							ButtonLabels.JOIN
						}
					} else {
						ButtonLabels.UNAVAILABLE
					}
				}
			}

			"sub_only_livestream" -> {
				if (screen == Screens.BROWSE) {
					if (isEventStarted) {
						if (isUserSubscribed) when (userEventAccess) {
							EventAccessType.VEEPS_PLUS_PAID, EventAccessType.VEEPS_PLUS, EventAccessType.FREE -> ButtonLabels.GO_TO_EVENT
							EventAccessType.PAID -> ButtonLabels.UNAVAILABLE
							else -> ButtonLabels.UNAVAILABLE
						}
						else ButtonLabels.UNAVAILABLE
					} else {
						if (isUserSubscribed) when (userEventAccess) {
							EventAccessType.VEEPS_PLUS_PAID, EventAccessType.VEEPS_PLUS, EventAccessType.FREE -> ButtonLabels.GO_TO_EVENT
							EventAccessType.PAID -> ButtonLabels.UNAVAILABLE
							else -> ButtonLabels.UNAVAILABLE
						}
						else ButtonLabels.UNAVAILABLE
					}
				} else {
					when (userEventAccess) {
						EventAccessType.VEEPS_FREE -> if (isEventStarted)
							ButtonLabels.JOIN_LIVE
						else ButtonLabels.JOIN
						else -> if (isUserSubscribed) {
							if (isEventStarted) {
								ButtonLabels.JOIN_LIVE
							} else {
								ButtonLabels.JOIN
							}
						} else {
							ButtonLabels.UNAVAILABLE
						}
					}
				}
			}

			"sub_only_ondemand" -> {
				if (screen == Screens.BROWSE) {
					if (isUserSubscribed) when (userEventAccess) {
						EventAccessType.VEEPS_PLUS_PAID, EventAccessType.VEEPS_PLUS, EventAccessType.FREE -> ButtonLabels.GO_TO_EVENT
						EventAccessType.PAID -> ButtonLabels.UNAVAILABLE
						else -> ButtonLabels.UNAVAILABLE
					}
					else ButtonLabels.UNAVAILABLE
				} else {
					if (userEventAccess == EventAccessType.VEEPS_FREE)
						ButtonLabels.PLAY
					else if (isUserSubscribed) {
						if (isEventStarted) {
							ButtonLabels.PLAY
						} else {
							ButtonLabels.JOIN
						}
					} else {
						ButtonLabels.UNAVAILABLE
					}
				}
			}

			"sub_livestream" -> {
				if (screen == Screens.BROWSE) {
					if (isEventStarted) {
						if (isUserSubscribed) when (userEventAccess) {
							EventAccessType.VEEPS_PLUS_PAID, EventAccessType.VEEPS_PLUS, EventAccessType.FREE, EventAccessType.PAID -> ButtonLabels.GO_TO_EVENT
							else -> ButtonLabels.UNAVAILABLE
						}
						else when (userEventAccess) {
							EventAccessType.VEEPS_PLUS_PAID, EventAccessType.PAID, EventAccessType.FREE -> ButtonLabels.GET_TICKETS
							EventAccessType.VEEPS_PLUS -> ButtonLabels.UNAVAILABLE
							else -> ButtonLabels.UNAVAILABLE
						}
					} else {
						if (isUserSubscribed) when (userEventAccess) {
							EventAccessType.VEEPS_PLUS_PAID, EventAccessType.VEEPS_PLUS, EventAccessType.FREE, EventAccessType.PAID -> ButtonLabels.GO_TO_EVENT
							else -> ButtonLabels.UNAVAILABLE
						}
						else when (userEventAccess) {
							EventAccessType.VEEPS_PLUS_PAID, EventAccessType.PAID, EventAccessType.FREE -> ButtonLabels.GO_TO_EVENT
							EventAccessType.VEEPS_PLUS -> ButtonLabels.UNAVAILABLE
							else -> ButtonLabels.UNAVAILABLE
						}
					}
				} else {
					when (userEventAccess) {
						EventAccessType.VEEPS_FREE -> if (isEventStarted)
							ButtonLabels.JOIN_LIVE
						else ButtonLabels.JOIN
						else -> if (isUserSubscribed || isEventPurchased) {
							if (isEventStarted) {
								ButtonLabels.JOIN_LIVE
							} else {
								ButtonLabels.JOIN
							}
						} else {
							if (userEventAccess == EventAccessType.PAID || userEventAccess == EventAccessType.VEEPS_PLUS_PAID) {
								ButtonLabels.BUY_TICKET
							} else {
								ButtonLabels.UNAVAILABLE
							}
						}
					}
				}
			}

			"sub_ondemand" -> {
				if (screen == Screens.BROWSE) {
					if (isUserSubscribed) when (userEventAccess) {
						EventAccessType.VEEPS_PLUS_PAID, EventAccessType.VEEPS_PLUS, EventAccessType.FREE, EventAccessType.PAID -> ButtonLabels.GO_TO_EVENT
						else -> ButtonLabels.UNAVAILABLE
					}
					else when (userEventAccess) {
						EventAccessType.VEEPS_PLUS_PAID, EventAccessType.PAID, EventAccessType.FREE -> ButtonLabels.GET_TICKETS
						EventAccessType.VEEPS_PLUS -> ButtonLabels.UNAVAILABLE
						else -> ButtonLabels.UNAVAILABLE
					}
				} else {
					if (userEventAccess == EventAccessType.VEEPS_FREE)
						ButtonLabels.PLAY
					else if (isUserSubscribed || isEventPurchased) {
						if (isEventStarted) {
							ButtonLabels.PLAY
						} else {
							ButtonLabels.JOIN
						}
					} else {
						if (userEventAccess == EventAccessType.PAID || userEventAccess == EventAccessType.VEEPS_PLUS_PAID) {
							ButtonLabels.BUY_TICKET
						} else {
							ButtonLabels.UNAVAILABLE
						}
					}
				}
			}

			"sold_out_livestream" -> {
				if (screen == Screens.BROWSE) {
					if (isUserSubscribed) when (userEventAccess) {
						EventAccessType.VEEPS_PLUS_PAID, EventAccessType.VEEPS_PLUS, EventAccessType.FREE -> ButtonLabels.GO_TO_EVENT
						EventAccessType.PAID -> ButtonLabels.SOLD_OUT
						else -> ButtonLabels.UNAVAILABLE
					}
					else when (userEventAccess) {
						EventAccessType.VEEPS_PLUS_PAID, EventAccessType.PAID, EventAccessType.FREE -> ButtonLabels.SOLD_OUT
						EventAccessType.VEEPS_PLUS -> ButtonLabels.UNAVAILABLE
						else -> ButtonLabels.UNAVAILABLE
					}
				} else {
					if (userEventAccess == EventAccessType.VEEPS_FREE)
						ButtonLabels.JOIN
					else if (isUserSubscribed) {
						if (isEventStarted) {
							ButtonLabels.JOIN_LIVE
						} else {
							ButtonLabels.JOIN
						}
					} else {
						ButtonLabels.SOLD_OUT
					}
				}
			}

			"sold_out_ondemand" -> {
				if (screen == Screens.BROWSE) {
					if (isUserSubscribed) when (userEventAccess) {
						EventAccessType.VEEPS_PLUS_PAID, EventAccessType.VEEPS_PLUS, EventAccessType.FREE -> ButtonLabels.GO_TO_EVENT
						EventAccessType.PAID -> ButtonLabels.SOLD_OUT
						else -> ButtonLabels.UNAVAILABLE
					}
					else when (userEventAccess) {
						EventAccessType.VEEPS_PLUS_PAID, EventAccessType.PAID, EventAccessType.FREE -> ButtonLabels.SOLD_OUT
						EventAccessType.VEEPS_PLUS -> ButtonLabels.UNAVAILABLE
						else -> ButtonLabels.UNAVAILABLE
					}
				} else {
					if (userEventAccess == EventAccessType.VEEPS_FREE)
						ButtonLabels.PLAY
					else if (isUserSubscribed) {
						if (isEventStarted) {
							ButtonLabels.PLAY
						} else {
							ButtonLabels.JOIN
						}
					} else {
						ButtonLabels.SOLD_OUT
					}
				}
			}

			else -> {
				if (screen == Screens.BROWSE) {
					ButtonLabels.GO_TO_EVENT
				} else {
					ButtonLabels.UNAVAILABLE
				}
			}
		}
	}

	fun getUserType(entity: Entities): String {
		return when (AppPreferences.get(
			AppConstants.userSubscriptionStatus, "none"
		)) {
			EventAccessType.VEEPS_PARTNER -> {
				UserType.VEEPS_PAID_SUBSCRIBER
			}

			else -> {
				if (entity.access.contains("veeps_free")) {
					UserType.VEEPS_FREE_TIER
				} else {
					UserType.VEEPS_TICKETS_HOLDER
				}
			}
		}
	}

	fun isEventStarted(streamStartAt: String): Boolean {
		return compare(DateTime(streamStartAt, DateTimeZone.UTC).withZone(DateTimeZone.getDefault())
			.toDateTime(), DateTime.now()) != DateTimeCompareDifference.GREATER_THAN
	}

	fun getGenreCardColor(genreName: String): Int {
		return when (genreName) {
			GenreName.COMEDY -> R.color.comedy
			GenreName.COUNTRY -> R.color.country
			GenreName.INDIE -> R.color.indie
			GenreName.POP -> R.color.pop
			GenreName.K_POP -> R.color.kpop
			GenreName.RAP -> R.color.rap
			GenreName.FOLK -> R.color.folk
			GenreName.ROCK -> R.color.rock
			GenreName.ALTERNATIVE -> R.color.alternative
			GenreName.METAL -> R.color.metal
			GenreName.JAZZ -> R.color.jazz
			GenreName.R_AND_B -> R.color.rnb
			else -> R.color.alternative
		}
	}
}