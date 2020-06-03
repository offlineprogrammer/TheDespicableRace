package com.stuartminion.thedespicablerace;

import androidx.appcompat.app.AppCompatActivity;

import com.amazon.device.iap.PurchasingListener;
import com.amazon.device.iap.PurchasingService;
import com.amazon.device.iap.model.FulfillmentResult;
import com.amazon.device.iap.model.Product;
import com.amazon.device.iap.model.ProductDataResponse;
import com.amazon.device.iap.model.PurchaseResponse;
import com.amazon.device.iap.model.PurchaseUpdatesResponse;
import com.amazon.device.iap.model.Receipt;
import com.amazon.device.iap.model.UserDataResponse;


import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    String shinyWheelsSKU = "com.stuartminion.thedespicablerace.shinywheels";
    Handler handler;
    //Define UserId and MarketPlace
    private String currentUserId;
    private String currentMarketplace;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setupIAP();
    }

    private void setupIAP() {


        Log.d(TAG, "onCreate: registering PurchasingListener");
        PurchasingService.registerListener(this, purchasingListener);
        Button raceCarButton =  findViewById(R.id.raceCarButton);
        raceCarButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PurchasingService.purchase(shinyWheelsSKU);
            }
        });

        //create a handler for the UI changes
        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                if (msg.obj.equals(shinyWheelsSKU)) {
                    Log.i(TAG, "handleMessage parentSKU: Complete");
                    ImageView wheelsImageView = findViewById(R.id.wheelsImageView);
                    wheelsImageView.setVisibility(View.VISIBLE);
                }
            }
        };
    }


    @Override
    protected void onResume() {
        super.onResume();
        //getUserData() will query the Appstore for the Users information
        PurchasingService.getUserData();
        //getPurchaseUpdates() will query the Appstore for any previous purchase
        PurchasingService.getPurchaseUpdates(true);
        //getProductData will validate the SKUs with Amazon Appstore
        final Set<String> productSkus = new HashSet<String>();
        productSkus.add(shinyWheelsSKU);
        PurchasingService.getProductData(productSkus);
        Log.i(TAG, "onResume: Validate the SKU");
    }


    PurchasingListener purchasingListener = new PurchasingListener() {
        @Override
        public void onUserDataResponse(UserDataResponse response) {
            final UserDataResponse.RequestStatus status = response.getRequestStatus();
            switch (status) {
                case SUCCESSFUL:
                    currentUserId = response.getUserData().getUserId();
                    currentMarketplace = response.getUserData().getMarketplace();
                    Log.i(TAG, String.format(" userId : %s\n MarketPlace: %s\n", currentUserId, currentMarketplace));
                    break;
                case FAILED:
                case NOT_SUPPORTED:
                    // Fail gracefully.
                    break;
            }
        }
        @Override
        public void onProductDataResponse(ProductDataResponse productDataResponse) {
            switch (productDataResponse.getRequestStatus()) {
                case SUCCESSFUL:

                    //get informations for all IAP Items (parent SKUs)
                    final Map<String, Product> products = productDataResponse.getProductData();
                    for ( String key : products.keySet()) {
                        Product product = products.get(key);
                        Log.i(TAG, String.format( "Product: %s\n Type: %s\n SKU: %s\n Price: %s\n Description: %s\n" , product.getTitle(), product.getProductType(),
                                product.getSku(), product.getPrice(), product.getDescription()));
                    }
                    //get all unavailable SKUs
                    for ( String s : productDataResponse.getUnavailableSkus()) {
                        Log.i(TAG, "Unavailable SKU:" + s);
                    }
                    break;
                case FAILED:

                    Log.i(TAG, "onProductDataResponse: Failed");
                    break ;
                case NOT_SUPPORTED:
                    Log.i(TAG, "onProductDataResponse: NOT_SUPPORTED");
                    break ;
            }
        }
        @Override
        public void onPurchaseResponse(PurchaseResponse purchaseResponse) {
            switch (purchaseResponse.getRequestStatus()) {
                case SUCCESSFUL:
                    Receipt receipt = purchaseResponse.getReceipt();
                    Log.i(TAG, "onPurchaseResponse: SKU is "+ receipt.getSku());
                    PurchasingService.notifyFulfillment(purchaseResponse.getReceipt().getReceiptId(),
                            FulfillmentResult.FULFILLED);
                    break ;
                case FAILED:
                    break ;
            }
        }
        @Override
        public void onPurchaseUpdatesResponse(PurchaseUpdatesResponse response) {
            switch (response.getRequestStatus()) {
                case SUCCESSFUL:
                    Log.i(TAG, "onPurchaseUpdatesResponse: response.getReceipts() is "+ response.getReceipts().size() );
                    for ( final Receipt receipt : response.getReceipts()) {
                        // Process receipts
                        if (!receipt.isCanceled()){
                            Log.i(TAG, "onPurchaseUpdatesResponse: SKU is " + receipt.getSku());
                            Message m= new Message();
                            m.obj=receipt.getSku();
                            handler.handleMessage(m);
                        } else {
                            Log.i(TAG, "onPurchaseUpdatesResponse: cancelled SKU " + receipt.getSku());
                        }
                    }
                    if (response.hasMore()) {
                        Log.i(TAG, "onPurchaseUpdatesResponse: has more");
                        PurchasingService.getPurchaseUpdates(true);
                    }
                    break ;
                case FAILED:
                    Log.i(TAG, "onPurchaseUpdatesResponse: Failed");
                    break ;
            }
        }
    };

}