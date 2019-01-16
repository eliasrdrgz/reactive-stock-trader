package com.redelastic.stocktrader.portfolio.api;

import akka.Done;
import akka.NotUsed;
import com.lightbend.lagom.javadsl.api.Descriptor;
import com.lightbend.lagom.javadsl.api.Service;
import com.lightbend.lagom.javadsl.api.ServiceCall;
import com.lightbend.lagom.javadsl.api.broker.Topic;
import com.redelastic.stocktrader.order.OrderDetails;

import static com.lightbend.lagom.javadsl.api.Service.*;

/**
 *
 */
public interface PortfolioService extends Service {

    ServiceCall<OpenPortfolioDetails, String> openPortfolio();

    /**
     * Place an order for a particular portfolio.
     * @param portfolioId ID for the portfolio placing the order.
     * @return Done when the order has been accepted. For a sell order this requires confirming that the
     * requested number of shares are available to be sold.
     */
    ServiceCall<OrderDetails, Done> placeOrder(String portfolioId);

    /**
     * Sell all equities (as market sell), then transfer all funds out, then close the portfolio.
     * Note that after selling all the equities the portfolio may still be in an overdrawn state (negative funds),
     * we will not close it until the balance of funds is zero. External action will be required to complete closure
     * of an overdrawn account.
     * @param portfolioId ID for the portfolio to liquidate.
     * @return Done when the liquidate command has been acknowledged.
     */
    ServiceCall<NotUsed, Done> liquidatePortfolio(String portfolioId);

    /**
     * Get a view of the portfolio, including the current valuation of the equities held in it.
     * @param portfolioId ID of the portfolio to view.
     * @return The current portfolio's state.
     */
    ServiceCall<NotUsed, PortfolioView> getPortfolio(String portfolioId);

    /**
     * The orders placed by portfolios managed by this service.
     * @return Orders placed by portfolios.
     */
    Topic<OrderPlaced> orderPlaced();

    String ORDERS_TOPIC_ID = "Portfolio-OrderPlaced";

    @Override
    default Descriptor descriptor() {
        // Map with auto ACL to provide access to this API through the Lagom gateway

        // @formatter:off
        return named("portfolio").withCalls(
                // Use restCall to make it explicit that this is an ordinary HTTP endpoint
                pathCall("/api/portfolio", this::openPortfolio),
                pathCall("/api/portfolio/:portfolioId/liquidate", this::liquidatePortfolio),
                pathCall("/api/portfolio/:portfolioId", this::getPortfolio),
                pathCall("/api/portfolio/:portfolioId/placeOrder", this::placeOrder)
        ).withTopics(
            topic(ORDERS_TOPIC_ID, this::orderPlaced)
        );
        // @formatter:on

    }
}
