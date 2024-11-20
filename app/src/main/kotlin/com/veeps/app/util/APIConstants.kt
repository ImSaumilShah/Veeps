package com.veeps.app.util

import com.veeps.app.BuildConfig.isProduction

object APIConstants {
	private const val STAGE: String = "https://api.veeps-team.com/"
	private const val PROD: String = "https://api.veeps.com/"
	private const val AUTH_PROD: String = "https://veeps.com/"
	private const val AUTH_STAG: String ="https://veeps-team.com/" //Staging
	private const val PLAYER_STAT_STAGE: String = "https://vstats.veeps-team.com"
	private const val PLAYER_STAT_PROD: String = "https://vstats.veeps.com"

	var BASE_URL: String = if (isProduction) PROD else STAGE
	private const val API_VERSION: String = "v3/"
	private const val AUTH_VERSION: String = AUTH_PROD + "oauth/"
	const val QR_CODE_BASE_URL: String =
		"https://veeps.com/qr-code?i="
	const val QR_CODE_ACTIVATE_URL: String =
		"${QR_CODE_BASE_URL}https://veeps.com/activate?code="

	const val FETCH_AUTHENTICATION_DETAILS: String = "${AUTH_VERSION}authorize_device"
	const val AUTHENTICATION_POLLING: String = "${AUTH_VERSION}token"
	const val FETCH_USER_DETAILS: String = "${API_VERSION}me"
	const val FETCH_BROWSE_RAILS: String = "${API_VERSION}features"
	const val FETCH_ON_DEMAND_RAILS: String = "${API_VERSION}features?scope=on-demand"
	const val FETCH_CONTINUE_WATCHING_RAIL: String = "${API_VERSION}me/tickets?type=on_demand"
	const val FETCH_USER_STATS: String = "/getuserstats"
	const val FETCH_UPCOMING_EVENTS: String = "${API_VERSION}events?type=upcoming"
	const val FETCH_SEARCH_RESULT: String = "${API_VERSION}search"
	const val FETCH_ENTITY_DETAILS: String = "${API_VERSION}entities/{ENTITY}/{ENTITY_ID}"
	const val FETCH_ENTITY_UPCOMING_EVENTS: String =
		"${API_VERSION}events/?per_page=10&page=1&type=upcoming"
	const val FETCH_ENTITY_ON_DEMAND_EVENTS: String =
		"${API_VERSION}events/?per_page=10&page=1&type=on_demand"
	const val FETCH_ENTITY_PAST_EVENTS: String = "${API_VERSION}events/?per_page=10&page=1&type=past"
	const val FETCH_ALL_PURCHASED_EVENTS: String = "${API_VERSION}me/tickets?type=all"
	const val WATCH_LIST_EVENTS: String = "${API_VERSION}watchlist_entries"
	const val REMOVE_WATCH_LIST_EVENTS: String = "${API_VERSION}watchlist_entries"
	const val FETCH_EVENT_STREAM_DETAILS: String = "${API_VERSION}events/{EVENT_ID}/stream"
	const val FETCH_EVENT_DETAILS: String = "${API_VERSION}events/{EVENT_ID}"
	const val FETCH_EVENT_PRODUCT_DETAILS: String = "${API_VERSION}events/{EVENT_ID}/products?vendor=fire_tv"
	const val CLAIM_FREE_TICKET_FOR_EVENT: String = "${API_VERSION}events/{EVENT_ID}/redeem"
	const val CLEAR_ALL_RESERVATIONS: String = "${API_VERSION}reservations/all"
	const val SET_NEW_RESERVATION: String = "${API_VERSION}reservations"
	const val GENERATE_NEW_ORDER: String = "${API_VERSION}orders/new"
	const val CREATE_ORDER: String = "${API_VERSION}orders"
	const val FETCH_STORY_BOARD: String = "FETCH_STORY_BOARD"
	const val ADD_STATS: String = "/addstat"
	const val FETCH_COMPANIONS: String = "${API_VERSION}me/companion"
	const val FETCH_FEATURED_CONTENT: String = "${API_VERSION}features?scope=search"
	const val FETCH_RECOMMENDED_CONTENT: String = "${API_VERSION}features"
	const val FETCH_GENRE_CONTENT: String = "${API_VERSION}features"
	const val SUBSCRIPTION_MAPPING: String = "${API_VERSION}partner_subscriptions/firetv/mapping"
	const val VALIDATE_APP_VERSIONS: String = "${API_VERSION}versions?platform=firetv&stage=prd"
}
