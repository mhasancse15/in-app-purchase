package com.mhasancse.coin;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.ConsumeResponseListener;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.crashlytics.buildtools.reloc.com.google.common.collect.ImmutableList;


import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements RecycleViewInterface {

    BillingClient billingClient;
    Button btn_use_coins;
    RecyclerView recyclerView;
    TextView txt_coins;
    List<ProductDetails> productDetailsList;
    Activity activity;

    Toolbar toolbar;
    Handler handler;
    ProgressBar loadProducts;
    BuyCoinsAdapter adapter;
    ArrayList<Integer> coins;
    ArrayList<String> productIds;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();

        billingClient = BillingClient.newBuilder(this)
                .enablePendingPurchases()
                .setListener(
                        (billingResult, list) -> {
                            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && list != null) {
                                for (Purchase purchase : list) {
                                    verifyPurchase(purchase);
                                }
                            }
                        }
                ).build();

        //start the connection after initializing the billing client
        connectGooglePlayBilling();

    }

    void connectGooglePlayBilling() {
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingServiceDisconnected() {
                connectGooglePlayBilling();
            }

            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    showProducts();
                }
            }
        });
    }

    @SuppressLint("SetTextI18n")
    void showProducts() {

        ImmutableList<QueryProductDetailsParams.Product> productList = ImmutableList.of(
                //Product 1 : YOUR PRODUCT ID
                QueryProductDetailsParams.Product.newBuilder()
                        .setProductId("YOUR PRODUCT ID")
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build(),

                //Product 2 : YOUR PRODUCT ID
                QueryProductDetailsParams.Product.newBuilder()
                        .setProductId("YOUR PRODUCT ID")
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
        );

        QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
                .setProductList(productList)
                .build();

        billingClient.queryProductDetailsAsync(params, (billingResult, list) -> {
            productDetailsList.clear();
            handler.postDelayed(() -> {
                loadProducts.setVisibility(View.INVISIBLE); //
                productDetailsList.addAll(list);

                adapter = new BuyCoinsAdapter(getApplicationContext(), productDetailsList, MainActivity.this);
                recyclerView.setHasFixedSize(true);
                recyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this, LinearLayoutManager.VERTICAL, false));
                recyclerView.setAdapter(adapter);

            }, 2000);
        });
    }

    void launchPurchaseFlow(ProductDetails productDetails) {

        ImmutableList<BillingFlowParams.ProductDetailsParams> productDetailsParamsList =
                ImmutableList.of(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                                .setProductDetails(productDetails)
                                .build()
                );
        BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productDetailsParamsList)
                .build();

        billingClient.launchBillingFlow(activity, billingFlowParams);
    }

    void verifyPurchase(Purchase purchase) {
        Log.d("testCoins", "Verify Purchase " + purchase.toString());
        ConsumeParams consumeParams = ConsumeParams.newBuilder()
                .setPurchaseToken(purchase.getPurchaseToken())
                .build();
        ConsumeResponseListener listener = (billingResult, s) -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                giveUserCoins(purchase);
            }
        };

        billingClient.consumeAsync(consumeParams, listener);
    }


    @SuppressLint("SetTextI18n")
    private void initViews() {

        handler = new Handler();
        activity = this;

        recyclerView = findViewById(R.id.recyclerview);
        loadProducts = findViewById(R.id.loadProducts);


        productDetailsList = new ArrayList<>();

     //   txt_coins.setText("0"+ " Coin(s)");

        productIds = new ArrayList<>();
        coins = new ArrayList<>();
        //Change Product Id here
        productIds.add("YOUR PRODUCT ID");
        coins.add(2500);

        //Change Product Id here
        productIds.add("YOUR PRODUCT ID");
        coins.add(15000);

    }

    @SuppressLint("SetTextI18n")
    void giveUserCoins(Purchase purchase) {
        for (int i = 0; i < productIds.size(); i++) {
            if (purchase.getProducts().get(0).equals(productIds.get(i))) {
                //set coins
                int boughtCoins = coins.get(i) * purchase.getQuantity();

                Log.d("testCoins", "bought Coins: " + boughtCoins);
                //Update UI
               // txt_coins.setText(boughtCoins + " Coin(s)");
            }
        }
    }

    protected void onResume() {
        super.onResume();
        billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build(),
                (billingResult, list) -> {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                        for (Purchase purchase : list) {
                            if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged()) {
                                verifyPurchase(purchase);
                            }
                        }
                    }
                }
        );
    }

    private void reloadScreen() {
        //Reload the screen to activate the removeAd and remove the actual Ad off the screen.
        finish();
        overridePendingTransition(0, 0);
        startActivity(getIntent());
        overridePendingTransition(0, 0);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(activity, MainActivity.class));
        finish();
    }

    @Override
    public void onItemClick(int pos) {
        launchPurchaseFlow(productDetailsList.get(pos));
    }

    public void showSnackBar(View view, String message) {
        Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show();
    }
}