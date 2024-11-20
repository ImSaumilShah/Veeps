package com.veeps.app.data.network

import com.google.gson.GsonBuilder
import com.google.gson.Strictness
import com.veeps.app.BuildConfig
import com.veeps.app.data.common.BaseResponseGeneric
import com.veeps.app.extension.isAppConnected
import com.veeps.app.feature.artist.model.ArtistResponse
import com.veeps.app.feature.contentRail.model.Entities
import com.veeps.app.feature.contentRail.model.ProductsResponse
import com.veeps.app.feature.contentRail.model.RailData
import com.veeps.app.feature.contentRail.model.RailResponse
import com.veeps.app.feature.contentRail.model.UserStatsResponse
import com.veeps.app.feature.event.model.OrderGeneration
import com.veeps.app.feature.event.model.Reservation
import com.veeps.app.feature.search.model.SearchResponse
import com.veeps.app.feature.signIn.model.PollingData
import com.veeps.app.feature.signIn.model.SignInData
import com.veeps.app.feature.user.model.UserData
import com.veeps.app.feature.video.model.Companion
import com.veeps.app.feature.video.model.StoryBoardImages
import com.veeps.app.util.APIConstants
import com.veeps.app.util.AppConstants
import com.veeps.app.util.AppPreferences
import com.veeps.app.util.Logger
import io.sentry.HttpStatusCodeRange
import io.sentry.okhttp.SentryOkHttpEventListener
import io.sentry.okhttp.SentryOkHttpInterceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url
import java.util.concurrent.TimeUnit

object APIUtil {

	private var startNs: Long = System.nanoTime()

	val service: APIService by lazy {
		val logging = HttpLoggingInterceptor(HttpLoggingInterceptor.Logger.DEFAULT)
		logging.level =
			if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE

		val httpClient = OkHttpClient.Builder().addInterceptor(
			SentryOkHttpInterceptor(
				captureFailedRequests = true, failedRequestStatusCodes = listOf(
					HttpStatusCodeRange(400, 599)
				)
			)
		).eventListener(SentryOkHttpEventListener()).addInterceptor { chain ->
			if (isAppConnected()) {
				startNs = System.nanoTime()
				var request = chain.request()
				val originalHttpUrl = request.url
				val headerToken =
					if (originalHttpUrl.encodedPath == APIConstants.FETCH_USER_STATS || originalHttpUrl.encodedPath == APIConstants.ADD_STATS) AppPreferences.get(
						AppConstants.generatedJWT, "GeneratedJWT"
					) else AppPreferences.get(
						AppConstants.authenticatedUserToken, "AuthenticatedUserToken"
					)
				val builder: Request.Builder =
					request.newBuilder().header("Authorization", "Bearer $headerToken")
				request = builder.build()
				if (originalHttpUrl.encodedPath == APIConstants.FETCH_USER_STATS || originalHttpUrl.encodedPath == APIConstants.ADD_STATS) AppPreferences.remove(
					AppConstants.generatedJWT
				)
				chain.proceed(request)
			} else {
				throw NoConnectivityException()
			}
		}.addInterceptor { chain ->
			val response = chain.proceed(chain.request())
			var bodyString: String
			response.body.let {
				bodyString = response.body.string()
				val tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs)
				val tookS = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - startNs)
				Logger.printAPILogs(chain, response, bodyString, tookMs, tookS)
			}
			val newResponse: okhttp3.Response.Builder = response.newBuilder()
			newResponse.body(bodyString.toResponseBody(response.body.contentType()))
			newResponse.build()
		}.addInterceptor(logging).connectTimeout(1, TimeUnit.MINUTES)
			.readTimeout(1, TimeUnit.MINUTES).writeTimeout(1, TimeUnit.MINUTES).build()

		Retrofit.Builder().baseUrl(APIConstants.BASE_URL)
			.addConverterFactory(ScalarsConverterFactory.create())
			.addConverterFactory(GsonConverterFactory.create(GsonBuilder().setStrictness(Strictness.LENIENT).create()))
			.client(httpClient).build().create(APIService::class.java)
	}

	interface APIService {
		@POST(APIConstants.FETCH_AUTHENTICATION_DETAILS)
		suspend fun fetchAuthenticationDetails(@Query("client_id") clientId: String): Response<SignInData>

		@POST(APIConstants.AUTHENTICATION_POLLING)
		@FormUrlEncoded
		suspend fun authenticationPolling(
			@Field("grant_type") grantType: String,
			@Field("device_code") deviceCode: String,
			@Field("client_id") clientId: String,
			@Field("client_secret") clientSecret: String,
		): Response<PollingData>

		@GET(APIConstants.FETCH_USER_DETAILS)
		suspend fun fetchUserDetails(): Response<BaseResponseGeneric<UserData>>

		@GET(APIConstants.FETCH_BROWSE_RAILS)
		suspend fun fetchBrowseRails(): Response<RailResponse>

		@GET(APIConstants.FETCH_ON_DEMAND_RAILS)
		suspend fun fetchOnDemandRails(): Response<RailResponse>

		@GET(APIConstants.FETCH_FEATURED_CONTENT)
		suspend fun fetchFeaturedContent(): Response<RailResponse>

		@GET(APIConstants.FETCH_CONTINUE_WATCHING_RAIL)
		suspend fun fetchContinueWatchingRail(): Response<RailData>

		@GET
		suspend fun fetchUserStats(
			@Url userStatsAPIURL: String, @Query("e") eventIds: String
		): Response<UserStatsResponse>

		@GET(APIConstants.FETCH_UPCOMING_EVENTS)
		suspend fun fetchUpcomingEvents(): Response<RailData>

		@GET(APIConstants.FETCH_SEARCH_RESULT)
		suspend fun fetchSearchResult(@Query("q") search: String): Response<SearchResponse>

		@GET(APIConstants.FETCH_ENTITY_DETAILS)
		suspend fun fetchEntityDetails(
			@Path("ENTITY") entity: String,
			@Path("ENTITY_ID") entityId: String,
		): Response<ArtistResponse>

		@GET(APIConstants.FETCH_ENTITY_UPCOMING_EVENTS)
		suspend fun fetchEntityUpcomingEvents(@Query("scope") entityScope: String): Response<RailData>

		@GET(APIConstants.FETCH_ENTITY_ON_DEMAND_EVENTS)
		suspend fun fetchEntityOnDemandEvents(@Query("scope") entityScope: String): Response<RailData>

		@GET(APIConstants.FETCH_ENTITY_PAST_EVENTS)
		suspend fun fetchEntityPastEvents(@Query("scope") entityScope: String): Response<RailData>

		@GET(APIConstants.FETCH_ALL_PURCHASED_EVENTS)
		suspend fun fetchAllPurchasedEvents(): Response<RailData>

		@GET(APIConstants.WATCH_LIST_EVENTS)
		suspend fun fetchAllWatchListEvents(): Response<RailData>

		@POST(APIConstants.WATCH_LIST_EVENTS)
		suspend fun addWatchListEvent(@Body hashMap: HashMap<String, Any>): Response<BaseResponseGeneric<Any>>

		@HTTP(method = "DELETE", path = APIConstants.REMOVE_WATCH_LIST_EVENTS, hasBody = true)
		suspend fun removeWatchListEvent(@Body hashMap: HashMap<String, Any>): Response<BaseResponseGeneric<Any>>

		@GET(APIConstants.FETCH_EVENT_STREAM_DETAILS)
		suspend fun fetchEventStreamDetails(@Path("EVENT_ID") entity: String): Response<BaseResponseGeneric<Entities>>

		@GET(APIConstants.FETCH_EVENT_DETAILS)
		suspend fun fetchEventDetails(@Path("EVENT_ID") entity: String): Response<BaseResponseGeneric<Entities>>

		@GET(APIConstants.FETCH_EVENT_PRODUCT_DETAILS)
		suspend fun fetchEventProductDetails(@Path("EVENT_ID") entity: String): Response<ProductsResponse>

		@POST(APIConstants.CLAIM_FREE_TICKET_FOR_EVENT)
		suspend fun claimFreeTicketForEvent(@Path("EVENT_ID") entity: String): Response<BaseResponseGeneric<Any>>

		@DELETE(APIConstants.CLEAR_ALL_RESERVATIONS)
		suspend fun clearAllReservations(): Response<BaseResponseGeneric<Any>>

		@POST(APIConstants.SET_NEW_RESERVATION)
		suspend fun setNewReservation(@Body hashMap: HashMap<String, Any>): Response<BaseResponseGeneric<Reservation>>

		@GET(APIConstants.GENERATE_NEW_ORDER)
		suspend fun generateNewOrder(): Response<BaseResponseGeneric<OrderGeneration>>

		@POST(APIConstants.CREATE_ORDER)
		suspend fun createOrder(@Body hashMap: HashMap<String, Any>): Response<BaseResponseGeneric<Any>>

		@POST(APIConstants.FETCH_COMPANIONS)
		suspend fun fetchCompanions(@Body hashMap: HashMap<String, Any>): Response<BaseResponseGeneric<Companion>>

		@GET
		suspend fun fetchStoryBoard(@Url storyBoardURL: String): Response<StoryBoardImages>

		@GET
		suspend fun addStats(
			@Url addStatsAPIURL: String,
			@Query("cur") currentTime: String,
			@Query("pld") duration: String,
			@Query("plv") playerVersion: String,
			@Query("dvm") deviceModel: String,
			@Query("dvv") deviceVendor: String,
			@Query("pls") playbackStreamType: String,
			@Query("p") platform: String,
			@Query("s") userType: String
		): Response<BaseResponseGeneric<Any>>

		@GET(APIConstants.FETCH_RECOMMENDED_CONTENT)
		suspend fun fetchRecommendedContent(@Query("scope") scope: String): Response<RailResponse>

		@POST(APIConstants.SUBSCRIPTION_MAPPING)
		suspend fun subscriptionMapping(@Body hashMap: HashMap<String, Any>): Response<BaseResponseGeneric<Any>>

		@GET(APIConstants.FETCH_GENRE_CONTENT)
		suspend fun fetchGenreContent(@Query("scope") scope: String): Response<RailResponse>

		@GET(APIConstants.VALIDATE_APP_VERSIONS)
		suspend fun validateAppVersion(@Query("version") appVersion: String): Response<BaseResponseGeneric<Any>>
	}
}