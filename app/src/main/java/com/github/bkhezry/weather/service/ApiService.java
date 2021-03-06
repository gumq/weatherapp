package com.github.bkhezry.weather.service;

import com.github.bkhezry.weather.model.currentweather.CurrentWeatherResponse;
import com.github.bkhezry.weather.model.daysweather.MultipleDaysWeatherResponse;
import com.github.bkhezry.weather.model.fivedayweather.FiveDayResponse;

import io.reactivex.Single;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface ApiService {

  /**
   * Get current weather of city
   *
   * @param q     String name of city
   * @param units String units of response
   * @param lang  String language of response
   * @param appId String api key
   * @return instance of {@link CurrentWeatherResponse}
   */
  @GET("weather")
  Single<CurrentWeatherResponse> getCurrentWeather(
      @Query("q") String q,
      @Query("units") String units,
      @Query("lang") String lang,
      @Query("appid") String appId
  );

  @GET("weather")
  Single<CurrentWeatherResponse> getCurrentWeatherbyLoc(
          @Query("lat") double lat,
          @Query ( "lon" ) double lon,
          @Query("units") String units,
          @Query("lang") String lang,
          @Query("appid") String appId
  );

  /**
   * Get five days weather forecast.
   *
   * @param lat,lon    double lat and lon of location
   * @param units String units of response
   * @param lang  String language of response
   * @param appId String api key
   * @return instance of {@link FiveDayResponse}
   */
  @GET("onecall")
  Single<FiveDayResponse> getFiveDaysWeather(
          @Query("lat") double lat,
          @Query ( "lon" ) double lon,
      @Query("units") String units,
      @Query("lang") String lang,
      @Query("appid") String appId
  );

  /**
   * Get multiple days weather
   *
   * @param q     String name of city
   * @param units String units of response
   * @param lang  String language of response
   * @param appId String api key
   * @return instance of {@link MultipleDaysWeatherResponse}
   */
  @GET("onecall/exclude")
  Single<MultipleDaysWeatherResponse> getMultipleDaysWeather(
          @Query("q") String q,
      @Query("units") String units,
      @Query("lang") String lang,
      @Query("cnt") int dayCount,
      @Query("appid") String appId
  );
}
