package com.github.bkhezry.weather.ui.activity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.github.bkhezry.weather.R;
import com.github.bkhezry.weather.databinding.ActivityMainBinding;
import com.github.bkhezry.weather.model.CityInfo;
import com.github.bkhezry.weather.model.currentweather.CurrentWeatherResponse;
import com.github.bkhezry.weather.model.daysweather.ListItem;
import com.github.bkhezry.weather.model.daysweather.MultipleDaysWeatherResponse;
import com.github.bkhezry.weather.model.db.CurrentWeather;
import com.github.bkhezry.weather.model.db.FiveDayWeather;
import com.github.bkhezry.weather.model.db.ItemHourlyDB;
import com.github.bkhezry.weather.model.fivedayweather.FiveDayResponse;
import com.github.bkhezry.weather.model.fivedayweather.ItemHourly;
import com.github.bkhezry.weather.service.ApiService;
import com.github.bkhezry.weather.ui.fragment.AboutFragment;
import com.github.bkhezry.weather.ui.fragment.MultipleDaysFragment;
import com.github.bkhezry.weather.utils.ApiClient;
import com.github.bkhezry.weather.utils.AppUtil;
import com.github.bkhezry.weather.utils.Constants;
import com.github.bkhezry.weather.utils.DbUtil;
import com.github.bkhezry.weather.utils.MyApplication;
import com.github.bkhezry.weather.utils.SnackbarUtil;
import com.github.bkhezry.weather.utils.TextViewFactory;
import com.github.pwittchen.prefser.library.rx2.Prefser;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.miguelcatalan.materialsearchview.MaterialSearchView;
import com.mikepenz.fastadapter.FastAdapter;
import com.mikepenz.fastadapter.IAdapter;
import com.mikepenz.fastadapter.adapters.ItemAdapter;
import com.mikepenz.fastadapter.listeners.OnClickListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import io.objectbox.Box;
import io.objectbox.BoxStore;
import io.objectbox.android.AndroidScheduler;
import io.objectbox.query.Query;
import io.objectbox.reactive.DataObserver;
import io.objectbox.reactive.DataSubscriptionList;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableSingleObserver;
import io.reactivex.schedulers.Schedulers;
import retrofit2.HttpException;

public class MainActivity extends BaseActivity implements LocationListener {

  private static final int REQUEST_LOCATION_PERMISSION = 100;
  private boolean mIsAutoUpdateLocation;
  public static final String TAG = MainActivity.class.getSimpleName();
  private static final long UPDATE_INTERVAL = 5000;
  private static final long FASTEST_INTERVAL = 5000;
  private FastAdapter<FiveDayWeather> mFastAdapter;
  private ItemAdapter<FiveDayWeather> mItemAdapter;
  private CompositeDisposable disposable = new CompositeDisposable ();
  private String defaultLang = "en";
  private String viLang = "vi";
  private List<FiveDayWeather> fiveDayWeathers;
  private ApiService apiService;
  private FiveDayWeather todayFiveDayWeather;
  private Prefser prefser;
  private Box<CurrentWeather> currentWeatherBox;
  private Box<FiveDayWeather> fiveDayWeatherBox;
  private Box<ItemHourlyDB> itemHourlyDBBox;
  private DataSubscriptionList subscriptions = new DataSubscriptionList ();
  private boolean isLoad = false;
  private CityInfo cityInfo;
  private String apiKey;
  private Typeface typeface;
  private ActivityMainBinding binding;
  private int[] colors;
  private int[] colorsAlpha;
  LocationManager locationManager;
  private GoogleApiClient mGoogleApiClient;
  private LocationRequest mLocationRequest;
  private Location mLastLocation;
  TextView textView;
  Button button;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate ( savedInstanceState );
    setContentView ( R.layout.activity_main);
    binding = ActivityMainBinding.inflate ( getLayoutInflater () );
    setContentView ( binding.getRoot () );
    setSupportActionBar ( binding.toolbarLayout.toolbar );
    initSearchView ();
    initValues ();
    setupTextSwitchers ();
    initRecyclerView ();
    showStoredCurrentWeather ();
    showStoredFiveDayWeather ();
    checkLastUpdate ();
    permissionVersion();
    button = findViewById ( R.id.button );
    textView = findViewById ( R.id.textView );
    locationManager = (LocationManager) getSystemService ( Context.LOCATION_SERVICE );
    //CAP NHAT VI TRI
    if (ActivityCompat.checkSelfPermission ( this, Manifest.permission.ACCESS_FINE_LOCATION )
            != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission
            ( this, Manifest.permission.ACCESS_COARSE_LOCATION ) != PackageManager.PERMISSION_GRANTED) {
      // TODO: Consider calling
      //    ActivityCompat#requestPermissions

      return;
    }
    button.setOnClickListener ( new View.OnClickListener () {
      @Override
      public void onClick(View v) {
        if (isGpsOn()||isNetworkOn ()) {
          updateUit();
          getCurrentWeatherByLoc(mLastLocation.getLatitude (),mLastLocation.getLongitude ());
          Toast.makeText(MainActivity.this, "C???p nh???t th??nh c??ng v??? tr?? c???a b???n!", Toast.LENGTH_SHORT).show();
        } else {
          Toast.makeText(MainActivity.this, "Vui l??ng b???t GPS!", Toast.LENGTH_SHORT).show();
        }
      }
    } );
    locationManager.requestLocationUpdates ( LocationManager.GPS_PROVIDER, 1000, 1, this );

  }
  private void updateUit() {
    if (mLastLocation != null) {
      textView.setText ( String.format ( Locale.getDefault (), "%f,%f",
              mLastLocation.getLatitude (), mLastLocation.getLongitude () ) );
    }



  }
  private boolean isGpsOn() {
    LocationManager manager = (LocationManager) getSystemService(LOCATION_SERVICE);
    return manager.isProviderEnabled(LocationManager.GPS_PROVIDER);

  }
  private boolean isNetworkOn() {
    LocationManager manager = (LocationManager) getSystemService(LOCATION_SERVICE);
    return manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

  }
  private void initSearchView() {
    binding.toolbarLayout.searchView.setVoiceSearch(false);
    binding.toolbarLayout.searchView.setHint(getString(R.string.search_label));
    binding.toolbarLayout.searchView.setCursorDrawable(R.drawable.custom_curosr);
    binding.toolbarLayout.searchView.setEllipsize(true);
    binding.toolbarLayout.searchView.setOnQueryTextListener(new MaterialSearchView.OnQueryTextListener() {
      @Override
      public boolean onQueryTextSubmit(String query) {
        requestWeather(query, true);
        return false;
      }

      @Override
      public boolean onQueryTextChange(String newText) {
        return false;
      }
    });
    binding.toolbarLayout.searchView.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        binding.toolbarLayout.searchView.showSearch();
      }
    });

  }

  private void initValues() {
    colors = getResources().getIntArray(R.array.mdcolor_500);
    colorsAlpha = getResources().getIntArray(R.array.mdcolor_500_alpha);
    prefser = new Prefser(this);
    apiService = ApiClient.getClient().create(ApiService.class);
    BoxStore boxStore = MyApplication.getBoxStore();
    currentWeatherBox = boxStore.boxFor(CurrentWeather.class);
    fiveDayWeatherBox = boxStore.boxFor(FiveDayWeather.class);
    itemHourlyDBBox = boxStore.boxFor(ItemHourlyDB.class);
    binding.swipeContainer.setColorSchemeResources(android.R.color.holo_blue_bright,
        android.R.color.holo_green_light,
        android.R.color.holo_orange_light,
        android.R.color.holo_red_light);
    binding.swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {

      @Override
      public void onRefresh() {
        cityInfo = prefser.get(Constants.CITY_INFO, CityInfo.class, null);
        if (cityInfo != null) {
          long lastStored = prefser.get(Constants.LAST_STORED_CURRENT, Long.class, 0L);
          if (AppUtil.isTimePass(lastStored)) {
            requestWeather(cityInfo.getName(), false);
          } else {
            binding.swipeContainer.setRefreshing(false);
          }
        } else {
          binding.swipeContainer.setRefreshing(false);
        }
      }

    });
    binding.bar.setNavigationOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        showAboutFragment();
      }
    });
    typeface = Typeface.createFromAsset(getAssets(), "fonts/RobotoMono-Regular.ttf");
    binding.nextDaysButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        AppUtil.showFragment(new MultipleDaysFragment(), getSupportFragmentManager(), true);
      }
    });
    binding.contentMainLayout.todayMaterialCard.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        if (todayFiveDayWeather != null) {
          Intent intent = new Intent(MainActivity.this, HourlyActivity.class);
          intent.putExtra(Constants.FIVE_DAY_WEATHER_ITEM, todayFiveDayWeather);
          startActivity(intent);
        }
      }
    });
  }

  private void setupTextSwitchers() {
    binding.contentMainLayout.tempTextView.setFactory(new TextViewFactory(MainActivity.this, R.style.TempTextView, true, typeface));
    binding.contentMainLayout.tempTextView.setInAnimation(MainActivity.this, R.anim.slide_in_right);
    binding.contentMainLayout.tempTextView.setOutAnimation(MainActivity.this, R.anim.slide_out_left);
    binding.contentMainLayout.descriptionTextView.setFactory(new TextViewFactory(MainActivity.this, R.style.DescriptionTextView, true, typeface));
    binding.contentMainLayout.descriptionTextView.setInAnimation(MainActivity.this, R.anim.slide_in_right);
    binding.contentMainLayout.descriptionTextView.setOutAnimation(MainActivity.this, R.anim.slide_out_left);
    binding.contentMainLayout.humidityTextView.setFactory(new TextViewFactory(MainActivity.this, R.style.HumidityTextView, false, typeface));
    binding.contentMainLayout.humidityTextView.setInAnimation(MainActivity.this, R.anim.slide_in_bottom);
    binding.contentMainLayout.humidityTextView.setOutAnimation(MainActivity.this, R.anim.slide_out_top);
    binding.contentMainLayout.windTextView.setFactory(new TextViewFactory(MainActivity.this, R.style.WindSpeedTextView, false, typeface));
    binding.contentMainLayout.windTextView.setInAnimation(MainActivity.this, R.anim.slide_in_bottom);
    binding.contentMainLayout.windTextView.setOutAnimation(MainActivity.this, R.anim.slide_out_top);
  }

  private void initRecyclerView() {
    LinearLayoutManager layoutManager
        = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
    binding.contentMainLayout.recyclerView.setLayoutManager(layoutManager);
    mItemAdapter = new ItemAdapter<>();
    mFastAdapter = FastAdapter.with(mItemAdapter);
    binding.contentMainLayout.recyclerView.setItemAnimator(new DefaultItemAnimator());
    binding.contentMainLayout.recyclerView.setAdapter(mFastAdapter);
    binding.contentMainLayout.recyclerView.setFocusable(false);
    mFastAdapter.withOnClickListener(new OnClickListener<FiveDayWeather>() {
      @Override
      public boolean onClick(@Nullable View v, @NonNull IAdapter<FiveDayWeather> adapter, @NonNull FiveDayWeather item, int position) {
        Intent intent = new Intent(MainActivity.this, HourlyActivity.class);
        intent.putExtra(Constants.FIVE_DAY_WEATHER_ITEM, item);
        startActivity(intent);
        return true;
      }
    });
  }

  private void showStoredCurrentWeather() {
    Query<CurrentWeather> query = DbUtil.getCurrentWeatherQuery(currentWeatherBox);
    query.subscribe(subscriptions).on(AndroidScheduler.mainThread())
        .observer(new DataObserver<List<CurrentWeather>>() {
          @Override
          public void onData(@NonNull List<CurrentWeather> data) {
            if (data.size() > 0) {
              hideEmptyLayout();
              CurrentWeather currentWeather = data.get(0);
              if (isLoad) {
                binding.contentMainLayout.tempTextView.setText(String.format(Locale.getDefault(), "%.0f??", currentWeather.getTemp()));
                binding.contentMainLayout.descriptionTextView.setText(AppUtil.getWeatherStatus(currentWeather.getWeatherId(), AppUtil.isRTL(MainActivity.this)));
                binding.contentMainLayout.humidityTextView.setText(String.format(Locale.getDefault(), "%d%%", currentWeather.getHumidity()));
                binding.contentMainLayout.windTextView.setText(String.format(Locale.getDefault(), getResources().getString(R.string.wind_unit_label), currentWeather.getWindSpeed()));
              } else {
                binding.contentMainLayout.tempTextView.setCurrentText(String.format(Locale.getDefault(), "%.0f??", currentWeather.getTemp()));
                binding.contentMainLayout.descriptionTextView.setCurrentText(AppUtil.getWeatherStatus(currentWeather.getWeatherId(), AppUtil.isRTL(MainActivity.this)));
                binding.contentMainLayout.humidityTextView.setCurrentText(String.format(Locale.getDefault(), "%d%%", currentWeather.getHumidity()));
                binding.contentMainLayout.windTextView.setCurrentText(String.format(Locale.getDefault(), getResources().getString(R.string.wind_unit_label), currentWeather.getWindSpeed()));
              }
              binding.contentMainLayout.animationView.setAnimation(AppUtil.getWeatherAnimation(currentWeather.getWeatherId()));
              binding.contentMainLayout.animationView.playAnimation();
            }
          }
        });
  }

  private void showStoredFiveDayWeather() {
    Query<FiveDayWeather> query = DbUtil.getFiveDayWeatherQuery(fiveDayWeatherBox);
    query.subscribe(subscriptions).on(AndroidScheduler.mainThread())
        .observer(new DataObserver<List<FiveDayWeather>>() {
          @Override
          public void onData(@NonNull List<FiveDayWeather> data) {
            if (data.size() > 0) {
              todayFiveDayWeather = data.remove(0);
              mItemAdapter.clear();
              mItemAdapter.add(data);
            }
          }
        });
  }

  private void checkLastUpdate() {
    cityInfo = prefser.get(Constants.CITY_INFO, CityInfo.class, null);
    if (cityInfo != null) {
      binding.toolbarLayout.cityNameTextView.setText(String.format("%s, %s", cityInfo.getName(), cityInfo.getCountry()));
      if (prefser.contains(Constants.LAST_STORED_CURRENT)) {
        long lastStored = prefser.get(Constants.LAST_STORED_CURRENT, Long.class, 0L);
        if (AppUtil.isTimePass(lastStored)) {
          requestWeather(cityInfo.getName(), false);
        }
      } else {
        requestWeather(cityInfo.getName(), false);
      }
    } else {
      showEmptyLayout();
    }

  }


  private void requestWeather(String cityName, boolean isSearch) {
    if (AppUtil.isNetworkConnected()) {
      getCurrentWeather(cityName, isSearch);
      getFiveDaysWeather(cityName);
    }
    else {
      SnackbarUtil
          .with(binding.swipeContainer)
          .setMessage(getString(R.string.no_internet_message))
          .setDuration(SnackbarUtil.LENGTH_LONG)
          .showError();
      binding.swipeContainer.setRefreshing(false);
    }
  }
// HAM TRA VE THOI TIET HIEN TAI BANG TEN THANH PHO
  private void getCurrentWeather(String cityName, boolean isSearch) {
    apiKey =getString( R.string.KEYAPI1);
    disposable.add(
            apiService.getCurrentWeather(
                    cityName, Constants.UNITS, defaultLang, apiKey)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeWith(new DisposableSingleObserver<CurrentWeatherResponse>() {
                      @Override
                      public void onSuccess(CurrentWeatherResponse currentWeatherResponse) {
                        isLoad = true;
                        storeCurrentWeather(currentWeatherResponse);
                        storeCityInfo(currentWeatherResponse);
                        binding.swipeContainer.setRefreshing(false);
                        if (isSearch) {
                          prefser.remove(Constants.LAST_STORED_MULTIPLE_DAYS);
                        }
                      }

                      @Override
                      public void onError(Throwable e) {
                        binding.swipeContainer.setRefreshing(false);
                        try {
                          HttpException error = (HttpException) e;
                          handleErrorCode(error);
                        } catch (Exception exception) {
                          e.printStackTrace();
                        }
                      }
                    })

    );
  }
  private void getCurrentWeatherByLoc(double lat, double lon) {

    apiKey ="2218dd462ace07b9da0027d374c6a7e6";
    disposable.add(
            apiService.getCurrentWeatherbyLoc (
                    lat,lon, Constants.UNITS, defaultLang, apiKey)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeWith(new DisposableSingleObserver<CurrentWeatherResponse>() {
                      @Override
                      public void onSuccess(CurrentWeatherResponse currentWeatherResponse) {
                        isLoad = true;
                        storeCurrentWeather(currentWeatherResponse);
                        binding.swipeContainer.setRefreshing(false);

                      }

                      @Override
                      public void onError(Throwable e) {
                        binding.swipeContainer.setRefreshing(false);
                        try {
                          HttpException error = (HttpException) e;
                          handleErrorCode(error);
                        } catch (Exception exception) {
                          e.printStackTrace();
                        }
                      }
                    })

    );
  }

  private void handleErrorCode(HttpException error) {
    if (error.code() == 404) {
      SnackbarUtil
          .with(binding.swipeContainer)
          .setMessage(getString(R.string.no_city_found_message))
          .setDuration(SnackbarUtil.LENGTH_INDEFINITE)
          .setAction(getResources().getString(R.string.search_label), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
              binding.toolbarLayout.searchView.showSearch();
            }
          })
          .showWarning();

    } else if (error.code() == 401) {
      SnackbarUtil
          .with(binding.swipeContainer)
          .setMessage(getString(R.string.invalid_api_key_message))
          .setDuration(SnackbarUtil.LENGTH_INDEFINITE)
          .setAction(getString(R.string.ok_label), new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
          })
          .showError();

    } else {
      SnackbarUtil
          .with(binding.swipeContainer)
          .setMessage(getString(R.string.network_exception_message))
          .setDuration(SnackbarUtil.LENGTH_LONG)
          .setAction(getResources().getString(R.string.retry_label), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
              if (cityInfo != null) {
                requestWeather(cityInfo.getName(), false);
              } else {
                binding.toolbarLayout.searchView.showSearch();
              }
            }
          })
          .showWarning();
    }
  }

  private void showEmptyLayout() {
    Glide.with(MainActivity.this).load(R.drawable.no_city).into(binding.contentEmptyLayout.noCityImageView);
    binding.contentEmptyLayout.emptyLayout.setVisibility(View.VISIBLE);
    binding.contentMainLayout.nestedScrollView.setVisibility(View.GONE);
  }

  private void hideEmptyLayout() {
    binding.contentEmptyLayout.emptyLayout.setVisibility(View.GONE);
    binding.contentMainLayout.nestedScrollView.setVisibility(View.VISIBLE);
  }


  private void storeCurrentWeather(CurrentWeatherResponse response) {
    CurrentWeather currentWeather = new CurrentWeather();
    currentWeather.setTemp(response.getMain().getTemp());
    currentWeather.setHumidity(response.getMain().getHumidity());
    currentWeather.setDescription(response.getWeather().get(0).getDescription());
    currentWeather.setMain(response.getWeather().get(0).getMain());
    currentWeather.setWeatherId(response.getWeather().get(0).getId());
    currentWeather.setWindDeg(response.getWind().getDeg());
    currentWeather.setWindSpeed(response.getWind().getSpeed());
    currentWeather.setStoreTimestamp(System.currentTimeMillis());
    prefser.put(Constants.LAST_STORED_CURRENT, System.currentTimeMillis());
    if (!currentWeatherBox.isEmpty()) {
      currentWeatherBox.removeAll();
      currentWeatherBox.put(currentWeather);
    } else {
      currentWeatherBox.put(currentWeather);
    }
  }

  private void storeCityInfo(CurrentWeatherResponse response) {
    CityInfo cityInfo = new CityInfo();
    cityInfo.setCountry(response.getSys().getCountry());
    cityInfo.setId(response.getId());
    cityInfo.setName(response.getName());
    prefser.put(Constants.CITY_INFO, cityInfo);
    binding.toolbarLayout.cityNameTextView.setText(String.format("%s, %s", cityInfo.getName(), cityInfo.getCountry()));
  }

  private void getFiveDaysWeather(String cityName) {
    disposable.add(
        apiService.getMultipleDaysWeather(
            cityName, Constants.UNITS, defaultLang, 3, apiKey)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeWith(new DisposableSingleObserver<MultipleDaysWeatherResponse>() {
              @Override
              public void onSuccess(MultipleDaysWeatherResponse response) {
                handleFiveDayResponse(response, cityName);
              }

              @Override
              public void onError(Throwable e) {
                e.printStackTrace();
              }
            })
    );
  }

  private void handleFiveDayResponse(MultipleDaysWeatherResponse response, String cityName) {
    fiveDayWeathers = new ArrayList<>();
    List<ListItem> list = response.getList();
    int day = 0;
    for (ListItem item : list) {
      int color = colors[day];
      int colorAlpha = colorsAlpha[day];
      Calendar calendar = Calendar.getInstance(TimeZone.getDefault());
      Calendar newCalendar = AppUtil.addDays(calendar, day);
      FiveDayWeather fiveDayWeather = new FiveDayWeather();
      fiveDayWeather.setWeatherId(item.getWeather().get(0).getId());
      fiveDayWeather.setDt(item.getDt());
      fiveDayWeather.setMaxTemp(item.getTemp().getMax());
      fiveDayWeather.setMinTemp(item.getTemp().getMin());
      fiveDayWeather.setTemp(item.getTemp().getDay());
      fiveDayWeather.setColor(color);
      fiveDayWeather.setColorAlpha(colorAlpha);
      fiveDayWeather.setTimestampStart(AppUtil.getStartOfDayTimestamp(newCalendar));
      fiveDayWeather.setTimestampEnd(AppUtil.getEndOfDayTimestamp(newCalendar));
      fiveDayWeathers.add(fiveDayWeather);
      day++;
    }
    getFiveDaysHourlyWeather(mLastLocation.getLatitude (),mLastLocation.getLongitude ());
  }

  private void getFiveDaysHourlyWeather(double lat , double lon) {

    disposable.add(
        apiService.getFiveDaysWeather(
            lat,lon, Constants.UNITS, defaultLang, apiKey)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeWith(new DisposableSingleObserver<FiveDayResponse>() {
              @Override
              public void onSuccess(FiveDayResponse response) {
                handleFiveDayHourlyResponse(response);
              }

              @Override
              public void onError(Throwable e) {
                e.printStackTrace();
              }
            })

    );
  }

  private void handleFiveDayHourlyResponse(FiveDayResponse response) {
    if (!fiveDayWeatherBox.isEmpty()) {
      fiveDayWeatherBox.removeAll();
    }
    if (!itemHourlyDBBox.isEmpty()) {
      itemHourlyDBBox.removeAll();
    }
    for (FiveDayWeather fiveDayWeather : fiveDayWeathers) {
      long fiveDayWeatherId = fiveDayWeatherBox.put(fiveDayWeather);
      ArrayList<ItemHourly> listItemHourlies = new ArrayList<>(response.getList());
      for (ItemHourly itemHourly : listItemHourlies) {
        Calendar calendar = Calendar.getInstance(TimeZone.getDefault());
        calendar.setTimeInMillis(itemHourly.getDt() * 1000L);
        if (calendar.getTimeInMillis()
            <= fiveDayWeather.getTimestampEnd()
            && calendar.getTimeInMillis()
            > fiveDayWeather.getTimestampStart()) {
          ItemHourlyDB itemHourlyDB = new ItemHourlyDB();
          itemHourlyDB.setDt(itemHourly.getDt());
          itemHourlyDB.setFiveDayWeatherId(fiveDayWeatherId);
          itemHourlyDB.setTemp(itemHourly.getMain().getTemp());
          itemHourlyDB.setWeatherCode(itemHourly.getWeather().get(0).getId());
          itemHourlyDBBox.put(itemHourlyDB);
        }
      }
    }
  }


  @Override
  protected void onDestroy() {
    super.onDestroy();
    disposable.dispose();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.menu_main, menu);
    MenuItem item = menu.findItem(R.id.action_search);
    binding.toolbarLayout.searchView.setMenuItem(item);
    return true;
  }

  public void showAboutFragment() {
    AppUtil.showFragment(new AboutFragment(), getSupportFragmentManager(), true);
  }

  @Override
  public void onBackPressed() {
    if (binding.toolbarLayout.searchView.isSearchOpen()) {
      binding.toolbarLayout.searchView.closeSearch();
    } else {
      super.onBackPressed();
    }
  }
  private void permissionVersion(){
    if(Build.VERSION.SDK_INT >= 23){//Android 6 tro di

      requestLocationPermissions();
    }else {
      // D?????i 6.0
      // Nh???n th???y r???ng mi???n l?? quy???n ???????c ????ng k?? trong AndroidManifest.xml, th?? ??i???u ???? s??? ???????c coi l?? quy???n ???? ???????c c???p v?? nh???c
      Toast.makeText ( this, "Nh???n di???n thi???t b??? c???a b???n d?????i Android 6", Toast.LENGTH_SHORT ).show ();
    }
  }
  private void requestLocationPermissions() {
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
      ActivityCompat.requestPermissions(this,
              new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
              REQUEST_LOCATION_PERMISSION);
    }
  }


  @Override
  public void onLocationChanged(Location location) {
    Log.e (TAG, String.format(Locale.getDefault(), "onLocationChanged : %f, %f",
            location.getLatitude(), location.getLongitude()));
    mLastLocation = location;
    if (mIsAutoUpdateLocation) {
      updateUi();
    }
  }

  private void updateUi() {
    if (mLastLocation != null) {
      textView.setText(String.format(Locale.getDefault(), "%f",
              mLastLocation.getLatitude()));
  }}

  @Override
  public void onStatusChanged(String provider, int status, Bundle extras) {

  }

  @Override
  public void onProviderEnabled(String provider) {

  }

  @Override
  public void onProviderDisabled(String provider) {

  }
}
