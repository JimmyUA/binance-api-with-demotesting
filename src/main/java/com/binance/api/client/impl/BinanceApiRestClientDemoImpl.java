package com.binance.api.client.impl;

import com.binance.api.client.domain.OrderSide;
import com.binance.api.client.domain.OrderStatus;
import com.binance.api.client.domain.account.*;
import com.binance.api.client.domain.account.request.CancelOrderRequest;
import com.binance.api.client.domain.account.request.OrderRequest;
import com.binance.api.client.domain.account.request.OrderStatusRequest;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BinanceApiRestClientDemoImpl extends BinanceApiRestClientImpl {

    private static long lastOrderId = 0;

    private String baseCurrency = "ETH";
    private String tradeCurrency = "BNB";
    private Account demoAccount;
    private List<Order> openedOrders = new ArrayList<>();

    public BinanceApiRestClientDemoImpl(String apiKey, String secret) {
       super(apiKey, secret);
       demoAccount = new Account();
    }

    public void setBaseCurrency(String baseCurrency) {
        this.baseCurrency = baseCurrency;
    }

    public void setTradeCurrency(String tradeCurrency) {
        this.tradeCurrency = tradeCurrency;
        initAccount();
    }

    private void initAccount() {
        addBaseAndTradeBalances();
    }

    private void addBaseAndTradeBalances() {
        AssetBalance initialBaseBalance = getInitialBaseBalance();
        AssetBalance initialTradeBalance = getInitialTradeBalance(tradeCurrency);

        List<AssetBalance> balances = Arrays.asList(initialTradeBalance, initialBaseBalance);
        demoAccount.setBalances(balances);
    }

    private AssetBalance getInitialTradeBalance(String tradeCurrency) {
        AssetBalance assetBalance = new AssetBalance();
        assetBalance.setAsset(tradeCurrency);
        assetBalance.setFree("0.0");
        assetBalance.setLocked("0.0");
        return assetBalance;
    }

    private AssetBalance getInitialBaseBalance() {
        AssetBalance assetBalance = new AssetBalance();

        assetBalance.setAsset(baseCurrency);
        assetBalance.setFree("1.0");
        assetBalance.setLocked("0.0");
        return assetBalance;
    }


    @Override
    public Account getAccount() {
        return demoAccount;
    }

    @Override
    public List<Order> getOpenOrders(OrderRequest orderRequest) {
        return openedOrders;
    }

    @Override
    public void cancelOrder(CancelOrderRequest cancelOrderRequest) {
        openedOrders.stream().filter(order -> order.getOrderId().equals(cancelOrderRequest.getOrderId()))
                .forEach(order -> order.setStatus(OrderStatus.CANCELED));
    }

    @Override
    public NewOrderResponse newOrder(NewOrder order) {
        Long thisOrderId = ++lastOrderId;

        Order notNewOrder = getNotNewOrder(order, thisOrderId);
        openedOrders.add(notNewOrder);
        String tradeTokensInOrder = order.getQuantity();
        String pairPrice = order.getPrice();
        if (pairPrice == null){
            pairPrice = getOrderBook(tradeCurrency+baseCurrency, 5).getAsks().get(0).getPrice();
        }
        Double tradeTokensInOrderValue = Double.valueOf(tradeTokensInOrder);
        Double paid = tradeTokensInOrderValue * Double.valueOf(pairPrice);
        String baseTokensInOrder = paid.toString();
        AssetBalance baseAssetBalance = demoAccount.getAssetBalance(baseCurrency);
        String currentBaseAmount = baseAssetBalance.getFree();
        AssetBalance tradeAssetBalance = demoAccount.getAssetBalance(tradeCurrency);
        String currentTradeAmount = tradeAssetBalance.getFree();
        Double currentBaseAmountValue = Double.valueOf(currentBaseAmount);
        Double baseTokensInOrderValue = Double.valueOf(baseTokensInOrder);
        Double currentTradeAmountValue = Double.valueOf(currentTradeAmount);
        if (OrderSide.BUY.equals(notNewOrder.getSide())){

            Double newBaseAmount = currentBaseAmountValue - baseTokensInOrderValue;
            baseAssetBalance.setFree(newBaseAmount.toString());

            Double newTradeAmount = currentTradeAmountValue + tradeTokensInOrderValue;
            tradeAssetBalance.setFree(newTradeAmount.toString());

        } else if (OrderSide.SELL.equals(notNewOrder.getSide())){
            Double newBaseAmount = currentBaseAmountValue + baseTokensInOrderValue;
            baseAssetBalance.setFree(newBaseAmount.toString());

            Double newTradeAmount = currentTradeAmountValue - tradeTokensInOrderValue;
            tradeAssetBalance.setFree(newTradeAmount.toString());
        }

        NewOrderResponse newOrderResponse = new NewOrderResponse();
        newOrderResponse.setOrderId(thisOrderId);
        newOrderResponse.setSymbol(order.getSymbol());
        newOrderResponse.setTransactTime(LocalDate.now().toEpochDay());
        return newOrderResponse;
    }

    private Order getNotNewOrder(NewOrder order, Long thisOrderId) {
        Order result = new Order();
        result.setStatus(OrderStatus.FILLED);
        result.setExecutedQty(order.getQuantity());
        result.setOrderId(thisOrderId);
        result.setOrigQty(order.getQuantity());
        result.setPrice(order.getPrice());
        result.setSide(order.getSide());
        result.setStopPrice(order.getStopPrice());
        result.setSymbol(order.getSymbol());
        result.setTime(order.getTimestamp());
        result.setTimeInForce(order.getTimeInForce());
        result.setType(order.getType());
        return result;
    }

    @Override
    public Order getOrderStatus(OrderStatusRequest orderStatusRequest) {

        Long goalOrderId = orderStatusRequest.getOrderId();
        Order goalOrder = openedOrders.stream().filter(order -> order.getOrderId().equals(goalOrderId)).findFirst().get();

        return goalOrder;
    }
}
