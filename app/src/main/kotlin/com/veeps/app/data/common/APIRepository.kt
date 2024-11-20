package com.veeps.app.data.common

import com.veeps.app.core.BaseDataSource
import com.veeps.app.data.network.APIUtil
import com.veeps.app.util.APIConstants
import com.veeps.app.util.AppConstants

class APIRepository : BaseDataSource() {
	fun authenticationDetails(clientId: String) =
		performOperation(APIConstants.FETCH_AUTHENTICATION_DETAILS) {
			getResult(APIConstants.FETCH_AUTHENTICATION_DETAILS) {
				APIUtil.service.fetchAuthenticationDetails(clientId)
			}
		}

	fun authenticationPolling(deviceCode: String) =
		performOperation(APIConstants.AUTHENTICATION_POLLING) {
			getResult(APIConstants.AUTHENTICATION_POLLING) {
				APIUtil.service.authenticationPolling(
					AppConstants.grantType,
					deviceCode,
					AppConstants.clientId,
					AppConstants.clientSecret
				)
			}
		}

	fun fetchUserDetails() = performOperation(APIConstants.FETCH_USER_DETAILS) {
		getResult(APIConstants.FETCH_USER_DETAILS) {
			APIUtil.service.fetchUserDetails()
		}
	}

	fun fetchBrowseRails() = performOperation(APIConstants.FETCH_BROWSE_RAILS) {
		getResult(APIConstants.FETCH_BROWSE_RAILS) {
			APIUtil.service.fetchBrowseRails()
		}
	}

	fun fetchOnDemandRails() = performOperation(APIConstants.FETCH_ON_DEMAND_RAILS) {
		getResult(APIConstants.FETCH_ON_DEMAND_RAILS) {
			APIUtil.service.fetchOnDemandRails()
		}
	}

	fun fetchGenreRails(genreName : String) = performOperation(APIConstants.FETCH_GENRE_CONTENT) {
		getResult(APIConstants.FETCH_GENRE_CONTENT) {
			APIUtil.service.fetchGenreContent(genreName)
		}
	}

	fun fetchFeaturedContent() = performOperation(APIConstants.FETCH_FEATURED_CONTENT) {
		getResult(APIConstants.FETCH_FEATURED_CONTENT) {
			APIUtil.service.fetchFeaturedContent()
		}
	}

	fun fetchContinueWatchingRail() = performOperation(APIConstants.FETCH_CONTINUE_WATCHING_RAIL) {
		getResult(APIConstants.FETCH_CONTINUE_WATCHING_RAIL) {
			APIUtil.service.fetchContinueWatchingRail()
		}
	}

	fun fetchUserStats(userStatsAPIURL: String, eventIds: String) =
		performOperation(APIConstants.FETCH_USER_STATS) {
			getResult(APIConstants.FETCH_USER_STATS) {
				APIUtil.service.fetchUserStats(userStatsAPIURL, eventIds)
			}
		}

	fun fetchUpcomingEvents() = performOperation(APIConstants.FETCH_UPCOMING_EVENTS) {
		getResult(APIConstants.FETCH_UPCOMING_EVENTS) {
			APIUtil.service.fetchUpcomingEvents()
		}
	}

	fun fetchSearchResult(search: String) =
		performOperation(APIConstants.FETCH_SEARCH_RESULT) {
			getResult(APIConstants.FETCH_SEARCH_RESULT) {
				APIUtil.service.fetchSearchResult(search)
			}
		}

	fun fetchEntityDetails(entity: String, entityId: String) =
		performOperation(APIConstants.FETCH_ENTITY_DETAILS) {
			getResult(APIConstants.FETCH_ENTITY_DETAILS) {
				APIUtil.service.fetchEntityDetails(entity, entityId)
			}
		}

	fun fetchEntityUpcomingEvents(entityScope: String) =
		performOperation(APIConstants.FETCH_ENTITY_UPCOMING_EVENTS) {
			getResult(APIConstants.FETCH_ENTITY_UPCOMING_EVENTS) {
				APIUtil.service.fetchEntityUpcomingEvents(entityScope)
			}
		}

	fun fetchEntityOnDemandEvents(entityScope: String) =
		performOperation(APIConstants.FETCH_ENTITY_ON_DEMAND_EVENTS) {
			getResult(APIConstants.FETCH_ENTITY_ON_DEMAND_EVENTS) {
				APIUtil.service.fetchEntityOnDemandEvents(entityScope)
			}
		}

	fun fetchEntityPastEvents(entityScope: String) =
		performOperation(APIConstants.FETCH_ENTITY_PAST_EVENTS) {
			getResult(APIConstants.FETCH_ENTITY_PAST_EVENTS) {
				APIUtil.service.fetchEntityPastEvents(entityScope)
			}
		}

	fun fetchAllPurchasedEvents() = performOperation(APIConstants.FETCH_ALL_PURCHASED_EVENTS) {
		getResult(APIConstants.FETCH_ALL_PURCHASED_EVENTS) {
			APIUtil.service.fetchAllPurchasedEvents()
		}
	}

	fun fetchAllWatchListEvents() = performOperation(APIConstants.WATCH_LIST_EVENTS) {
		getResult(APIConstants.WATCH_LIST_EVENTS) {
			APIUtil.service.fetchAllWatchListEvents()
		}
	}

	fun addRemoveWatchListEvent(eventId: HashMap<String, Any>, isRemoveFromWatchList: Boolean) =
		performOperation(APIConstants.WATCH_LIST_EVENTS) {
			getResult(APIConstants.WATCH_LIST_EVENTS) {
				if (isRemoveFromWatchList) APIUtil.service.removeWatchListEvent(eventId)
				else APIUtil.service.addWatchListEvent(eventId)
			}
		}

	fun fetchEventStreamDetails(eventId: String) =
		performOperation(APIConstants.FETCH_EVENT_STREAM_DETAILS) {
			getResult(APIConstants.FETCH_EVENT_STREAM_DETAILS) {
				APIUtil.service.fetchEventStreamDetails(eventId)
			}
		}

	fun fetchEventDetails(eventId: String) = performOperation(APIConstants.FETCH_EVENT_DETAILS) {
		getResult(APIConstants.FETCH_EVENT_DETAILS) {
			APIUtil.service.fetchEventDetails(eventId)
		}
	}

	fun fetchEventProductDetails(eventId: String) =
		performOperation(APIConstants.FETCH_EVENT_PRODUCT_DETAILS) {
			getResult(APIConstants.FETCH_EVENT_PRODUCT_DETAILS) {
				APIUtil.service.fetchEventProductDetails(eventId)
			}
		}

	fun claimFreeTicketForEvent(eventId: String) =
		performOperation(APIConstants.CLAIM_FREE_TICKET_FOR_EVENT) {
			getResult(APIConstants.CLAIM_FREE_TICKET_FOR_EVENT) {
				APIUtil.service.claimFreeTicketForEvent(eventId)
			}
		}

	fun clearAllReservations() = performOperation(APIConstants.CLEAR_ALL_RESERVATIONS) {
		getResult(APIConstants.CLEAR_ALL_RESERVATIONS) {
			APIUtil.service.clearAllReservations()
		}
	}

	fun setNewReservation(itemId: HashMap<String, Any>) =
		performOperation(APIConstants.SET_NEW_RESERVATION) {
			getResult(APIConstants.SET_NEW_RESERVATION) {
				APIUtil.service.setNewReservation(itemId)
			}
		}

	fun generateNewOrder() = performOperation(APIConstants.GENERATE_NEW_ORDER) {
		getResult(APIConstants.GENERATE_NEW_ORDER) {
			APIUtil.service.generateNewOrder()
		}
	}

	fun createOrder(orderDetails: HashMap<String, Any>) =
		performOperation(APIConstants.CREATE_ORDER) {
			getResult(APIConstants.CREATE_ORDER) {
				APIUtil.service.createOrder(orderDetails)
			}
		}

	fun fetchStoryBoard(storyBoardURL: String) = performOperation(APIConstants.FETCH_STORY_BOARD) {
		getResult(APIConstants.FETCH_STORY_BOARD) {
			APIUtil.service.fetchStoryBoard(storyBoardURL)
		}
	}

	fun fetchCompanions(eventDetails: HashMap<String, Any>) =
		performOperation(APIConstants.FETCH_COMPANIONS) {
			getResult(APIConstants.FETCH_COMPANIONS) {
				APIUtil.service.fetchCompanions(eventDetails)
			}
		}

	fun addStats(
		addStatsAPIURL: String,
		currentTime: String,
		duration: String,
		playerVersion: String,
		deviceModel: String,
		deviceVendor: String,
		playbackStreamType: String,
		platform: String,
		userType: String
	) = performOperation(APIConstants.ADD_STATS) {
		getResult(APIConstants.ADD_STATS) {
			APIUtil.service.addStats(
				addStatsAPIURL,
				currentTime,
				duration,
				playerVersion,
				deviceModel,
				deviceVendor,
				playbackStreamType,
				platform,
				userType
			)
		}
	}

	fun fetchRecommendedContent(scope: String) =
		performOperation(APIConstants.FETCH_RECOMMENDED_CONTENT) {
			getResult(APIConstants.FETCH_RECOMMENDED_CONTENT) {
				APIUtil.service.fetchRecommendedContent(scope)
			}
		}

	fun subscriptionMapping(subscriptionMappingRequest: HashMap<String, Any>) =
		performOperation(APIConstants.SUBSCRIPTION_MAPPING) {
			getResult(APIConstants.SUBSCRIPTION_MAPPING) {
				APIUtil.service.subscriptionMapping(subscriptionMappingRequest)
			}
		}

	fun validateAppVersion(appVersion: String) =
		performOperation(APIConstants.VALIDATE_APP_VERSIONS) {
			getResult(APIConstants.VALIDATE_APP_VERSIONS) {
				APIUtil.service.validateAppVersion(appVersion)
			}
		}
}