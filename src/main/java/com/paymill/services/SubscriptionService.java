package com.paymill.services;

import java.util.Date;
import java.util.List;

import javax.ws.rs.core.MultivaluedMap;

import com.paymill.models.Client;
import com.paymill.models.Interval;
import com.paymill.models.Offer;
import com.paymill.models.Payment;
import com.paymill.models.PaymillList;
import com.paymill.models.Subscription;
import com.paymill.models.Subscription.Creator;
import com.paymill.models.Transaction;
import com.sun.jersey.core.util.MultivaluedMapImpl;

/**
 * The {@link SubscriptionService} is used to list, create, edit, delete and update PAYMILL {@link Subscription}s.
 * @author Vassil Nikolov
 * @since 3.0.0
 */
public class SubscriptionService extends AbstractService {

  private final static String PATH = "/subscriptions";

  private SubscriptionService( com.sun.jersey.api.client.Client httpClient ) {
    super( httpClient );
  }

  /**
   * This function returns a {@link List} of PAYMILL {@link Subscription} objects.
   * @return {@link PaymillList} which contains a {@link List} of PAYMILL {@link Subscription}s and their total count.
   */
  public PaymillList<Subscription> list() {
    return this.list( null, null, null, null );
  }

  /**
   * This function returns a {@link List} of PAYMILL {@link Subscription} objects, overriding the default count and offset.
   * @param count
   *          Max {@link Integer} of returned objects in the {@link PaymillList}
   * @param offset
   *          {@link Integer} to start from.
   * @return {@link PaymillList} which contains a {@link List} of PAYMILL {@link Subscription}s and their total count.
   */
  public PaymillList<Subscription> list( Integer count, Integer offset ) {
    return this.list( null, null, count, offset );
  }

  /**
   * This function returns a {@link List} of PAYMILL {@link Subscription} objects. In which order this list is returned depends on
   * the optional parameters. If <code>null</code> is given, no filter or order will be applied.
   * @param filter
   *          {@link Subscription.Filter} or <code>null</code>
   * @param order
   *          {@link Subscription.Order} or <code>null</code>
   * @return {@link PaymillList} which contains a {@link List} of PAYMILL {@link Subscription}s and their total count.
   */
  public PaymillList<Subscription> list( Subscription.Filter filter, Subscription.Order order ) {
    return this.list( filter, order, null, null );
  }

  /**
   * This function returns a {@link List} of PAYMILL {@link Subscription} objects. In which order this list is returned depends on
   * the optional parameters. If <code>null</code> is given, no filter or order will be applied, overriding the default count and
   * offset.
   * @param filter
   *          {@link Subscription.Filter} or <code>null</code>
   * @param order
   *          {@link Subscription.Order} or <code>null</code>
   * @param count
   *          Max {@link Integer} of returned objects in the {@link PaymillList}
   * @param offset
   *          {@link Integer} to start from.
   * @return {@link PaymillList} which contains a {@link List} of PAYMILL {@link Subscription}s and their total count.
   */
  public PaymillList<Subscription> list( Subscription.Filter filter, Subscription.Order order, Integer count, Integer offset ) {
    return RestfulUtils.list( SubscriptionService.PATH, filter, order, count, offset, Subscription.class, super.httpClient );
  }

  /**
   * This function refresh and returns the detailed information of the concrete requested {@link Subscription}.
   * @param subscription
   *          A {@link Subscription} with Id.
   * @return Refreshed instance of the given {@link Subscription}.
   */
  public Subscription get( Subscription subscription ) {
    return RestfulUtils.show( SubscriptionService.PATH, subscription, Subscription.class, super.httpClient );
  }

  /**
   * This function refresh and returns the detailed information of the concrete requested {@link Subscription}.
   * @param subscriptionId
   *          The Id of an existing {@link Subscription}.
   * @return Refreshed instance of the given {@link Subscription}.
   */
  public Subscription get( String subscriptionId ) {
    return this.get( new Subscription( subscriptionId ) );
  }

  /**
   * This function creates a {@link Subscription}. Use any of the static create methods in {@link Subscription} and include
   * additional options.<br />
   * <strong>Example:</strong><br />
   * <blockquote>
   * 
   * <pre>
   * paymill.getSubscriptionService().create( Subscription.create( "pay_123", "offer_123" ).withClient( "client_123" ))
   * paymill.getSubscriptionService().create( Subscription.create( "pay_123", "offer_123" ).withAmount( 100 )) overrides the amount of "offer_123" and sets it to 100 for this subscription
   * </pre>
   * 
   * </blockquote>
   * @param creator
   *          see {@link Subscription.Creator}.
   * @return the subscription.
   */
  public Subscription create( Creator creator ) {
    return create( creator.getPayment(), creator.getClient(), creator.getOffer(), creator.getAmount(), creator.getCurrency(), creator.getInterval(),
        creator.getStartAt(), creator.getName(), creator.getPeriodOfValidity() );
  }

  /**
   * This function creates a {@link Subscription} between a {@link Client} and an {@link Offer}. A {@link Client} can have several
   * {@link Subscription}s to different {@link Offer}s, but only one {@link Subscription} to the same {@link Offer}. The
   * {@link Client}s is charged for each billing interval entered. <br />
   * <strong>NOTE</strong><br />
   * As the Subscription create method has a lot of options, we recommend you to use a {@link Subscription.Creator}.
   * @param payment
   *          A {@link Payment} used for charging.
   * @param client
   * @param offer
   *          An {@link Offer} to subscribe to. Mandatory only if amount, curreny and interval are not set
   * @param amount
   *          Amount to be charged. Mandatory if offer is null.
   * @param currency
   *          Currency in which to be charged. Mandatory if offer is null.
   * @param interval
   *          Interval of charging. Mandatory if offer is null.
   * @param startAt
   *          The date, when the subscription will start charging. If longer than 10 minutes in the future, a preauthorization
   *          will occur automatically to verify the payment.
   * @param name
   *          A name for this subscription
   * @param periodOfValidity
   *          if set, the subscription will expire after this period.
   * @return the subscription.
   */
  public Subscription create( Payment payment, Client client, Offer offer, Integer amount, String currency, Interval.PeriodWithChargeDay interval, Date startAt,
      String name, Interval.Period periodOfValidity ) {

    if( offer == null && (amount == null || currency == null || interval == null) ) {
      throw new IllegalArgumentException( "Either an offer or amount, currency and interval must be set, when creating a subscription" );
    }

    MultivaluedMap<String, String> params = new MultivaluedMapImpl();
    ValidationUtils.validatesPayment( payment );
    params.add( "payment", payment.getId() );
    if( client != null ) {
      ValidationUtils.validatesClient( client );
      params.add( "client", client.getId() );
    }
    if( offer != null ) {
      ValidationUtils.validatesOffer( offer );
      params.add( "offer", offer.getId() );
    }
    if( amount != null ) {
      ValidationUtils.validatesAmount( amount );
      params.add( "amount", String.valueOf( amount ) );
    }
    if( currency != null ) {
      ValidationUtils.validatesCurrency( currency );
      params.add( "currency", currency );
    }
    if( interval != null ) {
      ValidationUtils.validatesIntervalPeriodWithChargeDay( interval );
      params.add( "interval", interval.toString() );
    }
    if( startAt != null ) {
      params.add( "start_at", String.valueOf( startAt.getTime() / 1000 ) );
    }
    if( name != null ) {
      params.add( "name", name );
    }
    if( periodOfValidity != null ) {
      ValidationUtils.validatesIntervalPeriod( periodOfValidity );
      params.add( "period_of_validity", periodOfValidity.toString() );
    }

    return RestfulUtils.create( SubscriptionService.PATH, params, Subscription.class, super.httpClient );
  }

  /**
   * This function creates a {@link Subscription} between a {@link Client} and an {@link Offer}. A {@link Client} can have several
   * {@link Subscription}s to different {@link Offer}s, but only one {@link Subscription} to the same {@link Offer}. The
   * {@link Client}s is charged for each billing interval entered. <br />
   * <strong>NOTE</strong><br />
   * As the Subscription create method has a lot of options, we recommend you to use a {@link Subscription.Creator}.
   * @param paymentId
   *          A {@link Payment} used for charging.
   * @param clientId
   * @param offerId
   *          An {@link Offer} to subscribe to. Mandatory only if amount, curreny and interval are not set
   * @param amount
   *          Amount to be charged. Mandatory if offer is null.
   * @param currency
   *          Currency in which to be charged. Mandatory if offer is null.
   * @param interval
   *          Interval of charging. Mandatory if offer is null.
   * @param startAt
   *          The date, when the subscription will start charging. If longer than 10 minutes in the future, a preauthorization
   *          will occur automatically to verify the payment.
   * @param name
   *          A name for this subscription
   * @param periodOfValidity
   *          if set, the subscription will expire after this period.
   * @return the subscription.
   */
  public Subscription create( String paymentId, String clientId, String offerId, Integer amount, String currency, Interval.PeriodWithChargeDay interval,
      Date startAt, String name, Interval.Period periodOfValidity ) {
    return create( new Payment( paymentId ), new Client( clientId ), new Offer( offerId ), amount, currency, interval, startAt, name, periodOfValidity );
  }

  /**
   * Temporary pauses a subscription. <br />
   * <strong>NOTE</strong><br />
   * Pausing is permitted until one day (24 hours) before the next charge date.
   * @param subscription
   *          the subscription
   * @return the updated subscription
   */
  public Subscription pause( Subscription subscription ) {
    MultivaluedMap<String, String> params = new MultivaluedMapImpl();
    params.add( "pause", String.valueOf( true ) );
    return RestfulUtils.update( SubscriptionService.PATH, subscription, params, Subscription.class, super.httpClient );
  }

  /**
   * Temporary pauses a subscription.<br />
   * <strong>NOTE</strong><br />
   * Pausing is permitted until one day (24 hours) before the next charge date.
   * @param subscriptionId
   *          the Id of the subscription
   * @return the updated subscription
   */
  public Subscription pause( String subscriptionId ) {
    return this.pause( new Subscription( subscriptionId ) );
  }

  /**
   * Unpauses a subscription. Next charge will occur according to the defined interval.<br />
   * <strong>NOTE</strong><br />
   * if the nextCaptureAt is the date of reactivation: a charge will happen<br />
   * if the next_capture_at is in the past: it will be set to: reactivationdate + interval <br/>
   * <br />
   * <strong>IMPORTANT</strong><br />
   * An inactive subscription can reactivated within 13 month from the date of pausing. After this period, the subscription will
   * expire and cannot be re-activated.<br />
   * @param subscription
   *          the subscription
   * @return the updated subscription
   */
  public Subscription unpause( Subscription subscription ) {
    MultivaluedMap<String, String> params = new MultivaluedMapImpl();
    params.add( "pause", String.valueOf( false ) );
    return RestfulUtils.update( SubscriptionService.PATH, subscription, params, Subscription.class, super.httpClient );
  }

  /**
   * Unpauses a subscription. Next charge will occur according to the defined interval.<br />
   * <strong>NOTE</strong><br />
   * if the nextCaptureAt is the date of reactivation: a charge will happen<br />
   * if the next_capture_at is in the past: it will be set to: reactivationdate + interval <br/>
   * <br />
   * <strong>IMPORTANT</strong><br />
   * An inactive subscription can reactivated within 13 month from the date of pausing. After this period, the subscription will
   * expire and cannot be re-activated.<br />
   * @param subscriptionId
   *          the Id of the subscription
   * @return the updated subscription
   */
  public Subscription unpause( String subscriptionId ) {
    return this.pause( new Subscription( subscriptionId ) );
  }

  /**
   * Changes the amount of a subscription. The new amount is valid until the end of the subscription. If you want to set a
   * temporary one-time amount use {@link SubscriptionService#changeAmountTemporary(String, Integer)}
   * @param subscriptionId
   *          the Id of the subscription.
   * @param amount
   *          the new amount.
   * @return the updated subscription.
   */
  public Subscription changeAmount( String subscriptionId, Integer amount ) {
    return this.changeAmount( new Subscription( subscriptionId ), amount );
  }

  /**
   * Changes the amount of a subscription. The new amount is valid until the end of the subscription. If you want to set a
   * temporary one-time amount use {@link SubscriptionService#changeAmountTemporary(String, Integer)}
   * @param subscription
   *          the subscription.
   * @param amount
   *          the new amount.
   * @return the updated subscription.
   */
  public Subscription changeAmount( Subscription subscription, Integer amount ) {
    return changeAmount( subscription, amount, 1 );
  }

  /**
   * Changes the amount of a subscription. The new amount is valid one-time only after which the original subscription amount will
   * be charged again. If you want to permanently change the amount use {@link SubscriptionService#changeAmount(String, Integer)}
   * .
   * @param subscription
   *          the subscription.
   * @param amount
   *          the new amount.
   * @return the updated subscription.
   */
  public Subscription changeAmountTemporary( Subscription subscription, Integer amount ) {
    return changeAmount( subscription, amount, 0 );
  }

  /**
   * Changes the amount of a subscription. The new amount is valid one-time only after which the original subscription amount will
   * be charged again. If you want to permanently change the amount use {@link SubscriptionService#changeAmount(String, Integer)}
   * .
   * @param subscriptionId
   *          the Id of the subscription.
   * @param amount
   *          the new amount.
   * @return the updated subscription.
   */
  public Subscription changeAmountTemporary( String subscriptionId, Integer amount ) {
    return this.changeAmountTemporary( new Subscription( subscriptionId ), amount );
  }

  private Subscription changeAmount( Subscription subscription, Integer amount, Integer type ) {
    MultivaluedMap<String, String> params = new MultivaluedMapImpl();
    params.add( "amount", String.valueOf( amount ) );
    params.add( "amount_change_type", String.valueOf( type ) );
    return RestfulUtils.update( SubscriptionService.PATH, subscription, params, Subscription.class, super.httpClient );
  }

  /**
   * Change the offer of a subscription. <br />
   * The plan will be changed immediately. The next_capture_at will change to the current date (immediately). A refund will be
   * given if due. <br />
   * If the new amount is higher than the old one, a pro-rata charge will occur. The next charge date is immediate i.e. the
   * current date. If the new amount is less then the old one, a pro-rata refund will occur. The next charge date is immediate
   * i.e. the current date. <br />
   * <strong>IMPORTANT</strong><br />
   * Permitted up only until one day (24 hours) before the next charge date. <br />
   * @param subscription
   *          the subscription
   * @param offer
   *          the new offer
   * @return the updated subscription
   */
  public Subscription changeOfferChangeCaptureDateAndRefund( Subscription subscription, Offer offer ) {
    return changeOffer( subscription, offer, 2 );
  }

  /**
   * Change the offer of a subscription. <br />
   * The plan will be changed immediately.The next_capture_at date will remain unchanged. A refund will be given if due. <br />
   * If the new amount is higher than the old one, there will be no additional charge. The next charge date will not change. If
   * the new amount is less then the old one, a refund happens. The next charge date will not change. <br />
   * <strong>IMPORTANT</strong><br />
   * Permitted up only until one day (24 hours) before the next charge date. <br />
   * @param subscription
   *          the subscription
   * @param offer
   *          the new offer
   * @return the updated subscription
   */
  public Subscription changeOfferKeepCaptureDateAndRefund( Subscription subscription, Offer offer ) {
    return changeOffer( subscription, offer, 1 );
  }

  /**
   * Change the offer of a subscription. <br />
   * the plan will be changed immediately. The next_capture_at date will remain unchanged. No refund will be given <br />
   * <strong>IMPORTANT</strong><br />
   * Permitted up only until one day (24 hours) before the next charge date. <br />
   * @param subscription
   *          the subscription
   * @param offer
   *          the new offer
   * @return the updated subscription
   */
  public Subscription changeOfferKeepCaptureDateNoRefund( Subscription subscription, Offer offer ) {
    return changeOffer( subscription, offer, 0 );
  }

  private Subscription changeOffer( Subscription subscription, Offer offer, Integer type ) {
    ValidationUtils.validatesOffer( offer );
    MultivaluedMap<String, String> params = new MultivaluedMapImpl();
    params.add( "offer", offer.getId() );
    params.add( "offer_change_type", String.valueOf( type ) );
    return RestfulUtils.update( SubscriptionService.PATH, subscription, params, Subscription.class, super.httpClient );
  }

  /**
   * Stop the trial period of a subscription and charge immediately.
   * @param subscription
   *          the subscription.
   * @return the updated subscription.
   */
  public Subscription endTrial( Subscription subscription ) {
    MultivaluedMap<String, String> params = new MultivaluedMapImpl();
    params.add( "trial_end", String.valueOf( false ) );
    return RestfulUtils.update( SubscriptionService.PATH, subscription, params, Subscription.class, super.httpClient );
  }

  /**
   * This function removes an existing subscription. If you set the attribute cancelAtPeriodEnd parameter to the value
   * <code>true</code>, the subscription will remain active until the end of the period. The subscription will not be renewed
   * again. If the value is set to <code>false</code> it is directly terminated, but pending {@link Transaction}s will still be
   * charged.
   * @param subscription
   *          A {@link Subscription} with Id to be deleted.
   */
  public void delete( Subscription subscription ) {
    RestfulUtils.delete( SubscriptionService.PATH, subscription, Subscription.class, super.httpClient );
  }

  /**
   * This function removes an existing subscription. If you set the attribute cancelAtPeriodEnd parameter to the value
   * <code>true</code>, the subscription will remain active until the end of the period. The subscription will not be renewed
   * again. If the value is set to <code>false</code> it is directly terminated, but pending {@link Transaction}s will still be
   * charged.
   * @param subscriptionId
   *          Id of the {@link Subscription}, which have to be deleted.
   */
  public void delete( String subscriptionId ) {
    this.delete( new Subscription( subscriptionId ) );
  }

}
