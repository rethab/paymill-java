package com.paymill.services;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.time.DateUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.paymill.context.PaymillContext;
import com.paymill.exceptions.PaymillException;
import com.paymill.models.Client;
import com.paymill.models.Interval;
import com.paymill.models.Offer;
import com.paymill.models.Payment;
import com.paymill.models.Subscription;

public class SubscriptionServiceTest {

  private final static long   TWO_WEEKS_FROM_NOW = 1000 * 60 * 60 * 24 * 14;
  private final static long   TWO_DAYS_FROM_NOW  = 1000 * 60 * 60 * 24 * 2;

  private SubscriptionService subscriptionService;
  private PaymentService      paymentService;
  private ClientService       clientService;
  private OfferService        offerService;

  private String              clientEmail        = "john.rambo@qaiware.com";
  private String              clientDescription  = "Boom, boom, shake the room";
  private String              token              = "098f6bcd4621d373cade4e832627b4f6";
  private Integer             amount             = 900;
  private String              currency           = "EUR";
  private Interval.Period     interval           = Interval.period( 1, Interval.Unit.MONTH );
  private String              name               = "Chuck Testa";

  private Client              client;
  private Payment             payment;
  private Offer               offer1;
  private Offer               offer2;
  private List<Subscription>  subscriptions;
  private Subscription        subscription;

  @BeforeClass
  public void setUp() {
    PaymillContext paymill = new PaymillContext( System.getProperty( "apiKey" ) );

    this.subscriptionService = paymill.getSubscriptionService();
    this.paymentService = paymill.getPaymentService();
    this.clientService = paymill.getClientService();
    this.offerService = paymill.getOfferService();

    this.client = this.clientService.createWithEmailAndDescription( this.clientEmail, this.clientDescription );
    this.payment = this.paymentService.createWithTokenAndClient( this.token, this.client.getId() );
    this.offer1 = this.offerService.create( this.amount, this.currency, this.interval, this.name );
    this.offer2 = this.offerService.create( this.amount * 2, this.currency, this.interval, "Updated " + this.name );

    this.subscriptions = new ArrayList<Subscription>();
  }

  @Test
  public void testCreateWithPaymentAndOffer() {
    Subscription subscription = this.subscriptionService.create( Subscription.create( this.payment, this.offer1 ) );
    this.subscription = subscription;
    Assert.assertNotNull( subscription );
    Assert.assertNotNull( subscription.getClient() );
    Assert.assertEquals( subscription.getPayment().getId(), this.payment.getId() );
    Assert.assertEquals( subscription.getOffer().getId(), this.offer1.getId() );
    this.subscriptions.add( subscription );
  }

  @Test
  public void testCreateWithPaymentAndOfferComplex() {
    Date tomorrow = DateUtils.addDays( new Date(), 1 );
    Subscription subscription = this.subscriptionService.create( Subscription.create( this.payment, this.offer1 ).withClient( this.client )
        .withAmount( this.amount * 5 ).withCurrency( "EUR" ).withInterval( "2 WEEK,monday" ).withName( "test sub" ).withOffer( this.offer2 )
        .withPeriodOfValidity( "1 YEAR" ).withStartDate( tomorrow ) );
    Assert.assertNotNull( subscription );
    Assert.assertNotNull( subscription.getClient() );
    Assert.assertEquals( subscription.getPayment().getId(), this.payment.getId() );
    Assert.assertEquals( subscription.getClient().getId(), this.client.getId() );
    Assert.assertEquals( subscription.getAmount(), (Integer) (this.amount * 5) );
    Assert.assertEquals( subscription.getCurrency(), "EUR" );
    Assert.assertEquals( subscription.getInterval().getInterval(), (Integer) 2 );
    Assert.assertEquals( subscription.getInterval().getUnit(), Interval.Unit.WEEK );
    Assert.assertEquals( subscription.getInterval().getWeekday(), Interval.Weekday.MONDAY );

    Assert.assertEquals( subscription.getName(), "test sub" );
    Assert.assertEquals( subscription.getPeriodOfValidity().getInterval(), (Integer) 1 );
    Assert.assertEquals( subscription.getPeriodOfValidity().getUnit(), Interval.Unit.YEAR );
    Assert.assertTrue( subscription.getNextCaptureAt().getTime() > new Date().getTime() );
    Assert.assertEquals( subscription.getStatus(), Subscription.Status.ACTIVE );
    Assert.assertFalse( subscription.isCanceled() );
    Assert.assertFalse( subscription.isDeleted() );
    Assert.assertFalse( subscription.getLivemode() );

    Assert.assertNull( subscription.getCanceledAt() );
    Assert.assertTrue( datesAroundSame( subscription.getCreatedAt(), new Date() ) );
    Assert.assertTrue( datesAroundSame( subscription.getTrialStart(), new Date() ) );
    Assert.assertTrue( datesAroundSame( subscription.getTrialEnd(), tomorrow ) );
    Assert.assertTrue( datesAroundSame( subscription.getNextCaptureAt(), tomorrow ) );
    Assert.assertNull( subscription.getTempAmount() );
    this.subscriptions.add( subscription );
  }

  @Test( expectedExceptions = IllegalArgumentException.class )
  public void testCreateWithoutOfferAndAmount_shouldFail() {
    this.subscriptionService.create( this.payment, this.client, null, null, null, null, null, null, null );
  }

  @Test
  public void testCreateWithPaymentAndClient_WithOfferWithoutTrial_shouldReturnSubscriptionWithNullTrialStartAndNullTrialEnd() {
    Client client = clientService.createWithEmail( "zendest@example.com" );
    Payment payment = paymentService.createWithTokenAndClient( "098f6bcd4621d373cade4e832627b4f6", client );
    Offer offer = offerService.create( 2223, "EUR", Interval.period( 1, Interval.Unit.WEEK ), "Offer No Trial" );

    Subscription subscriptionNoTrial = subscriptionService.create( Subscription.create( payment.getId(), offer ).withClient( client.getId() ) );
    Assert.assertNull( subscriptionNoTrial.getTrialStart() );
    Assert.assertNull( subscriptionNoTrial.getTrialEnd() );

    this.subscriptions.add( subscriptionNoTrial );
  }

  @Test
  public void testCreateWithPaymentAndClient_WithOfferWithTrial_shouldReturnSubscriptionWithTrialEqualsTrialInOffer() {
    Client client = clientService.createWithEmail( "zendest@example.com" );
    Payment payment = paymentService.createWithTokenAndClient( "098f6bcd4621d373cade4e832627b4f6", client );
    Offer offer = offerService.create( 2225, "EUR", Interval.period( 1, Interval.Unit.WEEK ), "Offer With Trial", 2 );
    Subscription subscriptionWithOfferTrial = subscriptionService.create( Subscription.create( payment.getId(), offer ).withClient( client.getId() ) );

    Assert.assertNotNull( subscriptionWithOfferTrial.getTrialStart() );
    Assert.assertTrue( datesAroundSame( subscriptionWithOfferTrial.getTrialEnd(), DateUtils.addDays( new Date(), 2 ) ) );
    Assert.assertTrue( datesAroundSame( subscriptionWithOfferTrial.getNextCaptureAt(), DateUtils.addDays( new Date(), 2 ) ) );

    this.subscriptions.add( subscriptionWithOfferTrial );
  }

  @Test
  public void testCreateWithPaymentClientAndStartat_WithOfferWithTrial_shouldReturnSubscriptionWithTrialEqualsTrialInSubscription() {
    Client client = clientService.createWithEmail( "zendest@example.com" );
    Payment payment = paymentService.createWithTokenAndClient( "098f6bcd4621d373cade4e832627b4f6", client );
    Offer offer = offerService.create( 2224, "EUR", Interval.period( 1, Interval.Unit.WEEK ), "Offer No Trial", 2 );
    Subscription subscriptionWithOfferTrial = subscriptionService.create( Subscription.create( payment.getId(), offer ).withClient( client.getId() )
        .withStartDate( DateUtils.addDays( new Date(), 5 ) ) );

    Assert.assertNotNull( subscriptionWithOfferTrial.getTrialStart() );
    Assert.assertTrue( datesAroundSame( subscriptionWithOfferTrial.getTrialEnd(), DateUtils.addDays( new Date(), 5 ) ) );
    Assert.assertTrue( datesAroundSame( subscriptionWithOfferTrial.getNextCaptureAt(), DateUtils.addDays( new Date(), 5 ) ) );

    this.subscriptions.add( subscriptionWithOfferTrial );
  }

  @Test
  public void testPauseAndUnpauseSubscription() {
    Subscription subscription = subscriptionService.create( Subscription.create( this.payment, this.offer1 ).withInterval( "1 WEEK" ) );
    subscriptionService.pause( subscription );
    Assert.assertEquals( subscription.getStatus(), Subscription.Status.INACTIVE );
    subscriptionService.unpause( subscription );
    Assert.assertEquals( subscription.getStatus(), Subscription.Status.ACTIVE );
    Assert.assertTrue( datesAroundSame( subscription.getNextCaptureAt(), DateUtils.addWeeks( new Date(), 1 ) ) );
    this.subscriptions.add( subscription );
  }

  @Test
  public void testChangeSubscriptionAmountPermanently() {
    Subscription subscription = subscriptionService.create( Subscription.create( this.payment, 1200, "EUR", "1 WEEK" ) );
    subscriptionService.changeAmount( subscription, 2000 );
    Assert.assertEquals( subscription.getAmount(), (Integer) 2000 );
    Assert.assertNull( subscription.getTempAmount() );
    this.subscriptions.add( subscription );
  }

  @Test
  public void testChangeSubscriptionAmountTemp() {
    Subscription subscription = subscriptionService.create( Subscription.create( this.payment, 1200, "EUR", "1 WEEK" ) );
    subscriptionService.changeAmountTemporary( subscription, 2000 );
    Assert.assertEquals( subscription.getAmount(), (Integer) 1200 );
    Assert.assertEquals( subscription.getTempAmount(), (Integer) 2000 );
    this.subscriptions.add( subscription );
  }

  @Test
  public void testChangeOfferKeepNextCaptureNoRefund() {
    Date inAWeek = DateUtils.addWeeks( new Date(), 1 );
    Subscription subscription = subscriptionService.create( Subscription.create( this.payment, 1200, "EUR", "1 WEEK" ) );
    Assert.assertTrue( datesAroundSame( subscription.getNextCaptureAt(), inAWeek ) );
    subscriptionService.changeOfferKeepCaptureDateNoRefund( subscription, this.offer1 );
    Assert.assertTrue( datesAroundSame( subscription.getNextCaptureAt(), inAWeek ) );
    this.subscriptions.add( subscription );
  }

  @Test
  public void testChangeOfferKeepNextCaptureAndRefund() {
    Date inAWeek = DateUtils.addWeeks( new Date(), 1 );
    Date inTwoWeeks = DateUtils.addWeeks( new Date(), 2 );
    Subscription subscription = subscriptionService.create( Subscription.create( this.payment, 1200, "EUR", "1 WEEK" ) );
    Assert.assertTrue( datesAroundSame( subscription.getNextCaptureAt(), inAWeek ) );
    subscriptionService.changeOfferKeepCaptureDateAndRefund( subscription, this.offer1 );
    //TODO cannot be tested correctly as there 
    //Assert.assertTrue( datesAroundSame( subscription.getNextCaptureAt(), inAWeek ) );
    this.subscriptions.add( subscription );
  }

  @Test
  public void testChangeOfferChangeNextCaptureAndRefund() {
    Date inAWeek = DateUtils.addWeeks( new Date(), 1 );
    Date inTwoWeeks = DateUtils.addWeeks( new Date(), 2 );
    Date inAMonth = DateUtils.addMonths( new Date(), 1 );
    Subscription subscription = subscriptionService.create( Subscription.create( this.payment, 1200, "EUR", "1 WEEK" ).withStartDate( inTwoWeeks ) );
    Assert.assertTrue( datesAroundSame( subscription.getNextCaptureAt(), inTwoWeeks ) );
    subscriptionService.changeOfferChangeCaptureDateAndRefund( subscription, this.offer1 );
    // when we call the above we trigger a transaction, so the nextCapture moves to offer1 interval - 1 month
    Assert.assertTrue( datesAroundSame( subscription.getNextCaptureAt(), inAMonth ) );
    this.subscriptions.add( subscription );
  }

  @Test
  public void testEndTrial() {
    Date inAWeek = DateUtils.addWeeks( new Date(), 1 );
    Date inTwoWeeks = DateUtils.addWeeks( new Date(), 2 );
    Subscription subscription = subscriptionService.create( Subscription.create( this.payment, 1200, "EUR", "1 WEEK" ).withStartDate( inTwoWeeks ) );
    Assert.assertTrue( datesAroundSame( subscription.getNextCaptureAt(), inTwoWeeks ) );
    subscriptionService.endTrial( subscription );
    Assert.assertTrue( datesAroundSame( subscription.getNextCaptureAt(), new Date() ) );
    Assert.assertNull( subscription.getTrialEnd() );
    this.subscriptions.add( subscription );
  }

  @Test
  public void testChangePeriodValidity() {
    Subscription subscription = subscriptionService.create( Subscription.create( this.payment, offer1 ).withPeriodOfValidity( "1 YEAR" ) );
    Assert.assertEquals( subscription.getPeriodOfValidity().getInterval(), (Integer) 1 );
    Assert.assertEquals( subscription.getPeriodOfValidity().getUnit(), Interval.Unit.YEAR );
    subscriptionService.limitValidity( subscription, "2 MONTH" );
    Assert.assertEquals( subscription.getPeriodOfValidity().getInterval(), (Integer) 2 );
    Assert.assertEquals( subscription.getPeriodOfValidity().getUnit(), Interval.Unit.MONTH );
    subscriptionService.unlimitValidity( subscription );
    Assert.assertNull( subscription.getPeriodOfValidity() );
    this.subscriptions.add( subscription );
  }

  /*
  @Test( expectedExceptions = PaymillException.class )
  public void testCreateWithPaymentAndClient_shouldFail() {
    this.subscriptionService.create( Subscription.Creator.create( payment.getId(), client.getId() ).withOffer( offer1 ) );
  }

  
  @Test( dependsOnMethods = "testCreateWithPayment" )
  public void testCreateWithPaymentAndClient_shouldSecceed() throws Exception {
    Thread.sleep( 1000 );

    Subscription subscription = this.subscriptionService.createWithOfferPaymentAndClient( offer2, payment, client );
    Assert.assertNotNull( subscription );
    Assert.assertNotNull( subscription.getClient() );

    this.subscriptions.add( subscription );
  }

  @Test( dependsOnMethods = "testCreateWithPaymentAndClient_shouldSecceed" )
  public void testUpdate() {
    String offerId = this.subscription.getOffer().getId();
    String subscriptionId = this.subscription.getId();
    Assert.assertEquals( this.subscription.getOffer().getId(), this.offer1.getId() );

    this.subscription.setOffer( this.offer2 );
    this.subscriptionService.update( this.subscription );

    Assert.assertFalse( StringUtils.equals( this.subscription.getOffer().getId(), offerId ) );
    Assert.assertEquals( this.subscription.getOffer().getId(), this.offer2.getId() );
    Assert.assertEquals( this.subscription.getId(), subscriptionId );
  }

  //TODO[VNi]: uncomment when API returns null instead of empty array
  //@Test( dependsOnMethods = "testUpdate" )
  public void testListOrderByOffer() {
    // TODO[VNi]: There is an API error: No sorting by offer.
    Subscription.Order orderDesc = Subscription.createOrder().byOffer().desc();
    Subscription.Order orderAsc = Subscription.createOrder().byOffer().asc();

    List<Subscription> subscriptionsDesc = this.subscriptionService.list( null, orderDesc ).getData();
    Assert.assertEquals( subscriptionsDesc.size(), this.subscriptions.size() );

    List<Subscription> subscriptionsAsc = this.subscriptionService.list( null, orderAsc ).getData();
    Assert.assertEquals( subscriptionsAsc.size(), this.subscriptions.size() );

    Assert.assertEquals( subscriptionsDesc.get( 0 ).getOffer().getId(), subscriptionsAsc.get( 1 ).getOffer().getId() );
    Assert.assertEquals( subscriptionsDesc.get( 1 ).getOffer().getId(), subscriptionsAsc.get( 0 ).getOffer().getId() );
  }

  //TODO[VNi]: uncomment when API returns null instead of empty array
  //@Test( dependsOnMethods = "testUpdate" )
  public void testListOrderByCreatedAt() {
    Subscription.Order orderDesc = Subscription.createOrder().byCreatedAt().desc();
    Subscription.Order orderAsc = Subscription.createOrder().byCreatedAt().asc();

    List<Subscription> subscriptionsDesc = this.subscriptionService.list( null, orderDesc, 100000, 0 ).getData();
    for( Subscription subscription : subscriptionsDesc ) {
      if( subscription.getOffer() == null )
        this.subscriptionService.get( subscription );
    }

    List<Subscription> subscriptionsAsc = this.subscriptionService.list( null, orderAsc, 100000, 0 ).getData();

    Assert.assertEquals( subscriptionsDesc.get( 0 ).getId(), subscriptionsAsc.get( subscriptionsAsc.size() - 1 ).getId() );
    Assert.assertEquals( subscriptionsDesc.get( subscriptionsDesc.size() - 1 ).getId(), subscriptionsAsc.get( 0 ).getId() );
  }
  */
  public static boolean datesAroundSame( Date first, Date second, int minutes ) {
    long timespan = minutes * 60 * 1000;
    return Math.abs( first.getTime() - second.getTime() ) < timespan;
  }

  public static boolean datesAroundSame( Date first, Date second ) {
    return datesAroundSame( first, second, 10 );
  }
}
